# CWS Dependency Compatibility Matrix

### **Java Version Requirements**
- **Minimum**: Java 17
- **Current**: Java 17
- **Reason**: Required for Spring Framework 6.x and modern libraries

### **Maven Version Requirements**
- **Minimum**: Maven 3.9.6
- **Current**: Maven 3.9.6+
- **Reason**: Required for modern plugin support

## 📋 **Dependency Compatibility Matrix**

### **Core Framework Dependencies**

| Library | Current Version | Compatible Versions | Notes |
|---------|----------------|-------------------|-------|
| **Spring Framework** | 6.2.4 | 6.2.x, 6.3.x | ✅ Stable |
| **Camunda BPM** | 7.23.0-ee | 7.23.x | ✅ Enterprise |
| **Java** | 17 | 17, 21 | ✅ LTS |

### **Testing Dependencies**

| Library | Current Version | Compatible Versions | Notes |
|---------|----------------|-------------------|-------|
| **JUnit** | 4.13.2 | 4.13.x | ⚠️ Consider upgrading to JUnit 5 |

## 🚨 **Known Incompatibilities**

### **JUnit 4 → 5 Migration**
- **Breaking Changes**: Complete API rewrite
- **Required Actions**: Significant test code changes
- **Recommendation**: Gradual migration

## 📝 **Update Strategy**

### **Requires Code Changes**
1. **JUnit**: 4.13.2 → 5.12.2 (Complete rewrite)

### **Enterprise Considerations**
1. **Camunda**: 7.23.0-ee (Enterprise license required)
2. **Spring**: 6.2.4 (Stable, well-tested)

## 🛠️ **Tools for Compatibility Checking**

### **Maven Enforcer Plugin**
- Enforces version compatibility rules
- Prevents dependency conflicts
- Validates environment requirements
- Automatically runs during build

### **Versions Maven Plugin**
- Shows available updates
- Checks for newer versions
- Updates dependencies safely
- Must be run manually
- **Command to check for latest versions**: `mvn versions:display-property-updates`

### **OWASP Dependency-Check**
- Identifies security vulnerabilities
- Checks for known CVEs
- Provides remediation advice
- **Note**: Not automatic - must be run manually with commands:
  - `mvn clean dependency-check:aggregate` (for aggregate report)
  - `mvn clean dependency-check:check` (for check goal)

### **Automated Checks**
- Maven Enforcer Plugin (build-time)

### **Manual Security Checks**
- Versions Plugin (requires manual execution)
- OWASP Dependency-Check (requires manual execution)
