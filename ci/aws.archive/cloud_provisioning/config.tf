variable "project"            { default = "cws" }
variable "venue"              { default = "dev" }
variable "subsystem"          { default = "ids" }
variable "cap"                { default = "pipeline" }
variable "iamPolicy"          { default = "m20-ids-s3-access" }

locals {
  name_prefix = "${var.project}-${var.venue}"

  default_tags = {
    Name           = "${local.name_prefix}"
    Creator        = "email@jpl.nasa.gov"
    POC            = "email@jpl.nasa.gov"
    Venue          = "${var.venue}"
    Project        = "${var.project}"
    Subsystem      = "${var.subsystem}"
    Capability     = "${var.cap}"
    CapVersion     = "0.1"
    GDSRelease     = "NA"
    Component      = "NA"
    ExposedWeb     = "false"
    Experimental   = "false"
    UserFacing     = "false"
    CritInfra      = "true"
    SourceControl  = "NA"
  }
}

#
# This configuration is for launching a cloud fleet (IDS Pipeline)
# in the AWS cloud.
#
# This sets up the necessary infrastructure for running the:
#   -- CWS DB
#   -- CWS Console node
#   -- One or more CWS Worker nodes (quantity is defined be numWorkers variable)
#
variable "accessKey"      { }
variable "secretKey"      { }
variable "cwsAdminUser"   { }
variable "cwsS3Bucket"    { }
variable "tag"            { }
variable "numWorkers"     { }
variable "pemFile"        { }
#
# THIS VALUE MUST MATCH THE NAME OF THE PEM FILE YOU ARE USING
# (PREFIX OF PEM FILE NAME)
#
variable "keyName"        { }

variable "workerAmi"      { default = "ami-d563ecb4" }
variable "consoleAmi"     { default = "ami-d563ecb4" }

variable "region"         { default = "us-gov-west-1" }

variable "workerSg"       { default = "sg-6d4bda08" }
variable "consoleSg"      { default = "sg-4103a824" }
variable "dbSg"           { default = "sg-270ca742" }

variable "subnetId"       { default = "subnet-61248216" }

variable "dbClass"        { default = "db.t2.large" }
variable "consoleClass"   { default = "t2.xlarge" }
variable "workerClass"    { default = "t2.xlarge" }

variable "dbName"         { default = "cws" }
variable "dbUser"         { default = "cws" }
variable "dbPass"         { default = "myawscw5" }

#
# Database configuration
#
variable "engine"         { default = "mariadb" }
variable "engine_version" {
  description = "Engine version"
  default = {
    #mariadb    = "10.0.32"
    mariadb    = "10.1.23"
  }
}
#variable "engine"         { default = "mysql" }
#variable "engine_version" {
#  description = "Engine version"
#  default = {
#    mysql    = "5.6.34"
#     mysql    = "5.7.19"
#  }
#}


resource "aws_db_parameter_group" "default" {
  name   = "rds-pg"
  family = "mariadb10.1"

  parameter {
    name  = "character_set_server"
    value = "utf8"
  }

  parameter {
    name  = "character_set_client"
    value = "utf8"
  }

  #parameter {
  #  name = "time_zone"
  #  value = "US/Pacific"
  #}
}

variable "subnet_ids" {
  default = ["subnet-e7ee6982", "subnet-61248216"]
}

variable "info_file" {
  default = "host_info.txt"
  description = "File records service information. Such as IP address etc"
}
variable "console_hostname_file" { default = "console_hostname.txt" }
variable "worker_hostnames_file"  { default = "worker_hostnames.txt" }
variable "db_hostname_file"      { default = "db_hostname.txt" }

provider "aws" {
  access_key = "${var.accessKey}"
  secret_key = "${var.secretKey}"
  region     = "${var.region}"
}

resource "random_id" "server" {
  byte_length = 8
}

#
# Create subnet group
#
resource "aws_db_subnet_group" "ids_ci_cws_rds_subnet_group" {
  name        = "ids_ci_cws_rds_subnet_group-${random_id.server.hex}"
  description = "group of subnets for IDS CWS testing"
  subnet_ids  = "${var.subnet_ids}"
}

# -----------------
# THE CWS DATABASE
# -----------------
resource "aws_db_instance" "cws-db" {
  identifier             = "cws-db-${var.tag}"
  allocated_storage      = "10"
  engine                 = "${var.engine}"
  engine_version         = "${lookup(var.engine_version, var.engine)}"
  instance_class         = "${var.dbClass}"
  name                   = "${var.dbName}"
  username               = "${var.dbUser}"
  password               = "${var.dbPass}"
  parameter_group_name   = "${aws_db_parameter_group.default.id}" 
  vpc_security_group_ids = ["${var.dbSg}"]
  db_subnet_group_name   = "${aws_db_subnet_group.ids_ci_cws_rds_subnet_group.id}"
  provisioner "local-exec" {
    command = "echo ${aws_db_instance.cws-db.address} > ${var.db_hostname_file}"
  }
  skip_final_snapshot = true
}

# ----------------
# THE CWS CONSOLE
# ----------------
resource "aws_instance" "cws-console" {
  ami           = "${var.consoleAmi}"
  key_name      = "${var.keyName}"
  connection {
    private_key = "${file(var.pemFile)}"
    host = ""
  }
  instance_type = "${var.consoleClass}"
  subnet_id     = "${var.subnetId}"
  vpc_security_group_ids = ["${var.consoleSg}"]
  user_data = "INSTALL_TYPE=console&DB_HOST=${aws_db_instance.cws-db.address}&DB_USER=${var.dbUser}&DB_PASS=${var.dbPass}&S3_BUCKET=${var.cwsS3Bucket}&CWS_ADMIN_USER=${var.cwsAdminUser}"
  provisioner "local-exec" {
    command = "echo ${self.private_ip} > ${var.console_hostname_file} ; echo '\n\n-= I WILL WAIT HERE UNTIL THE CONSOLE WEB SERVER IS UP =-\n\n'; while ! test `curl ${self.private_ip}:38080 -s | grep console | wc -l` = '1'; do sleep 10; echo 'console not up yet...'; done"
  }
  depends_on = [aws_db_instance.cws-db]
  tags = "${merge(
        local.default_tags,
        map(
          "Name", "${local.name_prefix}-console-${var.tag}",
          "Component", "console"
        )
      )}"
}

# ------------------
# THE CWS WORKER(s)
# ------------------
resource "aws_instance" "cws-worker" {
  ami           = "${var.workerAmi}"
  key_name      = "${var.keyName}"
  connection {
    private_key = "${file(var.pemFile)}"
    host = ""
  }
  instance_type = "${var.workerClass}"
  subnet_id     = "${var.subnetId}"
  vpc_security_group_ids = ["${var.workerSg}"]
  user_data = "INSTALL_TYPE=worker&DB_HOST=${aws_db_instance.cws-db.address}&DB_USER=${var.dbUser}&DB_PASS=${var.dbPass}&S3_BUCKET=${var.cwsS3Bucket}&CWS_ADMIN_USER=${var.cwsAdminUser}"
  provisioner "local-exec" {
    command = "echo ${self.private_ip} >> ${var.worker_hostnames_file}"
  }
  depends_on = [aws_instance.cws-console]
  count = "${var.numWorkers}"
  tags = "${merge(
    local.default_tags,
    map(
      "Name", "${local.name_prefix}-worker-${var.tag}-${count.index}",
      "Component", "worker"
    )
  )}"
}

