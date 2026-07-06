# User Administration

User and permission management in CWS is handled through the **Camunda Admin** page, which is accessible from the top-right navigation bar in the CWS web console.

## Accessing the Admin Page

Click the **Admin** button in the top-right corner of the CWS console. This opens the Camunda Admin UI, which is CWS's primary control hub for user management.

!!! note
    In LDAP mode, users and groups are automatically synchronized from your directory service. You still manage *authorizations* (what each user/group can do) through the Admin page, but you do not create users manually — they exist in LDAP.

## Users

### Adding a user (Camunda auth mode)

1. Navigate to **Admin > Users**
2. Click **Create new user**
3. Fill in the user ID, first name, last name, email, and initial password
4. Click **Create**

The new user can log in immediately with the password you set. Prompt them to change it on first login.

### Editing a user

Select a user from the list to update their profile information or reset their password.

### Removing a user

Select the user and click **Delete**. This removes the user from Camunda's local identity store. In LDAP mode, users must be deactivated in LDAP itself.

## Groups

Groups allow batch permission assignment. A user inherits all authorizations granted to any group they belong to.

### Creating a group

1. Navigate to **Admin > Groups**
2. Click **Create new group**
3. Provide a group ID and name
4. Click **Create**

### Adding members to a group

1. Open the group
2. Click **Add member**
3. Search for and select the user

In LDAP mode, group membership is controlled in LDAP. The Admin page will reflect the current LDAP state.

## Authorizations

Authorizations define what a user or group can do within CWS. Navigate to **Admin > Authorizations** to manage them.

### Application authorizations

Control which Camunda applications (Cockpit, Tasklist, Admin) a user or group can access.

| Application | Purpose |
|-------------|---------|
| `cockpit` | View and manage running process instances |
| `tasklist` | Claim and complete user tasks |
| `admin` | Manage users, groups, and authorizations |

To grant a group access to an application:

1. Go to **Admin > Authorizations > Application**
2. Click **Create new authorization**
3. Set type to **Grant**, resource type to **Application**, and select the group
4. Enter the application name (e.g., `cockpit`)
5. Check the **Access** permission and save

### Process definition authorizations

Restrict which users or groups can read, create, or delete specific process definitions and their instances.

Common permission patterns:

| Permission | Allows |
|------------|--------|
| `READ` | View the process definition |
| `CREATE_INSTANCE` | Manually start an instance |
| `READ_HISTORY` | View past process instance data in Cockpit |
| `DELETE_HISTORY` | Remove historical instance records |

To grant a group access to a specific process definition:

1. Go to **Admin > Authorizations > Process Definition**
2. Create a new authorization for the group
3. Set the Resource ID to the process definition key (or `*` for all definitions)
4. Choose the required permissions

### Task authorizations

Control which users or groups can see and interact with user tasks within process instances.

## Tenants

CWS supports Camunda's multi-tenancy model. Tenants can isolate process definitions and instances between different teams or projects on a shared CWS instance. Configure tenants under **Admin > Tenants**.

## Further reading

- [Camunda 7.24 User Management](https://docs.camunda.org/manual/7.24/webapps/admin/user-management/)
- [Camunda 7.24 Authorization](https://docs.camunda.org/manual/7.24/user-guide/process-engine/authorization-service/)
