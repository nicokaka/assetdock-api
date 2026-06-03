# Runbook AssetDock API

## Pré-requisitos
- Java 21
- Docker com PostgreSQL na porta 5432
- Arquivo `.env` na raiz (copiar de `.env.example`)

## Portas
| Porta | Uso | Context-path | Observações |
|---|---|---|---|
| 8080 | API principal (Spring Security ativo) | /api/v1 | Apenas `/actuator/health` está público. Outros endpoints de `/actuator` exigem autenticação. |
| 8081 | Management / Actuator (porta interna) | / | Porta administrativa, não exposta externamente. |

## Inicialização
```bash
./gradlew bootRun
```

## Verificação rápida
```bash
# Verifica se a aplicacao esta online (Management Port)
curl http://localhost:8081/actuator/health   # deve retornar {"status":"UP"}

# Verifica se o endpoint de login esta aberto e o banco esta acessivel (API Port)
# Um 401 Unauthorized indica que o banco recusou as credenciais (ou seja, o endpoint esta publico). 
# Um 403 Forbidden indicaria um erro de configuracao do Spring Security.
curl -X POST http://localhost:8080/api/v1/web/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com", "password":"wrongpassword"}'
```
