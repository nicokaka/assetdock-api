# SAVE STATE — AssetDock API (15 de Maio de 2026)

## Status Atual: MVP Completo — v1.1.0 em `main`

Branch: `main` · Commit: `1265571` · Build: PASSING

Ambos os repositórios (`assetdock-api` e `assetdock-web`) estão sincronizados com o GitHub.

---

## O que foi implementado (histórico consolidado)

### Módulos do backend (11 bounded contexts)
- `auth` — Autenticação dual: JWT HS256 (M2M) + Cookie Sessions (web), CSRF, throttling, brute-force lock
- `user` — CRUD completo, 5 roles RBAC, password change/reset admin, auto-lock
- `organization` — Gestão de tenants
- `catalog` — Categories, Manufacturers, Locations (soft-delete)
- `asset` — CRUD, status lifecycle, archive, export
- `assignment` — Atribuição de ativos com histórico
- `checkout` — Checkout/Checkin com pessimistic locking, timeline, dashboard KPI
- `importer` — Import CSV em bulk (bounded, throttled, parcial success)
- `audit` — Trail imutável, 30 event types, paginado e filtrado
- `dashboard` — KPIs de ativos, usuários e checkouts ativos
- `search` — Busca global (Cmd+K, trigram indexes)
- `setup` — Wizard de primeiro uso self-hosted (advisory lock)

### Hardening de produção
- Lock pessimista em checkout/checkin (SELECT ... FOR UPDATE)
- Secure cookies forçados em production profile
- Rate limiting: login (10/min), import (5/5min), setup (3/10min)
- Índices parciais no PostgreSQL para checkouts ativos e busca textual
- Session cleanup automático (cron horário)
- RFC 9457 ProblemDetail em todos os erros

### Testes (29 arquivos)
- Integration tests: auth, web session, CSRF, user, org, catalog, asset, assignment, checkout, audit, importer, setup, CORS, contract
- Unit tests: AuthenticationService, CatalogServices, UserManagementService, ProblemDetailFactory, SeedRunner

### Documentação
- README.md / README.pt-BR.md
- INSTALL.md, DEPLOY_GUIDE.md, SELFHOSTED.md, RUNBOOK.md
- docs/security/ (threat model, trust boundaries, abuse cases, security decisions)
- docs/adr/ (ADR-001: global unique email)

---

## Próximos passos (se retomar)

1. Avaliar Dependabot PRs pendentes (zxing, springdoc, Spring Boot 4, GitHub Actions)
2. Fase 3 — funcionalidades futuras:
   - Notificações por e-mail (SMTP) para eventos críticos
   - MFA (Multi-Factor Authentication)
   - Relatórios e exportação avançada
   - Refresh token rotation
