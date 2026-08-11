# Agent Instructions for AssetDock API

This file provides the persistent context and operational rules for any AI agent interacting with the `assetdock-api` repository.

## Repository Context
* **Project**: AssetDock API (Frontend interface).
* **Backend Companion**: `assetdock-api` (resides in a separate repository alongside this one).
* **Current Status**: The web MVP is **completed** and consolidated in `main`.
* **MVP Features Included**: Cookie-based session authentication, Assets, Assignments, Users, Audit Logs, Imports, Playwright E2E smoke tests, and a consolidated visual pass.
* **Architecture**: Java 21, Spring Boot 3.5, PostgreSQL 17, Spring Security. 

## Core Principles
1. **No Scope Creep**: Do not invent or open new large functional scopes without explicit user instruction. The MVP is done.
2. **Technical & Serious**: Maintain a technical, direct, and professional tone. Avoid marketing buzzwords, emojis, or "AI scaffold" appearances.
3. **B2B Aesthetic**: The UI must remain serious, simple, intuitive, and clean (B2B Admin panel style). Do not inflate the component architecture unnecessarily.
4. **Incremental Changes**: Favor small, well-defined tasks using feature/fix branches. 
5. **No Hallucinations**: Do not write invented documentation, fake roadmaps, obvious comments, or fake placeholder screenshots.

## Server Deployment Commands (Debian Self-Hosted)
* **Debian Server IP**: `192.168.97.10:5003`
* **Docker Compose Services**: `postgres` (container `assetdock-db`), `api` (container `assetdock-api`), `web` (container `assetdock-web`).
* **Deploy Web Updates**:
  `cd ~/assetdock/assetdock-web && git pull && docker compose -f ../assetdock-api/docker-compose.selfhosted.yml --env-file ../assetdock-api/.env.selfhosted up -d --build web`
* **Deploy Full Stack Updates**:
  `cd ~/assetdock/assetdock-api && git pull && docker compose -f docker-compose.selfhosted.yml --env-file .env.selfhosted up -d --build`

## Validation Requirements
Every proposed change, no matter how small, **must** be validated prior to completion:
* Run `./gradlew check` to ensure unit and integration tests pass.
* Run `./gradlew build` to ensure Gradle successfully compiles the Java app.
* Verify behavior in runtime when applicable.

## Task Completion Protocol
At the end of every task execution, the agent must output a structured summary containing exactly:
1. What was done.
2. Files modified/added/deleted.
3. Results of validations (`build` and `lint`).
4. Current branch name.
5. Upstream tracking branch.
6. Last commit hash and message.
7. Current working tree status.
