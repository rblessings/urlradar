# urlradar - Secure & Seamless URL Redirection Service

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
    - **MongoDB**: NoSQL database for storing redirection data, analytics, and user profiles.
    - **Elasticsearch**: Enables efficient full-text search and indexing of URLs for fast lookups and analytics.

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
    ```

2. Build and run the Docker containers:
    ```bash
    docker compose up --build -d
    ```

   This will start the following services:
    - **Backend (API)**: Core URL redirection service.
    - **MongoDB**: Stores user data and redirection records.
    - **Redis**: Caching layer for efficient URL lookups.
    - **Kafka**: Manages event streaming for distributed communication.
    - **Elasticsearch**: Provides real-time analytics for user data and URL performance.

---

## Usage

### API Usage

_Coming soon..._

---

## License

This project is open-source software released under the [MIT License](https://opensource.org/licenses/MIT).
