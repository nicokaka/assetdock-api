<div align="center">

🌐 [English](README.md) · **Português**

# 🔒 AssetDock API

**Plataforma de Inventário de Ativos Multi-Tenant com Segurança em Primeiro Lugar**

[![CI](https://github.com/nicokaka/assetdock-api/actions/workflows/ci.yml/badge.svg)](https://github.com/nicokaka/assetdock-api/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot 3](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL 17](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![License](https://img.shields.io/badge/Licen%C3%A7a-MIT-green)

Uma API backend de nível profissional para gerenciamento de ativos de hardware e software entre organizações.  
Construída como **projeto de portfólio backend/security** em torno de **isolamento de tenants**, **controle de acesso baseado em papéis (RBAC)**, **trilhas de auditoria imutáveis** e **design de API resistente a abuso**.

[Por que ele importa](#por-que-ele-importa) · [Arquitetura](#arquitetura) · [Modelo de Segurança](#segurança--multi-tenancy) · [Docs de Security Assurance](#docs-de-security-assurance) · [Como Começar](#como-começar)

</div>

---

## Por que o AssetDock?

Organizações precisam de um sistema confiável e auditável para rastrear **quem tem o quê, quando foi atribuído e o que mudou**. O AssetDock resolve isso com:

- 🏢 **Isolamento multi-tenant** — os dados de cada organização são logicamente separados no nível de consulta; acesso entre tenants é negado por design.
- 🛡️ **Autorização baseada em papéis** — cinco papéis distintos aplicam o princípio do menor privilégio em todos os endpoints.
- 📋 **Logs de auditoria imutáveis** — ações críticas de auth, gestão de usuários, atribuição, importação e ciclo de vida são registradas com ator, timestamp e contexto do tenant.
- 🚦 **Controles de abuso** — lockout por falhas de login, throttling de endpoints, queries limitadas e import CSV validado endurecem a superfície pública da API.
- 📦 **Importação em massa via CSV** — upload de inventários de ativos com validação por linha, limites de tamanho e semântica de sucesso parcial.

## Por que ele importa

O AssetDock foi pensado para demonstrar partes de engenharia backend que realmente importam em sistemas sensíveis à segurança:

- design de API seguro por padrão, e não apenas CRUD de happy path
- fronteiras explícitas de tenant e de papéis na camada de aplicação
- fluxos auditáveis de autenticação e administração
- tratamento realista de abuso em login, importações e leituras grandes
- testes de integração com PostgreSQL real e foco em comportamento operacional

---

## Arquitetura

```
┌─────────────────────────────────────────────────────────┐
│                     AssetDock API                       │
│                  Monólito Modular                       │
├──────────┬──────────┬──────────┬────────────┬───────────┤
│   Auth   │  Asset   │ Catalog  │ Assignment │  Importer │
│  Módulo  │  Módulo  │  Módulo  │   Módulo   │   Módulo  │
├──────────┼──────────┼──────────┼────────────┼───────────┤
│   User   │   Org    │  Audit   │  Security  │  Common   │
│  Módulo  │  Módulo  │  Módulo  │ (compart.) │ (compart.)│
├──────────┴──────────┴──────────┴────────────┴───────────┤
│         Spring Security + JWT (HMAC / HS256)            │
├─────────────────────────────────────────────────────────┤
│           PostgreSQL 17 + Flyway Migrations             │
└─────────────────────────────────────────────────────────┘
```

| Princípio | Implementação |
|---|---|
| **Monólito Modular** | Cada módulo de domínio segue a camada `api → application → domain → infrastructure` |
| **Multi-Tenancy em Schema Compartilhado** | Isolamento de tenant via `organization_id` em todas as entidades com escopo |
| **Migrações Somente Adiante** | DDL gerenciado pelo Flyway — sem SQL manual, sem scripts de rollback |
| **Empacotamento Domain-Driven** | 8 contextos delimitados: `auth`, `user`, `organization`, `catalog`, `asset`, `assignment`, `importer`, `audit` |
| **Architecture Decision Records** | Trade-offs documentados em `docs/adr/` (ex: estratégia de unicidade global de email) |

---

## Segurança & Multi-Tenancy

> Segurança não é uma feature — é a arquitetura.

### Controle de Acesso Baseado em Papéis (RBAC)

| Papel | Escopo | Capacidades |
|---|---|---|
| `SUPER_ADMIN` | Global | Acesso total ao sistema, operações entre tenants |
| `ORG_ADMIN` | Tenant | Gerenciar usuários, ativos e catálogos dentro da organização |
| `ASSET_MANAGER` | Tenant | CRUD em ativos, atribuições e importações |
| `AUDITOR` | Tenant | Acesso somente leitura aos logs de auditoria |
| `VIEWER` | Tenant | Acesso somente leitura a ativos e catálogos |

### Garantias de Isolamento de Tenant

- ✅ Toda operação de leitura e escrita é delimitada ao tenant autenticado
- ✅ Acesso a dados de outro tenant é sistematicamente negado
- ✅ Logs de auditoria capturam `organizationId`, `actorId`, `eventType` e `timestamp`
- ✅ Respostas de erro padronizadas seguem o formato **RFC 9457 (Problem Details)**

### Fluxo de Autenticação

```
Cliente ──► POST /api/v1/auth/login (credenciais)
        ◄── Token de acesso JWT (assinado, com expiração)
Cliente ──► GET /assets (Authorization: Bearer <token>)
        ◄── Resposta com escopo de tenant
```

## Destaques de Segurança

- autorização tenant-aware aplicada na camada de serviço
- isolamento em schema compartilhado com `organization_id` nas entidades com escopo
- trilha de auditoria persistida para auth, gestão de usuários, ciclo de vida, atribuições e importações
- lockout de conta e throttling de endpoint para resistência a abuso
- importações e leituras limitadas para reduzir superfície operacional de abuso
- documentação pública desabilitada por padrão e respostas públicas sanitizadas

## Docs de Security Assurance

- [Threat Model](docs/security/threat-model.md)
- [Trust Boundaries](docs/security/trust-boundaries.md)
- [Abuse Cases](docs/security/abuse-cases.md)
- [Security Decisions](docs/security/security-decisions.md)
- [Changelog](CHANGELOG.md)

---

## Módulos Implementados

Todos os endpoints de negócio são tenant-aware. O `organization_id` do usuário autenticado é aplicado automaticamente.

| Módulo | Escopo |
|---|---|
| **Auth** | Login JWT, rastreamento de falhas, lockout e throttling de login |
| **Organização** | Leitura tenant-scoped da organização |
| **Usuário** | Ciclo de vida do usuário, atualização de papéis e mudanças protegidas de status |
| **Catálogo** | Categories, manufacturers e locations com controles de ciclo de vida |
| **Ativos** | Gerenciamento de ciclo de vida de ativos com proteção de archive |
| **Atribuições** | Fluxos de atribuição/desatribuição com validações de integridade |
| **Importação** | Importação CSV com processamento síncrono e limitado |
| **Auditoria** | Eventos persistidos e consultáveis de segurança/auditoria |

> 📖 **Documentação interativa** habilitada nos perfis `local` e `test`, ou quando `PUBLIC_DOCS_ENABLED=true`.

---

## Estratégia de Testes

O projeto inclui cobertura unitária e de integração com **JUnit 5** e **Testcontainers** (PostgreSQL real para fluxos integrados).

| Camada de Teste | O que é Coberto |
|---|---|
| **Testes de Integração** | Login de auth, CRUD de ativos, atribuições, gerenciamento de catálogo, importações CSV, leitura/escrita de logs de auditoria, gerenciamento de usuários |
| **Testes Unitários** | Lógica do serviço de autenticação, serviços de catálogo, gerenciamento de usuários, tratamento de erros (ProblemDetail factory), seed runner de desenvolvimento |

```bash
# Executar a suíte completa de testes contra um container PostgreSQL real
./gradlew clean check
```

CI roda automaticamente em todo push e pull request via **GitHub Actions**.

---

## Stack Tecnológica

| Camada | Tecnologia |
|---|---|
| **Linguagem** | Java 21 (LTS) |
| **Framework** | Spring Boot 3.5, Spring Security, Spring JDBC |
| **Autenticação** | JWT com `spring-security-oauth2-jose` |
| **Banco de Dados** | PostgreSQL 17 (Alpine) |
| **Migrações** | Flyway |
| **Documentação da API** | SpringDoc OpenAPI (Swagger UI) |
| **Monitoramento** | Spring Boot Actuator |
| **Importação em Massa** | Apache Commons CSV |
| **Testes** | JUnit 5, Testcontainers |
| **CI/CD** | GitHub Actions |
| **Containerização** | Docker Compose |

---

## Como Começar

### Pré-requisitos

- **Java 21 JDK** (Temurin recomendado)
- **Docker** + **Docker Compose**

### 1. Clone e configure

```bash
git clone https://github.com/nicokaka/assetdock-api.git
cd assetdock-api
cp .env.example .env   # configure banco de dados e JWT_SECRET
```

### 2. Inicie o banco de dados

```bash
docker compose up -d
```

### 3. Execute os testes

```bash
./gradlew clean check
```

### 4. Inicie o servidor

```bash
./gradlew bootRun
```

A API estará disponível em `http://localhost:8080`.  
Swagger UI em `http://localhost:8080/swagger-ui.html`.

---

## Roadmap do Projeto

### ✅ MVP — Concluído

- Inventário de ativos multi-tenant com CRUD completo
- Autenticação JWT e RBAC (5 papéis)
- Fluxo de atribuição de ativos com histórico
- Importação em massa via CSV com validação e sucesso parcial
- Logs de auditoria imutáveis com endpoints de leitura
- Pipeline de CI com Testcontainers

### 🔮 Melhorias Futuras

- Rotação de refresh token e revogação de tokens
- Autenticação multifator (MFA / TOTP)
- Processamento de jobs em background para importações grandes
- Motor de importação genérico entre domínios
- Aplicação frontend (React / Next.js)

---

## Estrutura do Projeto

```
assetdock-api/
├── src/main/java/com/assetdock/api/
│   ├── auth/           # Autenticação & JWT
│   ├── user/           # Gestão do ciclo de vida do usuário
│   ├── organization/   # Gerenciamento de tenants
│   ├── catalog/        # Categorias, fabricantes, localizações
│   ├── asset/          # CRUD principal de ativos
│   ├── assignment/     # Atribuição de ativos & histórico
│   ├── importer/       # Motor de importação CSV em massa
│   ├── audit/          # Trilha de auditoria imutável
│   ├── security/       # Filtros de segurança & configuração compartilhada
│   ├── common/         # Utilitários compartilhados & tratamento de erros
│   └── config/         # Configuração da aplicação
├── src/test/           # Testes unitários e de integração
├── docs/adr/           # Architecture Decision Records
├── .github/workflows/  # Workflows de CI e segurança
├── docker-compose.yml  # PostgreSQL local
└── build.gradle        # Configuração de build Gradle
```

---

<div align="center">

**Construído com 🔒 segurança no centro.**

*Projetado como um backend de nível portfólio demonstrando padrões enterprise:  
multi-tenancy, RBAC, conformidade de auditoria, design orientado a domínio e testes automatizados.*

</div>
