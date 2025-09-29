# Info
Author: Jānis Romāns

# Launch application
To not have to install many dependencies just to launch the application, I've set up Docker image build.
## Requirements
- Docker installed on system

## Steps
1. Download the Docker image
   ```bash
   docker pull acherrydev/hwk:0.0.1
   ```

2. Run the image
    ```bash
    docker run -d -p 8088:8088 acherrydev/hwk:0.0.1
    ```
   - `-d` for detached, so terminal isn't taken over by logs
   - `-p` port kept at 8088


3. (Optional) Run the rockets script
   ```bash
   ./rockets launch "http://localhost:8088/messages" --message-delay=10ms --concurrency-level=10
   ```

## Posting messages
Messages can be posted as specified in task readme.md to `http://localhost:8088/messages`

## Query state
> ### 1. All rockets
> `/rockets`
```bash
curl localhost:8088/rockets
```
See a list of all rockets, their speeds, missions, launch time etc.

<br>

> `/rockets?sortBy={property}&orderBy={asc/desc}`
```bash
curl localhost:8088/rockets?sortBy=speed&orderBy=desc
```
The output can be sorted by a property, in ascending (default) or descending order  
Available properties:
- `mission` (default)
- `type`
- `speed`
- `status`
- `launchTime`
- `endTime` - the time when rocket explosion message was received


> ### 1.1. Rockets with type

> `/rockets?type={rocketType}`
```bash
curl localhost:8088/rockets?type=Saturn-I
```

See all types
> `/rockets/types`
```bash
curl localhost:8088/rockets/types
```


> ### 2. Specific rocket

`/rockets/{rocketChannel}`
```bash
curl localhost:8088/rockets/8a6e7887-064f-54fa-b5ee-fac02e0dc05c
```


# Comments
The task description was really clear and had all the necessary details to get started 😊.

I'm using MacOS on Apple Silicon, so I ran the arm64 version.  
I've been mostly working with Java (and in backend with Spring Boot), however it has certain pain points in performance and testing, which is why for the web server framework I chose Micronaut instead, so there most likely is some awkwardness.


# Solution description
Messages can arrive out of order and multiple times ("at least once" guarantee).  
Messages cannot be skipped. You cannot decrease speed by 5000 when it's only 4000.  
Likewise we would want to know the state of the rocket before it exploded (find out why, find patterns).

Messages contain a number that tells their order - I use this number to check if a message has already been received (avoiding duplicates) and to process messages in order.  
When receiving a message with #5 while the last processed message was #3, we know we are missing #4.  
Until the next-in-order message arrives, I keep #5 and any other message for rocket R in a queue.

## Solution wants
Things I would add/change for a real-world application. Not an exhaustive list, just some of the things that came to mind:
- store data permanently (e.g. in a database) so it's not lost on restart (purposeful or accidental) and so the application could be scaled horizontally
- return paged results, so an imaginary frontend application would not need to load a lot of data about a lot of rockets at once
- create API documentation (e.g. Swagger/OpenAPI) from code
- performance measurement (throughput, latency), e.g. concerning "synchronized" blocks that _may_ be better with locks instead
- authenticate that the sender is really my trustworthy data source
- incoming message validation (currently I'm assuming contents are valid, e.g. that with a speed of 5000 there won't be a message-decrease-speed by 6000, resulting in negative 1000 speed)
- parsing failsafes (e.g. failing to parse message timestamp in one dateformat, retrying with another; parsing sortBy/orderBy could be done with a ready made solution)
- allow multi-property sorting (currently only allowing sorting by one property), and filtering (e.g. by active/decomissioned status)
- integrate a code coverage tool that would fail the build if code or code changes were not covered by tests


- return data history, for example speed by datetime, a rocket's mission history
- return some statistics for the imaginary dashboard that would use this REST API (e.g. average speed, lifetime by rocket type, lifetime by mission...)

# Metrics
## Test coverage
IntelliJ reports:
- Method 89%
- Line 88%
- Branch 74%

# Development
## Run tests
> - Java

```bash
./gradlew test
```

# Other
> - Java
> - Docker

Build local docker image
```bash
./gradlew jibDockerBuild &&
docker tag homework:0.0.1 acherrydev/hwk:0.0.1 &&
docker push acherrydev/hwk:0.0.1
```
(./gradlew jib should work with credential helper but it didn't)