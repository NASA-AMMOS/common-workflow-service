# Certificates & Keystore

The CWS web console runs over TLS and needs a Tomcat **keystore** and
**truststore**, plus the keystore password.

## Required files

| File | Path | Purpose |
| --- | --- | --- |
| Keystore | `install/.keystore` | Tomcat server certificate. |
| Truststore | `install/tomcat_lib/cws_truststore.jks` | Trusted certificates. |
| Password | `~/.cws/creds` | Plaintext keystore password. |

Lock down the credentials file so only you can read it:

```bash
chmod 700 ~/.cws/
chmod 400 ~/.cws/creds
```

See the [Apache Tomcat SSL How-To](https://tomcat.apache.org/tomcat-9.0-doc/ssl-howto.html)
for background on Tomcat TLS.

## Generate self-signed certificates

For development or open-source use, generate a self-signed keystore and
truststore with the project's script:

```bash
cd cws-certs
./generate-certs.sh
```

!!! warning
    Running `generate-certs.sh` **replaces** the existing keystore and
    truststore in `install/` with new certificates.

## Certificates and the Docker image

The published CWS Docker image ships with self-signed certificates that use the
default password `changeit`, so it runs without extra configuration.

To use your own certificates with the image:

1. Generate them with `generate-certs.sh`.
2. Make them available to the container — either copy them in before startup or
   (more easily) use volume mounts. The `docker-compose.yml` in
   `install/docker/` has commented-out volume lines for this.

Inside the image, CWS looks for the keystore files at:

```
/home/cws_user/cws/server/apache-tomcat-11.0.20/conf/.keystore
/home/cws_user/cws/server/apache-tomcat-11.0.20/lib/cws_truststore.jks
```

Provide the keystore password as a plaintext file mounted at `/root/.cws/creds`.

!!! note
    This certificate password is **not** the password you use to log into the
    CWS interface — it only unlocks the certificates themselves.

Next: [Building from Source](building.md).
