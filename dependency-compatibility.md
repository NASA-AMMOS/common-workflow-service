# CWS Dependency Compatibility Matrix

## 🔒 **Enforced Compatibility Rules**

### **Java Version Requirements**
- **Minimum**: Java 17
- **Current**: Java 17
- **Reason**: Required for Spring Framework 6.x and modern libraries

### **Maven Version Requirements**
- **Minimum**: Maven 3.6.0
- **Current**: Maven 3.6.0+
- **Reason**: Required for modern plugin support

## 📋 **Dependency Compatibility Matrix**

### **Core Framework Dependencies**

| Library | Current Version | Compatible Versions | Notes |
|---------|----------------|-------------------|-------|
| **Spring Framework** | 6.2.4 | 6.2.x, 6.3.x | ✅ Stable |
| **Camunda BPM** | 7.23.0-ee | 7.23.x | ✅ Enterprise |
| **Java** | 17 | 17, 21 | ✅ LTS |

### **Logging Dependencies**

| Library | Current Version | Compatible Versions | Notes |
|---------|----------------|-------------------|-------|
| **Log4j** | 2.17.1 | 2.17.1+ | ⚠️ Security requirement |
| **SLF4J** | 2.17.1 | 2.17.1+ | ✅ Compatible |

### **Apache Commons Dependencies**

| Library | Current Version | Compatible Versions | Notes |
|---------|----------------|-------------------|-------|
| **Commons-Lang3** | 3.18.0 | 3.18.x+ | ✅ Migrated from 2.x; use `org.apache.commons.lang3.*` |
| **Commons-IO** | 2.14.0 | 2.14.x+ | ✅ Updated |
| **Commons-Exec** | 1.3 | 1.3+ | ✅ Stable |
| **Commons-Email** | 2.0.0-M1 | 2.0.0+ | ✅ Stable |
| **Commons-Text** | 1.10.0 | 1.10.x+ | ✅ Needed for `StringEscapeUtils` |

### **Database Dependencies**

| Library | Current Version | Compatible Versions | Notes |
|---------|----------------|-------------------|-------|
| **MariaDB Driver** | 2.7.2 | 2.7.x, 3.x | ✅ Stable |
| **MySQL Driver** | 8.0.28 | 8.0.x, 8.1.x | ✅ Stable |
| **HikariCP** | 4.0.3 | 4.0.x, 5.x | ✅ Stable |
| **H2 Database** | 2.2.220 | 2.2.x | ✅ Test only |

### **Testing Dependencies**

| Library | Current Version | Compatible Versions | Notes |
|---------|----------------|-------------------|-------|
| **JUnit** | 4.13.1 | 4.13.x | ⚠️ Consider JUnit 5 |
| **Mockito** | 4.3.1 | 4.3.x, 5.x | ✅ Stable |
| **AssertJ** | 3.22.0 | 3.22.x, 4.x | ✅ Stable |

### **JSON Processing**

| Library | Current Version | Compatible Versions | Notes |
|---------|----------------|-------------------|-------|
| **Jackson** | 2.16.1 | 2.16.x, 2.17.x, 2.18.x | ✅ Stable |
| **Gson** | 2.8.9 | 2.8.x, 2.9.x, 2.10.x, 2.11.x | ✅ Stable |

### **Web Dependencies**

| Library | Current Version | Compatible Versions | Notes |
|---------|----------------|-------------------|-------|
| **Jakarta Servlet** | 6.0.0 | 6.0.x | ✅ Stable |
| **Jakarta JMS** | 3.1.0 | 3.1.x | ✅ Stable |
| **Jakarta Mail** | 2.0.1 | 2.0.x | ✅ Stable |

## 🚨 **Known Incompatibilities**

### **Commons-Lang 2.6 → 3.x Migration (COMPLETED)**
- **Breaking Changes**: API changes between 2.x and 3.x
- **What changed in code**:
  - Imports moved from `org.apache.commons.lang.*` to `org.apache.commons.lang3.*`
  - `ExceptionUtils.getFullStackTrace(Throwable)` → `ExceptionUtils.getStackTrace(Throwable)`
  - `StringEscapeUtils` moved to `org.apache.commons.text.StringEscapeUtils` (add Commons-Text)
  - `StringUtils` now `org.apache.commons.lang3.StringUtils`


### **JUnit 4 → 5 Migration**
- **Breaking Changes**: Complete API rewrite
- **Required Actions**: Significant test code changes
- **Recommendation**: Gradual migration

## 🔧 **Enforcement Rules**

### **Version Requirements**
- Log4j: Must be 2.17.1 or higher (security requirement)
- Java: Must be 17 or higher
- Maven: Must be 3.6.0 or higher

## 📝 **Update Strategy**

### **Safe Updates (No Breaking Changes)**
2. **Gson**: 2.8.9 → 2.11.0
3. **Jackson**: 2.16.1 → 2.18.1
4. **Commons-Text**: Added at 1.10.0 for `StringEscapeUtils`
5. **Maven Plugins**: Update to latest versions

### **Requires Code Changes**
1. **JUnit**: 4.13.1 → 5.11.1 (Complete rewrite)

### **Enterprise Considerations**
1. **Camunda**: 7.23.0-ee (Enterprise license required)
2. **Spring**: 6.2.4 (Stable, well-tested)

## 🛠️ **Tools for Compatibility Checking**

### **Maven Enforcer Plugin**
- Enforces version compatibility rules
- Prevents dependency conflicts
- Validates environment requirements

### **Versions Maven Plugin**
- Shows available updates
- Checks for newer versions
- Updates dependencies safely

### **OWASP Dependency-Check**
- Identifies security vulnerabilities
- Checks for known CVEs
- Provides remediation advice

## 📋 **Testing Strategy**

### **Before Updates**
1. Run full test suite
2. Check dependency tree for conflicts
3. Review changelog for breaking changes

### **After Updates**
1. Run unit tests
2. Run integration tests
3. Run security scans
4. Test critical functionality

## 🔄 **Continuous Monitoring**

### **Automated Checks**
- Maven Enforcer Plugin (build-time)
- OWASP Dependency-Check (security)
- Versions Plugin (update notifications)

### **Manual Reviews**
- Monthly dependency review
- Security bulletin monitoring
- Compatibility testing
