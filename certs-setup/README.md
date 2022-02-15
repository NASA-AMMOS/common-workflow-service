## Instructions generate certs

#### While in the `certs-setup` directory, run the `generate_new_trustore_and_keystore.sh` script

Make file executable
```
chmod +x generate_new_trustore_and_keystore.sh
```


Run the script
```
./generate_new_trustore_and_keystore.sh
```

---------
#### Check `temp` folder for files: `keystore.jks` and `cws_truststore.jks`


Copy `keystore.jks` and `cws_truststore.jks` to respective CWS TOMCAT directories

* cp keystore.jks to <TOMCAT>/conf/.keystore

* cp cp cws_truststore.jks to <TOMCAT>/lib/cws_truststore.jk