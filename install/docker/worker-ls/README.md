# Docker Worker Setup

### Quickly add a common-workflow-service worker to a system.  

## Prerequisites:

1. Have a running CWS server to add this worker to--recommended to be on a separate machine.
2. Update the `config.properties` and `docker-compose.yml`with correct server URLs, etc.
3. Update `cws-logstash.conf` `__ES_HOST__` with correct Elasticsearch URL.

To run use the command:
    
    docker-compose up
