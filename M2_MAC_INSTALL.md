# NASA-AMMOS CWS - Mac Quick Install Guide

## For Mac using Prebuilt Docker Image (No Build Required)

Quick setup guide for **running** the NASA-AMMOS Common Workflow Service (CWS) prebuilt Docker image on Mac.

**For Building from Source**: See [README.md](README.md) for full developer setup with Java 17, Maven, and build scripts.

---

## What You Need vs. What You Don't

| Need for Running Prebuilt | Don't Need (Build Only) |
|---------------------------|-------------------------|
| ✅ Docker or Colima | ❌ Java 17 JDK |
| ✅ 10+ GB RAM | ❌ Maven |
| ✅ 64+ GB disk | ❌ ITerm2 |
| | ❌ Logstash |
| | ❌ Build scripts |

---

## Quick Setup (5 Steps)

### 1. Start Docker Runtime

**Option A: Docker Desktop**
- Allocate resources: 5 CPUs, 14GB RAM minimum
- Start Docker Desktop

**Option B: Colima (Lightweight)**
```bash
colima start -p rose --cpu 10 --disk 210 --memory 32
colima status  # Verify running
```

### 2. Create Docker Network

```bash
docker network create cws-network
```

### 3. Start MariaDB Database

```bash
docker run -d \
  --name cws-db \
  --network cws-network \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=YOUR_DB_PASSWORD \
  -e MYSQL_DATABASE=cws_dev \
  -e TZ=America/Los_Angeles \
  mariadb:10.6
```

**Replace `YOUR_DB_PASSWORD`** with your chosen password. Remember it for step 5.

### 4. Start Elasticsearch

```bash
docker run -d \
  --name cws-es \
  --network cws-network \
  -p 9200:9200 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -e "ES_JAVA_OPTS=-Xms512m -Xmx512m" \
  elasticsearch:8.12.0
```

Wait 30 seconds for Elasticsearch to initialize.

### 5. Start CWS Container

#### A. Create Credentials File

```bash
mkdir -p ~/.cws
echo "changeit" > ~/.cws/creds
chmod 700 ~/.cws
chmod 400 ~/.cws/creds
```

**Note**: `changeit` is the default password for the prebuilt image's SSL certificates.

#### B. Create Configuration File

```bash
cat > ~/.cws/config.properties <<'EOF'
auto_accept_config=y
startup_autoregister_process_defs=false
hostname=localhost
install_type=2
database_type=mariadb
database_host=cws-db
database_port=3306
database_name=cws_dev
database_username=root
database_password=YOUR_DB_PASSWORD
admin_user=YOUR_ADMIN_USERNAME
admin_firstname=YOUR_FIRST_NAME
admin_lastname=YOUR_LAST_NAME
admin_email=YOUR_EMAIL
cws_web_port=8080
cws_ssl_port=8443
cws_ajp_port=8009
cws_shutdown_port=8005
cws_console_host=localhost
cws_console_ssl_port=8443
amq_host=localhost
amq_port=61616
cws_amq_jmx_port=7099
cws_jmx_port=1099
identity_plugin_type=CAMUNDA
notify_users_email=n
brand_header=CWS Docker
project_webapp_root=cws-app
cws_enable_cloud_autoscaling=n
cws_notification_emails=YOUR_EMAIL
elasticsearch_protocol=http
elasticsearch_use_auth=n
elasticsearch_host=cws-es
elasticsearch_port=9200
user_provided_logstash=n
history_level=full
history_days_to_live=30
cws_token_expiration_hours=24
EOF
```

**Replace these placeholders:**
- `YOUR_DB_PASSWORD` - Your MariaDB password from step 3
- `YOUR_ADMIN_USERNAME` - Your CWS admin username (e.g., `admin`)
- `YOUR_FIRST_NAME` - Your first name
- `YOUR_LAST_NAME` - Your last name
- `YOUR_EMAIL` - Your email address (appears twice)

#### C. Pull and Run CWS

```bash
# Pull the image
docker pull ghcr.io/nasa-ammos/common-workflow-service:2.8.0-20260202-121314

# Run CWS
docker run -d \
  --name cws \
  --platform linux/amd64 \
  --network cws-network \
  -p 8080:8080 \
  -p 8443:8443 \
  -e DB_HOST=cws-db \
  -e DB_PORT=3306 \
  -e DB_NAME=cws_dev \
  -e DB_USER=root \
  -e DB_PW=YOUR_DB_PASSWORD \
  -e ES_HOST=cws-es \
  -e ES_PORT=9200 \
  -e ES_PROTOCOL=http \
  -v ~/.cws/creds:/root/.cws/creds:ro \
  -v ~/.cws/config.properties:/home/cws_user/config.properties:ro \
  ghcr.io/nasa-ammos/common-workflow-service:2.8.0-20260202-121314
```

**Replace `YOUR_DB_PASSWORD`** with your MariaDB password from step 3.

**About `--platform linux/amd64`**: The CWS Docker image is built for Intel/AMD (x86_64) architecture. On Apple Silicon Macs (M1/M2/M3/M4), this flag tells Docker to use Rosetta 2 emulation to run the Intel-based image. Performance may be slightly slower than native ARM, but it works reliably. Intel Mac users don't need this flag but it doesn't hurt to include it.

#### D. Wait for Startup (2-5 minutes)

```bash
# Watch logs until you see "Server startup in ... milliseconds"
docker logs -f cws

# Press Ctrl+C when startup completes
```

---

## Access CWS

Once startup completes:

```bash
# Open CWS UI
open http://localhost:8080/cws-ui/

# Login with credentials you set in config.properties
# Username: YOUR_ADMIN_USERNAME
# Password: (the password you set)
```

**Other Interfaces:**
- **Camunda Cockpit**: http://localhost:8080/camunda/app/cockpit/
- **Camunda Admin**: http://localhost:8080/camunda/app/admin/

---

## Custom SSL Certificates (Optional)

If you want to use your own SSL certificates instead of the prebuilt defaults:

### 1. Generate Certificates

```bash
git clone https://github.com/NASA-AMMOS/common-workflow-service.git
cd common-workflow-service/cws-certs
./generate-certs.sh
# Enter a keystore password when prompted - remember it!
```

### 2. Copy Certificates

```bash
mkdir -p ~/.cws/certs
cp .keystore ~/.cws/certs/
cp cws_truststore.jks ~/.cws/certs/
```

### 3. Update Credentials File

```bash
echo "YOUR_KEYSTORE_PASSWORD" > ~/.cws/creds
chmod 400 ~/.cws/creds
```

### 4. Run CWS with Custom Certificates

```bash
docker run -d \
  --name cws \
  --platform linux/amd64 \
  --network cws-network \
  -p 8080:8080 \
  -p 8443:8443 \
  -e DB_HOST=cws-db \
  -e DB_PORT=3306 \
  -e DB_NAME=cws_dev \
  -e DB_USER=root \
  -e DB_PW=YOUR_DB_PASSWORD \
  -e ES_HOST=cws-es \
  -e ES_PORT=9200 \
  -e ES_PROTOCOL=http \
  -v ~/.cws/creds:/root/.cws/creds:ro \
  -v ~/.cws/config.properties:/home/cws_user/config.properties:ro \
  -v ~/.cws/certs/.keystore:/home/cws_user/cws/server/apache-tomcat-10.1.36/conf/.keystore:ro \
  -v ~/.cws/certs/cws_truststore.jks:/home/cws_user/cws/server/apache-tomcat-10.1.36/lib/cws_truststore.jks:ro \
  ghcr.io/nasa-ammos/common-workflow-service:2.8.0-20260202-121314
```

**Replace `YOUR_DB_PASSWORD`** with your MariaDB password.

---

## Managing CWS

### Stop Services
```bash
docker stop cws cws-db cws-es
```

### Start Services
```bash
docker start cws-db cws-es
sleep 10  # Wait for DB and ES
docker start cws
```

### Restart CWS Only
```bash
docker restart cws
```

### View Logs
```bash
docker logs -f cws
docker logs cws-db
docker logs cws-es
```

### Check Status
```bash
docker ps | grep cws
```

---

## Troubleshooting

### CWS Won't Start

```bash
# Check all containers are running
docker ps | grep cws

# Check CWS logs for errors
docker logs cws | tail -50

# Common issues:
# - Wrong database password in docker run command
# - Database not ready yet (wait 30 more seconds)
# - Elasticsearch not ready (wait 60 more seconds)
```

### Can't Login

- Verify username matches what you put in `~/.cws/config.properties`
- Check `admin_user` field in config file

### Port Already in Use

```bash
# Find what's using port 8080
lsof -i :8080

# Either stop that service, or change CWS ports:
docker run ... -p 8081:8080 -p 8444:8443 ...
# Then access at http://localhost:8081/cws-ui/
```

### Database Connection Failed

```bash
# Verify database is running
docker ps | grep cws-db

# Test connection
docker exec cws ping -c 3 cws-db

# Check database password matches
docker logs cws | grep -i "database"
```

### Certificate Errors

```bash
# For default certs, verify creds file contains "changeit"
cat ~/.cws/creds

# For custom certs, verify password matches what you used in generate-certs.sh
# Recreate creds file if needed
echo "YOUR_PASSWORD" > ~/.cws/creds
chmod 400 ~/.cws/creds
docker restart cws
```

---

## Quick Command Reference

```bash
# Start everything
docker start cws-db cws-es cws

# Stop everything
docker stop cws cws-db cws-es

# View CWS logs
docker logs -f cws

# Check status
docker ps | grep cws

# Remove everything (including data)
docker stop cws cws-db cws-es
docker rm cws cws-db cws-es
docker network rm cws-network
docker volume prune
```

---

## Next Steps

After successful installation:

1. **Try the Web UI**: http://localhost:8080/cws-ui/
2. **Deploy a Workflow**: Use Camunda Cockpit to deploy BPMN files
3. **Run Tests**: See [test-cws-lisa](../test-cws-lisa/) for automated testing
4. **Learn BPMN**: https://camunda.com/bpmn/

---

## Resources

- [CWS GitHub](https://github.com/NASA-AMMOS/common-workflow-service)
- [CWS Wiki](https://github.com/NASA-AMMOS/common-workflow-service/wiki)
- [Camunda Docs](https://docs.camunda.org/)
- [BPMN Tutorial](https://camunda.com/bpmn/)

---

**Note**: This guide uses default self-signed SSL certificates suitable for testing. For production deployments, use custom certificates and follow additional security hardening steps.
