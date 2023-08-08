# CWS v2.4 Database Upgrade

### Use this script before installing CWS v2.4 to update your database schema.

#### _NOTICE: Once you've used this upgrade script, preserve your existing data by answering 'No' to the cws installation prompted: Do you want this script to drop and re-create the database for you, so that you have a clean install?_

This upgrade helps to prepare the database of older CWS versions, previous to v2.4, to be used for new installations of CWS v2.4. The script updates the existing database schema to match CWS v2.4 core. This gives the user the option of preserving data and migrating to CWS v2.4 without blowing up the database.

### Update Actions:

* *Script updates to database* : 
  * Alter database table *cws_worker* to add new column `max_num_running_procs`
  * Remove existing worker data from tables `cws_worker`, `cws_worker_proc_def`, `cws_log_usage`

To run the commands:


```
cd install/upgrade/
```

```
./upgrade_to_2.4.sh
```