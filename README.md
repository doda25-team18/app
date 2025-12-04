# SMS Checker / Frontend

The frontend allows users to interact with the model in the backend through a web-based UI.

The frontend is implemented with Spring Boot and only consists of a website and one REST endpoint.
It **requires Java 25+** to run (tested with 25.0.1).
Any classification requests will be delegated to the `backend` service that serves the model.
You must specify the environment variable `MODEL_HOST` to define where the backend is running.

The frontend service can be started through running the `Main` class (e.g., in your IDE) or through Maven (recommended):

    MODEL_HOST="http://localhost:8081" mvn spring-boot:run

The server runs on port 8080. Once its startup has finished, you can access [localhost:8080/sms](http://localhost:8080/sms) in your browser to interact with the application.

---

## Local Development Setup: Maven Authentication for GitHub Packages

To allow your local Maven (and thus your IDE) to download private dependencies like `lib-version` from GitHub Packages, you need to configure your personal Maven `settings.xml` file.

1.  **Locate/Create `settings.xml`:**
    *   **Mac/Linux:** `~/.m2/settings.xml`
    *   **Windows:** `C:\Users\<YourUsername>\.m2\settings.xml`
2.  **Add Server Credentials:** Open this file and add the following `<servers>` block, replacing `YOUR_GITHUB_USERNAME` with your GitHub username and `YOUR_GITHUB_TOKEN` with a GitHub Personal Access Token (PAT) that has at least `read:packages` scope.

    ```xml
    <settings>
      <servers>
        <server>
          <id>github</id> <!-- This ID must match the repository ID in pom.xml -->
          <username>YOUR_GITHUB_USERNAME</username>
          <password>YOUR_GITHUB_TOKEN</password>
        </server>
      </servers>
    </settings>
    ```

---

## Building Docker Images

To build and push multi-architecture Docker images for this `app` service:

1.  **Ensure Docker Buildx is configured:**
    ```bash
    docker buildx create --use
    ```
2.  **Set Environment Variables:**
    ```bash
    export GITHUB_ACTOR="YOUR_GITHUB_USERNAME"
    export GITHUB_TOKEN="YOUR_GITHUB_PAT_TOKEN"
    ```
3.  **Build and Push Image:** Run this command from the `app/` directory.
    ```bash
    docker buildx build \
      --platform linux/amd64,linux/arm64 \
      --build-arg GITHUB_ACTOR \
      --secret id=github_token,env=GITHUB_TOKEN \
      -t ghcr.io/doda25-team18/app:0.1.0 \
     .
    ```
    *Replace `0.1.0` with the target version.*

    
## Metrics
The `app` service exposes three Prometheus metrics at the `/sms/metrics` endpoint

An example output of this endpoint is given below:

```
# HELP num_predictions This is the total amount of predictions that were requested and handled
# TYPE num_predictions counter
num_predictions{model_response="spam"} 1
num_predictions{model_response="ham"} 13

# HELP correct_predictions_ratio This is the fraction of predictions where the user correctly predicted the model response
# TYPE correct_predictions_ratio gauge
correct_predictions_ratio{model_response="spam"} 1.0
correct_predictions_ratio{model_response="ham"} 0.30769232

# HELP predict_latency_seconds This is how long it took to get a response from the model service in seconds
# TYPE predict_latency_seconds histogram
predict_latency_seconds_bucket{le="0.02"} 6
predict_latency_seconds_bucket{le="0.05"} 13
predict_latency_seconds_bucket{le="0.1"} 13
predict_latency_seconds_bucket{le="0.2"} 13
predict_latency_seconds_bucket{le="0.5"} 14
predict_latency_seconds_bucket{le="+Inf"} 14
predict_latency_seconds_sum 0.527920170687139
predict_latency_seconds_count 14
```
