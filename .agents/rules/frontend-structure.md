# Frontend Project Architecture & Structure (`basic-lifes-support-app`)

This document outlines the directory layout, architectural conventions, and AI agent guidelines for the **Basic Life Support (BLS) Scheduler** frontend application.

---

## 1. Overview & Tech Stack

- **Project Name**: `basic-lifes-support-app`
- **Framework**: Next.js (App Router)
- **Language**: TypeScript (`tsconfig.json`)
- **Styling / Theming**: Centralized theme tokens (`theme.ts`)
- **Deployment Target**: Google Cloud App Engine (`app.yaml`)
- **Backend Integration**: Consumes REST APIs exposed by the Spring Boot `blsscheduler` service.

---

## 2. Directory Structure

```text
basic-lifes-support-app/
├── .idea/                      # JetBrains IDE workspace configuration
├── .next/                      # Next.js build output & cache (gitignored)
├── app/                        # Next.js App Router (pages, layouts, loading/error states, route handlers)
├── components/                 # Reusable, modular UI components
├── context/                    # React Context providers for global client state (e.g., Auth, User session)
├── lib/                        # Core utilities, HTTP client setup, formatters, and helper functions
├── node_modules/               # Installed npm dependencies (gitignored)
├── public/                     # Static public assets (images, icons, fonts)
├── services/                   # API service modules for communicating with the `blsscheduler` backend
├── types/                      # Global TypeScript interfaces, enums, and DTO type definitions
├── .env                        # Local environment variables (gitignored)
├── .env.example                # Template documenting required environment variables
├── .gitignore                  # Git ignore rules
├── .node-version               # Node.js runtime version specification
├── .nvmrc                      # Node Version Manager (nvm) configuration
├── app.yaml                    # Google App Engine deployment configuration
├── basiclifesupport.iml        # IntelliJ/WebStorm module file
├── implementation_plan.md      # Feature roadmap and architectural planning notes
├── next-env.d.ts               # Next.js TypeScript declaration file
├── package.json                # Project dependencies and npm scripts
├── package-lock.json           # Locked dependency tree
├── README.md                   # Setup instructions and project overview
├── theme.ts                    # Centralized UI theme configuration (colors, typography, spacing)
├── tsconfig.json               # TypeScript compiler configuration
├── tsconfig.tsbuildinfo        # TypeScript incremental build cache
└── walkthrough.md              # Developer onboarding and feature walkthrough documentation
```

---

## 3. Architectural Conventions & Agent Rules

When generating or modifying code in `basic-lifes-support-app`, follow these strict rules:

1. **Routing & Pages (`app/`)**:
   - Use Next.js App Router conventions (`page.tsx`, `layout.tsx`, `loading.tsx`, `error.tsx`).
   - Keep route files lightweight; delegate complex UI rendering to `components/` and data fetching to `services/`.

2. **API Layer (`services/`)**:
   - **Never** make raw `fetch` or HTTP client calls directly inside `app/` pages or `components/`.
   - Encapsulate all communication with the `blsscheduler` backend inside dedicated service modules under `services/`.

3. **Type Safety (`types/`)**:
   - Define strict TypeScript interfaces in `types/` that mirror the backend `dto/` request and response contracts.
   - Avoid using `any`.

4. **State Management (`context/`)**:
   - Use React Context providers in `context/` for cross-cutting concerns (such as authentication tokens, active user profiles, and global notifications).

5. **Styling & Design System (`theme.ts`)**:
   - Adhere to the design tokens, color palette, and typography rules configured in `theme.ts` rather than hardcoding arbitrary values.

6. **Environment Configuration (`.env` / `.env.example`)**:
   - Reference backend base URLs and public keys via environment variables (`NEXT_PUBLIC_*`). Whenever adding a new variable to `.env`, document a placeholder in `.env.example`.