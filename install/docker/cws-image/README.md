### This is the Docker Image for common-workflow-service.

Required Prerequisites:

- SSL Keystore Setup:
    - Add a `.keystore` SSL file into the `install` dir
    - Add a `cws_truststore.jks` file into `install/tomcat_lib` dir
    
- Timezone (default: America/Los_Angeles) Setup:
    - Update the ENV TZ variable in the Dockerfile with your desired time zone.
    - Update the docker-compose.xml db service with your desired time zone.


    Run the `./build.sh` script to build the image.  

#### Resulting Image Tag:

    nasa-ammos/common-workflow-service:2.0
