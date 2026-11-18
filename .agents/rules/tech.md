---
trigger: model_decision
description: Tech stack versions, Firebase configuration, environment variables, Maven/npm build commands, and email service standards.
---

# Tech Stack & Environment (`tech.md`)

## Backend (`blsscheduler`)
- **Language & Framework**: Java, Spring Boot, Maven (`./mvnw spring-boot:run`)
- **Database & Auth**: Firebase Admin SDK (Firestore & Firebase Auth token verification)
- **Config**: `src/main/resources/application.yaml`

## Frontend (`basic-lifes-support-app`)
- **Framework**: Next.js (App Router), TypeScript (`npm run dev`, `npm run build`)
- **Database & Auth**: Firebase Client SDK (`lib/`)
- **Styling & Hosting**: `theme.ts`, Google Cloud App Engine (`app.yaml`)

## Cloud Firestore Databases (GCP Project: BLS Training Scheduler)
- **Important**: This project does **not** use the `(default)` Firestore database. Always pass an explicit `databaseId` when initializing Firestore clients.
- **Production Database ID**: `bls-sched-db` (`us-central1`, Standard Native Mode)
- **Test / Development Database ID**: `bls-sched-db-test` (`us-central1`, Standard Native Mode)
- **Backend Configuration**: Controlled via `firebase.firestore.database-id` (`FIRESTORE_DATABASE_ID` env var) in `application.yaml` and injected via `FirestoreOptions.newBuilder().setDatabaseId(...)` in `config/`.
- **Frontend Configuration**: Controlled via `NEXT_PUBLIC_FIRESTORE_DATABASE_ID` in `.env` and passed to `getFirestore(app, databaseId)` in `lib/`.

## Email Standards & Configuration
- **Protocol & Provider**: SMTP via Gmail (`smtp.gmail.com:587`, STARTTLS enabled).
- **Credentials & Secrets**:
  - Retrieved via GCP Secret Manager (`sm://gmail-app-password`) with fallback to environment variables (`SPRING_MAIL_PASSWORD` / `MAIL_PASSWORD`).
  - Never hardcode email passwords or sensitive mail configurations in code or commit them to source control.
- **Mail Properties (`application.yaml`)**:
  - `spring.mail.username`: Authenticated SMTP user (`safetynet@safetynetaccess.org`).
  - `app.mail.from`: Sender email address (`safetynet@safetynetaccess.org`).
  - `app.mail.sender-name`: Branded display name (`"Safety Net Access BLS Scheduler"`).
  - `app.mail.admin-notification-email`: Admin alert recipient (`ADMIN_NOTIFICATION_EMAIL` env var, default: `rerdt@safetynetaccess.info`).
- **Reactive & Non-Blocking Execution**:
  - `JavaMailSender` is synchronous and blocking.
  - Always execute mail dispatch inside `Mono.fromRunnable(...)` scheduled on `Schedulers.boundedElastic()` to prevent blocking Spring WebFlux event loop threads.
- **Message Structure & Templating**:
  - Send messages as `MimeMessage` using UTF-8 (`StandardCharsets.UTF_8`) and `MULTIPART_MODE_MIXED_RELATED`.
  - Always include both responsive HTML content and a plain-text fallback.
  - Escape all dynamic user and company input strings before interpolating into HTML templates to prevent injection.
- **Validation & Error Handling**:
  - Validate email fields on incoming DTOs using `@NotBlank` and `@Email` (`jakarta.validation.constraints.*`).
  - Isolate email dispatch errors with logging so mail delivery issues do not fail primary transactional operations (e.g., user or company registration).