# External Database

The **Adaptation External Database** feature lets you connect to a database
separate from the core CWS schema, so your project-specific data lives
independently.

## Setup

### 1. Add SQL templates

Place the following files in `install/sql/`:

| File | Targets |
| --- | --- |
| `adaptation.sql.template` | Core database (`cws_dev`) |
| `adaptation_core.sql.template` | Core database (`cws_dev`) |
| `adaptation_external.sql.template` | External database (`cws_external_db` or your name) |

### 2. Define your schema

Add your `CREATE TABLE` statements in `adaptation_external.sql.template`:

```sql
CREATE TABLE IF NOT EXISTS `my_project_table` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
```

### 3. Write Java code

Extend `DbService` and use the appropriate JDBC template:

| Template | Database |
| --- | --- |
| `jdbcTemplate` | Core CWS database |
| `jdbcAdaptationTemplate` | Your external adaptation database |

```java
@Service
public class MyProjectDbService extends DbService {

    public List<MyRecord> getRecords() {
        return jdbcAdaptationTemplate.query(
            "SELECT * FROM my_project_table",
            new MyRecordRowMapper()
        );
    }
}
```

### 4. Configure the connection

The external database connection details are specified in your
[configuration properties](../install/configuration.md) and applied during
installation.

## Notes

- The external database is created and managed alongside the core CWS database
  during installation.
- Both databases can be on the same host or on different hosts.
- Use the adaptation SQL templates to version-control your schema alongside
  CWS.
