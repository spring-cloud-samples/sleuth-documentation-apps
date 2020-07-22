![Java CI with Maven](https://github.com/spring-cloud-samples/sleuth-documentation-apps/workflows/Java%20CI%20with%20Maven/badge.svg)

# Sleuth documentation apps

[Apps used for the Sleuth documentation graphs](https://cloud.spring.io/spring-cloud-sleuth/spring-cloud-sleuth.html) . They're not using
service discovery so don't treat them as reference production applications ;)

The apps are sending spans to Zipkin via RabbitMQ and `spring-cloud-sleuth-stream`.

# Running acceptance tests

Run:

```
./runAcceptanceTests.sh
```

# Running the apps locally with RabbitMQ

Provide the env variable `DEFAULT_HEALTH_HOST` pointing to your Docker Machine. By default it will be `localhost`.
That way we'll start a RabbitMQ instance for you.

Run:

```
./scripts/start_with_zipkin_server.sh
```
