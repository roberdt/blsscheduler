# Project Architecture & Structure

This document outlines the directory layout and architectural conventions for the **Basic Life Support (BLS) Scheduler** full-stack application. It serves as a reference guide for AI agents (including Antigravity / Gemini) and developers working across the repository.

---

## 1. System Overview

The project is split into two primary modules:
- **Backend (`blsscheduler`)**: A Java Spring Boot REST API built with Maven, responsible for business logic, security, and database persistence.
- **Frontend (`basic-lifes-support-app`)**: A Next.js (App Router) TypeScript web application responsible for the user interface, client state, and API consumption.

---

## 2. Backend Architecture (`blsscheduler`)

- **Base Package**: `info.safetynetaccess.blsscheduler`
- **Build Tool**: Maven (`pom.xml`, `./mvnw`)
- **Configuration**: `src/main/resources/application.yaml`
- **Containerization**: `Dockerfile`

### Directory Tree

```text
blsscheduler/
├── .agents/                         # Agent rules, workflows, and project guidance
│   └── rules/                       # Modular rules and conventions for AI agents
│       ├── backend-structure.md     # Project architecture & backend directory structure
│       ├── coding-standards.md      # Java & TypeScript coding standards
│       ├── frontend-structure.md    # Frontend architecture & directory structure
│       └── tech.md                  # Tech stack, Firestore database IDs & email standards
├── .gitattributes                   # Git attribute settings
├── .gitignore                       # Git ignore patterns
├── .idea/                           # JetBrains IDE configuration (gitignored)
├── .mvn/                            # Maven wrapper files
│   └── wrapper/
│       └── maven-wrapper.properties # Maven wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── info/safetynetaccess/blsscheduler/
│   │   │       ├── config/          # Spring & application configurations (CORS, Swagger, Beans)
│   │   │       ├── controller/      # REST API endpoints (@RestController)
│   │   │       ├── dto/             # Data Transfer Objects (Request/Response payloads)
│   │   │       ├── entity/          # JPA database entities (@Entity)
│   │   │       ├── filter/          # HTTP request/response filters (e.g., JWT auth filters)
│   │   │       ├── model/           # Domain models & internal data structures
│   │   │       ├── repository/      # Spring Data JPA repositories
│   │   │       ├── security/        # Spring Security rules, auth providers, user details
│   │   │       ├── service/         # Core business logic layer
│   │   │       ├── util/            # Reusable helper & utility classes
│   │   │       └── BlsschedulerApplication.java  # Spring Boot entry point
│   │   └── resources/
│   │       ├── static/
│   │       │   └── index.html       # Static web assets
│   │       ├── templates/           # Server-side templates (if applicable)
│   │       └── application.yaml     # Environment, database, and server properties
│   └── test/                        # Unit and integration tests
│       └── java/
│           └── info/safetynetaccess/blsscheduler/
│               ├── controller/      # Controller unit and integration tests
│               └── service/         # Service unit and integration tests
├── target/                          # Compiled build output (gitignored)
├── Dockerfile                       # Container definition
├── GEMINI.md                        # Master project context, system overview & agent rules
├── HELP.md
├── mvnw / mvnw.cmd
├── pom.xml
└── README.md