variable "project"      { default = "m20" }
variable "venue"        { default = "dev" }
variable "subsystem"    { default = "ids" }
variable "cap"          { default = "pipeline" }
variable "securityPlan" { default = "644" }
variable "iamPolicy"    { default = "m20-ids-s3-access" }

locals {
  name_prefix = "${var.project}-${var.venue}-${var.subsystem}-${var.cap}"
 
  default_tags = {
    Name           = "${local.name_prefix}"
    Creator        = "ghollins@jpl.nasa.gov"
    POC            = "ghollins@jpl.nasa.gov"
    Venue          = "${var.venue}"
    Project        = "${var.project}"
    Subsystem      = "${var.subsystem}"
    Capability     = "${var.cap}"
    CapVersion     = "0.1"
    GDSRelease     = "NA"
    Component      = "NA"
    SecurityPlanID = "${var.securityPlan}"
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
variable "token"          { }
variable "cwsAdminUser"   { }
variable "cwsS3Bucket"    { }
variable "tag"            { }
variable "grelease"       { }
variable "numWorkers"     { }
variable "pemFile"        { }
variable "consoleAmi"     { default = "ami-a62f4cc7" }
variable "workerAmi"      { default = "ami-a62f4cc7" }

variable "region"         { default = "us-gov-west-1" }
variable "subnetId"       { default = "subnet-54a61031" }
variable "dbSg"           { default = "sg-19d75c7c" }
variable "consoleSg"      { default = "sg-8ad75cef" }
variable "workerSg"       { default = "sg-9ad75cff" }
variable "db_subnet_ids" {
  default = ["subnet-36c71041", "subnet-54a61031"]
}

variable "dbClass"        { default = "db.t2.xlarge" }
variable "consoleClass"   { default = "t2.2xlarge" }
variable "workerClass"    { default = "t2.xlarge" }

#
# THIS VALUE MUST MATCH THE NAME OF THE PEM FILE YOU ARE USING
# (PREFIX OF PEM FILE NAME)
#
variable "keyName"        { }

#
# Database configuration
#
variable "dbName"         { default = "cws" }
variable "dbUser"         { default = "cws" }
variable "dbPass"         { default = "myawscw5" }

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
  token      = "${var.token}"
  region     = "${var.region}"
}

resource "random_id" "server" {
  byte_length = 8
}


# ------------------
# THE CWS WORKER(s)
# ------------------
resource "aws_instance" "cws-worker" {
  ami           = "${var.workerAmi}"
  key_name      = "${var.keyName}"
  connection {
     private_key = "${file(var.pemFile)}"
  }
  instance_type = "${var.workerClass}"
  subnet_id     = "${var.subnetId}"
  vpc_security_group_ids = ["${var.workerSg}"]
  user_data = "INSTALL_TYPE=worker&DB_HOST=cws-db-roastt1214c.cbtgh8rlzfls.us-gov-west-1.rds.amazonaws.com&DB_USER=${var.dbUser}&DB_PASS=${var.dbPass}&S3_BUCKET=${var.cwsS3Bucket}&CWS_ADMIN_USER=${var.cwsAdminUser}&GRELEASE=${var.grelease}"
  #tags {
  #      Name = "cws-worker-${var.tag}-${count.index}"
  #}
  iam_instance_profile = "${var.iamPolicy}"
  provisioner "local-exec" {
    command = "echo ${self.private_ip} >> ${var.worker_hostnames_file}"
  }
  count = "${var.numWorkers}"
  tags = "${merge(
    local.default_tags,
    map(
      "Name", "${local.name_prefix}-worker-${var.tag}-${count.index}",
      "Component", "worker"
    )
  )}"
}

