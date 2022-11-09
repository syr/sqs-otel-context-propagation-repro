# Quarkus demo: SQS Client

This example showcases how to use the AWS SQS client with Quarkus. As a prerequisite install Install [AWS Command line interface](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html).


# Setup reproducer quarkus app (amazon-sqs-quickstart)
- config otel agent with env var (I included otel agent jar in the project to simplify setup, was donwloaded from https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/tag/v1.19.2): 
 ```
JAVA_TOOL_OPTIONS=-javaagent:./otel/opentelemetry-javaagent.jar -Dotel.traces.exporter=none -Dotel.metrics.exporter=none
```
- start quarkus app (amazon-sqs-quickstart), for example simply in intellij
  - alternatively by:  `./mvnw clean quarkus:dev`

# AWS SQS local instance

Just run it as follows in order to start SQS locally:
```
docker run --rm --name local-sqs -p 8010:4576 -e SERVICES=sqs -e START_WEB=0 -d localstack/localstack:0.11.1
```
SQS listens on `localhost:8010` for REST endpoints.

Create an AWS profile for your local instance using AWS CLI:

```
$ aws configure --profile localstack
AWS Access Key ID [None]: test-key
AWS Secret Access Key [None]: test-secret
Default region name [None]: us-east-1
Default output format [None]:
```

## Create SQS queue

Create a SQS queue and store Queue url in environment variable as we will need to provide it to the our app
```
aws sqs create-queue --queue-name=ColliderQueue --profile localstack --endpoint-url=http://localhost:8010
```



## Send messages to the queue

Simply use Intellij HTTP Client requests (Intellij IDEA Ultimate): `test.http`

Alternatively use curl:

Shoot with a couple of quarks
```
curl -XPOST -H"Content-type: application/json" http://localhost:8080/sync/cannon/shoot -d'{"flavor": "Charm", "spin": "1/2"}'
curl -XPOST -H"Content-type: application/json" http://localhost:8080/sync/cannon/shoot -d'{"flavor": "Strange", "spin": "1/2"}'
```
And receive it from the queue
```
curl http://localhost:8080/sync/shield
```

## Expected behaviour
Expected context propagation via sqs message (aws sdk2) to work automatically as it was fixed/implemented in https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/6199
having send and receive logs in the same traceId.


## Actual behaviour
Instead, receive is associated with a new traceId, e.g.
```
11:19:47 INFO  traceId=92dbb93c5ba9bd564e0307eb4b157788 [or.ac.sq.QuarksCannonSyncResource] (executor-thread-0) Fired Quark[Charm, 1/2}]
11:19:57 INFO  traceId=19fd77d0c37fdf31ed0b140314d02ced [or.ac.sq.QuarksShieldSyncResource] (executor-thread-0) before receive
11:19:57 INFO  traceId=19fd77d0c37fdf31ed0b140314d02ced [or.ac.sq.QuarksShieldSyncResource] (executor-thread-0) after receive
11:19:57 INFO  traceId=19fd77d0c37fdf31ed0b140314d02ced [or.ac.sq.QuarksShieldSyncResource] (executor-thread-0) Receiving message{"flavor":"Charm","spin":"1/2"}
```