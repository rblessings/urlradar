# urlradar - Secure & Seamless URL Redirection Service

---
**For a detailed understanding of the project's direction and planned features, please refer to the open issues.**

---

## Overview

**Secure and Up-to-Date Links for Professionals**

**urlradar** is a dynamic URL redirection service designed to help professionals manage and maintain their links across
various digital platforms.

*Key benefits:*

* Easily update destinations without changing shared links.
* Prevent broken links due to website changes.
* Ideal for resumes, portfolios, social media, and more.

urlradar simplifies link management for job seekers, entrepreneurs, and anyone with an online presence.

[![Build, Test, Dockerize, and Deploy to Docker Hub](https://github.com/rblessings/urlradar/actions/workflows/build-test-dockerize-deploy.yml/badge.svg)](https://github.com/rblessings/urlradar/actions/workflows/build-test-dockerize-deploy.yml)

---

## Table of Contents

- [Features](#features)
- [Technical Features](#technical-features)
- [Installation](#installation)
- [Usage](#usage)
- [License](#license)

---

## Features

### Core Features

- **URL Redirection**: Redirect a static URL to a new destination without altering the shared link on CVs, LinkedIn
  profiles, portfolios, etc.
- **URL Shortening**: Transform long URLs into concise, custom short links for easier sharing. Example:
  `https://urlradar.io/{username}/{randomcode}`.
- **Analytics**: Track URL performance through detailed analytics, including click counts, geographical data, and
  referral sources.
- **Real-Time Link Updates**: Instantly update the destination of any redirected URL to keep links accurate and
  up-to-date across platforms.
- **Customizable Branding**: Create branded short URLs that align with your personal or professional identity.

---

## Technical Features

**urlradar** employs modern technologies to ensure scalability, security, and performance.

### Key Technical Components:

- **Security**:
    - **OAuth2 & JWT**: Secure user authentication and authorization with OAuth2 and JSON Web Tokens for seamless
      integration with third-party platforms.
    - **Encryption**: All data and URL redirects are encrypted in transit using HTTPS.

- **Fault Tolerance**:
    - **Circuit Breaker**: Resilience mechanisms to prevent cascading failures during service disruptions.
    - **Retries & Resilience**: Implemented using Resilience4J for retry logic and fault-tolerant network communication.

- **Distributed System**:
    - **Kafka**: Enables high-throughput, real-time data streaming for user analytics (click counts, geography,
      referrals) and facilitates decoupled microservice communication.
    - **MongoDB**: The system utilizes a NoSQL database to manage redirection data, analytics, and user profiles. This
      architecture supports efficient full-text search and indexing of URLs, ensuring rapid lookups and high-performance
      analytics.

- **Caching**:
    - **Redis**: Leverages Redis to cache frequently accessed URL redirection and analytics data (click counts,
      geography, referrals). This improves performance, reduces database load, and ensures data freshness through
      periodic updates and efficient memory management with eviction policies.

- **Session Management**:
    - **Spring Session**: Enables centralized session management for consistent handling across multiple instances or
      deployments, enhancing scalability, security, and session persistence.

- **Scalable Architecture**:
    - Microservices architecture designed for horizontal scaling to accommodate growing traffic.
    - Dockerized services for simplified deployment and scaling in cloud environments (AWS, GCP, Azure).

- **Cloud-Native**:
    - This application is designed for deployment within a **Kubernetes cluster**, enabling seamless scalability, high
      availability, and fault tolerance. Containerized using **Docker** for efficient packaging and portability.

---

## Installation

Follow these steps to set up the **urlradar** project locally or deploy it to the cloud.

### Prerequisites

Ensure the following tools are installed:

- [Docker v27.4.1](https://www.docker.com/get-started): (or latest).
- [Java 23](https://docs.aws.amazon.com/corretto/latest/corretto-23-ug/downloads-list.html): (or latest).

### Local Setup with Docker

To run the project locally using Docker:

1. Clone the repository:
    ```bash
    git clone https://github.com/rblessings/urlradar.git
    cd urlradar
    ```

2. Run the **unit and integration tests** by executing the following command:
    ```bash
   ./gradlew clean build
   ```

3. Build and run the Docker containers:
    ```bash
    docker compose up --build -d
    ```

   This will start the following services:
    - **MongoDB**: Stores user data and redirection records.
    - **Redis**: Caching layer for efficient URL lookups.
    - **Kafka**: handles event streaming for distributed communication, enabling real-time analytics on user data and
      URL performance with low-latency, high-throughput processing.


4. To start the **Backend (API)** Core URL redirection and analytics service:
    ```bash
   SPRING_PROFILES_ACTIVE=dev ./gradlew clean bootRun
   ```

   **Note:** This command automatically runs the `compose.yaml` file to start the Backend (API) service and its
   dependencies, offering an alternative to `docker compose up --build -d`.

---

## Usage

### API Usage

#### Fetch OpenID Configuration

- The following `curl` command is used to retrieve the OpenID configuration from the authorization server:

    ```bash
    curl http://localhost:8080/.well-known/openid-configuration
    ```

#### OAuth2 Client Credentials Flow: JWT Token Request

To obtain a JWT token using the OAuth2 client credentials flow:

1. **Base64 Encode Client Credentials**

   First, base64 encode your client credentials (`client:secret`) to generate the `Authorization` header:

    ```bash
    echo -n "client:secret" | base64
    ```

   This will output the base64-encoded string:

    ```
    Y2xpZW50OnNlY3JldA==
    ```

2. **Request the JWT Token**

   Once you have the base64-encoded credentials, use the following `curl` command to request a JWT token from the OAuth2
   server:

    ```bash
    curl -X POST 'http://localhost:8080/oauth2/token' \
    --header 'Authorization: Basic Y2xpZW50OnNlY3JldA==' \
    --header 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode 'grant_type=client_credentials' \
    --data-urlencode 'scope=apis'
    ```

   **Explanation**:
    - `Authorization: Basic Y2xpZW50OnNlY3JldA==`: The base64-encoded client credentials (`client:secret`).
    - `Content-Type: application/x-www-form-urlencoded`: Specifies the content type of the request.
    - `grant_type=client_credentials`: The grant type used for this request.
    - `scope=apis`: Defines the scope for the token.

   This command will return a JSON response containing the access token if the credentials are valid.

#### Example Output

If the request is successful, the response will contain an access token in the JSON format:

```json
{
  "access_token": "<jwt-token>",
  "token_type": "Bearer",
  "expires_in": 299,
  "scope": "apis"
}
```

#### Request the Protected Resource Using a JWT Token

After obtaining the JWT token, you can use it to access a protected resource, such as an API endpoint. To do this, send
an HTTP `GET` request to the desired endpoint, passing the token as a Bearer token in the `Authorization` header.

#### Example `curl` Command:

```bash
curl -v http://localhost:8080/api/v1/principal -H "Authorization: Bearer <your-jwt-token>"
```

---

## License

This project is open-source software released under the [MIT License](https://opensource.org/licenses/MIT).
