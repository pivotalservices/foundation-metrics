---
Metrics Rest endpoint
---

Rest endpoint to gather the metrics from cloud controller and Ops Metrics

- Create a **cf service account** user
- Assign the `cloud_controller.admin` privileges to the service account user, using the uaac cli
- Use the following manifest, and update the values accordingly

```
---
applications:
- name: metrics
  memory: 1024M
  path: target/metrics-0.0.1-SNAPSHOT.jar
  buildpack: java_buildpack_offline
  env:
    SPRING_PROFILES_ACTIVE: 'jmx'
    JBP_CONFIG_CONTAINER_CERTIFICATE_TRUST_STORE: '{enabled: true}'
    jmx.host: <jmx-endpoint>
    jmx.port: <jmx-port>
    jmx.username: <jmx-username>
    jmx.password: <jmx-password>
    cf.target: https://<api-endpoint>
    cf.username: <service-account-user>
    cf.password: <service-account-password>
    mail.protocol: smtp
    mail.host: <smtp-host>
    mail.port: <smtp-port>
    mail.username: <smtp-user>
    mail.password: <smtp-password>
    mail.starttls: true
    mail.auth: true
```

**NOTE: If you don't want emails, then you can skip the mail properties**

**NOTE: With SPRING_PROFILES_ACTIVE=jmx, and without specifying the jmx properties, the application will fail to start**

**NOTE: Without specifying the cf properties, the application will fail to start**


- Compile the code using `mvn clean install`
- Target to the dedicated org/space for these utils
- `cf push` to your foundation

## Custom Rules
The rules are externalized into a `rules.csv` file, that is located `https://github.com/pivotalservices/foundation-metrics/blob/master/metrics/src/main/resources/rules.csv`.

You can add more rules to this file. The convention is
`job-name, attribute1|attribute2, threshold, alert-message`

where:
`job-name` - is the job name without the **-partition** postfix
`attribute1` - the attribute name from Ops Metrics for the job
`attribute2` - (optional) the second attribute name from Ops Metrics for the job, that needs to be compared with `attribute1`
`threshold` - Tolerance value for the rule
`alert-message` - Notification message format to be sent for the given rule

```
--- Rule execution ---

  if both attribute1 and attribute2 are specified
  then {
    (((attribute2.value/attribute1.value) * 100) < threshold)
    send message;
  }

  if only attribute1 is specified
  then {
    (attribute2.value > threshold)
    send message;
  }

```

**NOTE: Ensure you separate the attribute1 and attribute2 by the pipe `|` delimiter**
