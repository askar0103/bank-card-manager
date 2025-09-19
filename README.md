# 🚀 Bank Card Manager

A bank card management system built with Spring Boot, featuring role-based access control, card
operations, and internal money transfers.

![Java](https://img.shields.io/badge/Java-17+-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-green.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)
![Docker](https://img.shields.io/badge/Docker-Compose-blue.svg)
![License](https://img.shields.io/badge/License-MIT-green.svg)

## 📖 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [API Documentation](#-api-documentation)
- [Authentication](#-authentication)
- [Database Schema](#-database-schema)

## ✨ Features

### Administrator Capabilities

- ✅ Create, activate, block, and delete cards
- ✅ Manage users and their permissions
- ✅ View all cards in the system

### User Capabilities

- ✅ View personal cards with search and pagination
- ✅ Request card blocking
- ✅ Transfer money between own cards
- ✅ View account balance
- ✅ Secure authentication with JWT

## 🛠 Tech Stack

- **Backend Framework**: Spring Boot 3.5.x
- **Language**: Java 17
- **Security**: Spring Security with JWT
- **Persistence**: Spring Data JPA, Hibernate
- **Database**: PostgreSQL 16
- **Migrations**: Liquibase
- **API Documentation**: OpenAPI 3.1 (Swagger)
- **Containerization**: Docker, Docker Compose
- **Testing**: JUnit 5, Mockito, MockMvc

## ⚡ Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 17+ (for local development)
- Maven 3.6+ (for local development)

### Running with Docker (Recommended)

1. **Clone the repository**
   ```bash
   git clone https://github.com/askar0103/bank-card-manager.git
   cd bank-card-manager
   ```

2. **Set up environment variables**
   ```bash
   cp .env.example .env
   # Edit .env file if needed (default values should work)
   ```

3. **Start the application**
   ```bash
   docker-compose up --build
   ```

4. **Access the application**
    - Application: http://localhost:8080
    - Swagger UI: http://localhost:8080/swagger-ui/index.html
    - Database: localhost:5432 (credentials in .env file)

5. **Stop the application**
   ```bash
   docker-compose down -v
   ```
   > `-v` removes volumes (database data will be erased).

## ⚙️ Configuration

### Environment Variables

Create a `.env` file based on `.env.example`:

```properties
# Database
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=bankcards
POSTGRES_USER=admin
POSTGRES_PASSWORD=password
# Security
CARD_ENCRYPTOR_PASSWORD=encryption_password
CARD_ENCRYPTOR_SALT=encryption_salt
CARD_HASHER_SECRET_KEY=hashing_secret
# JWT
JWT_SECRET_KEY=jwt_super_secret_key
JWT_EXPIRATION_SECONDS=3600
# ⚠️ JWT_SECRET_KEY must be at least 256 bits long for HS256 algorithm
# Application
SERVER_PORT=8080
```

### Application Properties

Main configuration file: `src/main/resources/application.yml`

## 📋 API Documentation

Interactive API documentation is available at:
http://localhost:8080/swagger-ui/index.html

OpenAPI specification:
http://localhost:8080/v3/api-docs

## 🔐 Authentication

The system uses JWT (JSON Web Tokens) for authentication.

### Flow:

1. Obtain token via `/api/auth/login` endpoint
2. Include token in subsequent requests: `Authorization: Bearer <token>`
3. Token expiration: 1 hour (configurable)

### Roles:

- **ADMIN**: Full system access
- **USER**: Personal card management only

## 🗄 Database Schema

### Key Tables:

- `users` - User accounts and credentials
- `cards` - Bank card information

### Migrations:

Database migrations are handled by Liquibase and located in:
`src/main/resources/db/migration/`

Migrations run automatically on application startup.