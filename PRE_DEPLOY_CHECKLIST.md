# Checklist AssetDock — Pré-Deploy

## Backend (assetdock-api)
- [ ] O `./gradlew check` passou sem erros? (Isso rodará todos os testes, incluindo o `PublicEndpointsSecurityTest` para garantir que o Spring Security não sofreu regressões).
- [ ] O `./gradlew build` compilou com sucesso sem warnings críticos?
- [ ] Flyway: As migrations de banco de dados não possuem conflitos de versão?
- [ ] O `.env.example` foi atualizado se você introduziu novas variáveis de ambiente neste ciclo de desenvolvimento?
- [ ] O `RUNBOOK.md` reflete a arquitetura e as portas atuais?
- [ ] Verificação Rápida Pós-Boot: 
  - Actuator (sem auth) está respondendo: `curl http://localhost:8081/actuator/health`
  - Endpoint de Login não está bloqueado com 403: `curl -X POST http://localhost:8080/api/v1/web/auth/login` (esperado: 401 ou 400).

## Frontend (assetdock-web)
- [ ] O `npm run build` compilou sem erros TypeScript?
- [ ] O `VITE_API_URL` está configurado **com** o path `/api/v1`?
- [ ] O `VITE_MANAGEMENT_URL` está apontando corretamente para a porta 8081 do backend?
- [ ] A badge indicadora de saúde "API Online/Offline" reflete o status real quando o backend sobe ou cai?
- [ ] A tela de login está permitindo o acesso com credenciais válidas digitadas manualmente?
- [ ] O teste E2E do Playwright validou o health check com sucesso? (`npx playwright test e2e/health-check.spec.ts`).
