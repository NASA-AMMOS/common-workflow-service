
### Setup

#### ATTENTION:  THIS IS THE OLD AWS SETUP.  DO NOT USE THIS FOR DEPLOYMENT.  USE ONLY FOR REFERENCE.

NOTE:  You will need to create your own personal ``cloud_provisioning/terraform.tfvars`` file before running the run.sh script.

For example:

```javascript
accessKey = "AKIALWYWXXXXXXKSEEGQ"
secretKey = "xpo4NsIzmSUgimDp1tg1epXXXXXXR3H8MLs/vx9g"
cwsS3Bucket = "my-bucket-name"
cwsAdminUser = "cws"
pemFile = "~/.ssh/cws-core-gov.pem"
keyName = "cws-core-gov"
```

The cwsS3Bucket should contain these files:
* cws_server.tar.gz
* console_conf.template
* worker_conf.template
* cws-process-initiators.xml
* snippets.java

NOTE: example files can be found here:
[./aws/s3/cws-dev/](https://github.jpl.nasa.gov/CWS/cws/tree/master/ci/aws/aws/s3/cws-dev)

The ``cws_server.tar.gz`` file in your bucket will be the version of CWS that gets configured and launched.
Get the latest version from [this page](https://wiki.jpl.nasa.gov/display/cws/Downloads)

### Creating a CWS Fleet on the Cloud
``./create.sh <your_fleet_name>  <num_workers>``

wait for it to finish...

### Destroying a CWS Fleet on the Cloud
``cd ./cloud_provisioning/fleets/<your_fleet_name>``

``./destroy_cloud_fleet.sh``

then follow the prompts...
