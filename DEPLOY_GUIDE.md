# AssetDock — Guia Completo de Deploy Corporativo

**Versão:** 1.1.0  
**Data:** 07 de Maio de 2026  
**Classificação:** Documento Técnico-Operacional  
**Destinatário:** Equipe de TI / Gestão

---

## 1. Visão Geral do Sistema

O AssetDock é um sistema web de **gestão de patrimônio** (ativos de TI) para uso interno da empresa. Funciona 100% on-premise — nenhum dado sai da rede da empresa.

### Componentes de Runtime

O sistema é composto por **4 containers Docker** que trabalham juntos:

```
┌─────────────────────────────────────────────────────────────┐
│                    Rede Docker (isolada)                      │
│                                                              │
│  ┌─────────────┐    ┌──────────────────┐    ┌────────────┐  │
│  │   Web        │    │   API             │    │ PostgreSQL │  │
│  │   (nginx)    │───▶│   (Java 21)       │───▶│  (v17)     │  │
│  │   ~15 MB RAM │    │   ~256 MB RAM     │    │  ~64 MB    │  │
│  │   Porta 3000 │    │   Porta 8080      │    │  RAM       │  │
│  │   (exposta)  │    │   (interna)       │    │  (interna) │  │
│  └─────────────┘    └──────────────────┘    └────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────────┐│
│  │  Backup (pg_dump diário, retenção 7 dias)  ~5 MB RAM     ││
│  └──────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
                            ▲
                            │ Porta 3000 (HTTP)
                            │
                    ┌───────┴───────┐
                    │  Navegadores  │
                    │  dos usuários │
                    └───────────────┘
```

| Container | Imagem | Função | RAM (típica) | Disco |
|---|---|---|---|---|
| `assetdock-web` | nginx:alpine | Serve a interface React + proxy para API | ~15 MB | ~30 MB |
| `assetdock-api` | eclipse-temurin:21-jre-alpine | Backend Java/Spring Boot | ~256 MB | ~180 MB |
| `assetdock-db` | postgres:17-alpine | Banco de dados relacional | ~64 MB | ~50 MB + dados |
| `assetdock-backup` | postgres-backup-local | Backup diário automático | ~5 MB | Proporcional ao BD |

**Consumo total estimado:** ~340 MB de RAM + ~260 MB de imagens Docker.

---

## 2. Escolha da Máquina — Linux vs Windows

### Recomendação: **Linux (Ubuntu Server 24.04 LTS)**

> [!IMPORTANT]
> Todas as imagens Docker do AssetDock são baseadas em Alpine Linux. Elas rodam **nativamente** no Linux, sem camada de virtualização. No Windows, o Docker Desktop cria uma máquina virtual WSL2 internamente, adicionando overhead de RAM e disco.

| Critério | Linux (Ubuntu Server) | Windows 10/11 |
|---|---|---|
| **Performance** | ✅ Nativa, sem overhead | ⚠️ WSL2 adiciona ~500 MB de overhead |
| **RAM mínima necessária** | **2 GB** | **4 GB** (WSL2 consome ~1.5 GB) |
| **Consumo ocioso** | ~350 MB (SO + Docker + AssetDock) | ~2.5 GB (SO + Docker Desktop + WSL2) |
| **Estabilidade 24/7** | ✅ Projetado para servidor | ⚠️ Updates forçados, reinícios |
| **Licenciamento** | ✅ Gratuito | ⚠️ Requer licença Windows |
| **Auto-start após reinício** | ✅ systemd nativo | ⚠️ Docker Desktop precisa login |
| **Gerenciamento remoto** | ✅ SSH (sem interface gráfica) | ⚠️ Precisa RDP ou login presencial |
| **Backup de volume Docker** | ✅ Acesso direto ao filesystem | ⚠️ Volumes dentro da VM WSL2 |
| **Firewall** | ✅ ufw, simples e confiável | ⚠️ Windows Firewall, mais complexo |
| **Facilidade de setup** | ⚠️ Requer linha de comando | ✅ Interface gráfica |

### Se optar por Linux

Qualquer distribuição com suporte ao Docker Engine funciona. Recomendações por ordem de preferência:

1. **Ubuntu Server 24.04 LTS** — Suporte até 2029, maior base de documentação
2. **Debian 12** — Estabilidade máxima, menos atualizações
3. **Rocky Linux 9** — Se a empresa já usa RHEL/CentOS

### Se optar por Windows

Funciona perfeitamente para uso interno. Considerações:

- Instalar **Docker Desktop** (gratuito para empresas com menos de 250 funcionários ou faturamento < US$10M)
- Ativar **WSL2** durante a instalação do Docker Desktop
- Configurar o Docker Desktop para **iniciar automaticamente** com o Windows
- Desabilitar **Windows Update** durante horário comercial para evitar reinícios

---

## 3. Requisitos de Hardware

### Cenário 1: Uso Leve (até 30 usuários, até 5.000 ativos)

> Para a maioria das empresas de médio porte. **Qualquer PC desktop moderno serve.**

| Item | Mínimo | Recomendado |
|---|---|---|
| **CPU** | 2 cores (x64) | 4 cores |
| **RAM** | 2 GB (Linux) / 4 GB (Windows) | 4 GB (Linux) / 8 GB (Windows) |
| **Disco** | 20 GB SSD | 50 GB SSD |
| **Rede** | Ethernet 100 Mbps | Gigabit |
| **SO** | Ubuntu Server 24.04 LTS | Ubuntu Server 24.04 LTS |

### Cenário 2: Uso Médio (30–100 usuários, até 50.000 ativos)

| Item | Recomendado |
|---|---|
| **CPU** | 4 cores (x64) |
| **RAM** | 8 GB |
| **Disco** | 100 GB SSD |
| **Rede** | Gigabit |
| **SO** | Ubuntu Server 24.04 LTS |

### Cenário 3: Uso Intensivo (100+ usuários, importações frequentes)

| Item | Recomendado |
|---|---|
| **CPU** | 8 cores (x64) |
| **RAM** | 16 GB |
| **Disco** | 200 GB SSD NVMe |
| **Rede** | Gigabit |
| **SO** | Ubuntu Server 24.04 LTS |

### Pode ser um PC comum?

**Sim.** Para o cenário 1 (maioria dos casos), um PC desktop ou notebook antigo com:
- Processador Intel i3/i5 (6ª geração em diante) ou AMD Ryzen
- 4 GB de RAM
- SSD de 120 GB

...é mais que suficiente. Não precisa ser um servidor dedicado.

> [!TIP]
> **Opção custo zero:** reutilize um PC desktop que seria aposentado. Instale Ubuntu Server (sem interface gráfica), Docker, e pronto.

---

## 4. Requisitos de Rede

| Item | Detalhe |
|---|---|
| **IP fixo na rede local** | A máquina precisa de IP fixo (ex: `192.168.1.50`) para que o endereço não mude |
| **Porta TCP 3000** | Aberta no firewall da máquina para acesso dos navegadores |
| **Acesso à internet** | Necessário apenas durante instalação e atualizações (para baixar imagens Docker). Após instalação, o sistema funciona 100% offline |
| **DNS (opcional)** | Se quiser acessar por nome (ex: `http://assetdock.empresa.local`) em vez de IP, configure no DNS interno |

### Acesso Externo (fora da empresa)

O sistema é projetado para rede interna. Para acesso externo (filiais, VPN, home office):

| Método | Complexidade | Segurança |
|---|---|---|
| **VPN corporativa** | Baixa | ✅ Alta — acesso como se estivesse na rede |
| **HTTPS com domínio público** | Média | ✅ Alta — usa `docker-compose.https.yml` com Caddy/Let's Encrypt |
| **Port forwarding no roteador** | Baixa | ❌ Inseguro — expõe HTTP sem criptografia |

---

## 5. Instalação Passo a Passo

### 5.1. Instalação em Linux (Ubuntu Server)

#### Passo 1 — Instalar Docker Engine

```bash
# Atualizar o sistema
sudo apt update && sudo apt upgrade -y

# Instalar Docker (script oficial)
curl -fsSL https://get.docker.com | sudo sh

# Adicionar seu usuário ao grupo docker (evita usar sudo sempre)
sudo usermod -aG docker $USER

# Sair e entrar novamente para aplicar
exit
# (reconectar via SSH)

# Verificar instalação
docker --version
docker compose version
```

#### Passo 2 — Criar diretório de instalação

```bash
sudo mkdir -p /opt/assetdock
cd /opt/assetdock
```

#### Passo 3 — Baixar os arquivos necessários

```bash
# Baixar os 3 arquivos do repositório
curl -LO https://raw.githubusercontent.com/nicokaka/assetdock-api/main/docker-compose.client.yml
curl -LO https://raw.githubusercontent.com/nicokaka/assetdock-api/main/.env.client.example
curl -LO https://raw.githubusercontent.com/nicokaka/assetdock-api/main/update.sh
chmod +x update.sh
```

#### Passo 4 — Configurar variáveis de ambiente

```bash
cp .env.client.example .env
```

Edite o `.env`:
```bash
nano .env
```

Preencha obrigatoriamente:
```env
# Gerar segredo (copie o resultado):
#   openssl rand -base64 48
JWT_SECRET=COLE_O_RESULTADO_AQUI

# Senha do banco interno
DB_PASSWORD=SuaSenhaForte!2026

# IP da máquina na rede
FRONTEND_URL=http://192.168.1.50:3000
```

#### Passo 5 — Iniciar o sistema

```bash
docker compose -f docker-compose.client.yml up -d
```

Primeira execução: ~2 minutos para baixar imagens. Depois: ~30 segundos.

#### Passo 6 — Verificar se está funcionando

```bash
# Verificar status dos containers
docker compose -f docker-compose.client.yml ps

# Deve mostrar 4 containers com status "healthy" ou "running"
```

#### Passo 7 — Configurar firewall

```bash
sudo ufw allow 3000/tcp comment "AssetDock Web"
sudo ufw enable
```

#### Passo 8 — Configurar auto-start

O Docker já reinicia os containers automaticamente (`restart: unless-stopped`). Basta garantir que o Docker inicie com o sistema:

```bash
sudo systemctl enable docker
```

---

### 5.2. Instalação em Windows

#### Passo 1 — Instalar Docker Desktop

1. Baixe de [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/)
2. Execute o instalador
3. Marque "Use WSL 2 instead of Hyper-V"
4. Reinicie o computador
5. Abra o Docker Desktop e aguarde inicializar

#### Passo 2 — Criar pasta de instalação

```powershell
New-Item -ItemType Directory -Path "C:\AssetDock" -Force
Set-Location "C:\AssetDock"
```

#### Passo 3 — Baixar os arquivos

Baixe manualmente do GitHub ou via PowerShell:
```powershell
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/nicokaka/assetdock-api/main/docker-compose.client.yml" -OutFile "docker-compose.client.yml"
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/nicokaka/assetdock-api/main/.env.client.example" -OutFile ".env"
Invoke-WebRequest -Uri "https://raw.githubusercontent.com/nicokaka/assetdock-api/main/update.sh" -OutFile "update.sh"
```

#### Passo 4 — Configurar o `.env`

Abra o `.env` no Bloco de Notas e preencha:

```powershell
# Para gerar o JWT_SECRET:
[Convert]::ToBase64String((1..48 | ForEach-Object { [byte](Get-Random -Max 256) }))
# Copie o resultado e cole no JWT_SECRET
```

#### Passo 5 — Iniciar o sistema

```powershell
docker compose -f docker-compose.client.yml up -d
```

#### Passo 6 — Liberar no Firewall do Windows

```
Painel de Controle
  → Sistema e Segurança
    → Firewall do Windows Defender
      → Configurações Avançadas
        → Regras de Entrada
          → Nova Regra
            → Porta → TCP → 3000 → Permitir conexão → Nome: "AssetDock"
```

Ou via PowerShell (Admin):
```powershell
New-NetFirewallRule -DisplayName "AssetDock Web" -Direction Inbound -Protocol TCP -LocalPort 3000 -Action Allow
```

---

## 6. Primeiro Acesso — Setup Wizard

Após a instalação, abra o navegador em qualquer computador da rede e acesse:

```
http://<IP-DA-MÁQUINA>:3000
```

O sistema apresentará o **Assistente de Configuração** (aparece apenas na primeira vez):

| Campo | O que preencher | Exemplo |
|---|---|---|
| Nome da Organização | Nome da empresa | `Minha Empresa Ltda` |
| Slug da Organização | Identificador curto (sem espaços) | `minha-empresa` |
| Nome do Administrador | Nome completo do admin principal | `João Silva` |
| E-mail do Administrador | E-mail corporativo para login | `joao@empresa.com` |
| Senha do Administrador | Mínimo 8 caracteres, com maiúscula, minúscula, número e caractere especial | `MinhaSenh@2026` |

Após configurar, você será redirecionado para a tela de login.

---

## 7. Operação Diária

### Comandos Essenciais

Todos os comandos devem ser executados na pasta de instalação (`/opt/assetdock` ou `C:\AssetDock`).

| Ação | Comando |
|---|---|
| **Ver status** | `docker compose -f docker-compose.client.yml ps` |
| **Iniciar** | `docker compose -f docker-compose.client.yml up -d` |
| **Parar** | `docker compose -f docker-compose.client.yml down` |
| **Ver logs** | `docker compose -f docker-compose.client.yml logs -f` |
| **Ver logs da API** | `docker compose -f docker-compose.client.yml logs -f api` |
| **Reiniciar API** | `docker compose -f docker-compose.client.yml restart api` |

### Atualização para Nova Versão

```bash
# Linux
cd /opt/assetdock
docker compose -f docker-compose.client.yml pull
docker compose -f docker-compose.client.yml up -d --remove-orphans

# Windows (PowerShell)
Set-Location "C:\AssetDock"
docker compose -f docker-compose.client.yml pull
docker compose -f docker-compose.client.yml up -d --remove-orphans
```

> [!NOTE]
> As atualizações **nunca apagam dados**. O banco de dados persiste em um volume Docker independente dos containers.

### Verificação de Saúde

Para confirmar que o sistema está operacional:

```bash
# Deve retornar {"status":"UP"}
curl http://localhost:8080/actuator/health

# Ou pelo container
docker exec assetdock-api wget -qO- http://localhost:8080/actuator/health
```

---

## 8. Backup e Restauração

### Backup Automático

O sistema executa backup automático do banco de dados:
- **Frequência:** Diariamente às 02:00 UTC
- **Retenção:** 7 dias (backups mais antigos são removidos)
- **Formato:** pg_dump comprimido (`.sql.gz`)
- **Armazenamento:** Volume Docker `assetdock-backups`

### Extrair Backup para Pasta Local

```bash
# Linux
docker run --rm \
  -v assetdock_assetdock-backups:/source \
  -v /tmp/assetdock-backups:/dest \
  alpine cp -r /source/. /dest/

ls -la /tmp/assetdock-backups/
```

```powershell
# Windows
docker run --rm `
  -v assetdock_assetdock-backups:/source `
  -v "${PWD}\backups:/dest" `
  alpine cp -r /source/. /dest/

Get-ChildItem .\backups\
```

### Backup Manual (sob demanda)

```bash
docker exec assetdock-db pg_dump -U assetdock -d assetdock | gzip > backup_$(date +%Y%m%d_%H%M%S).sql.gz
```

### Restaurar um Backup

> [!CAUTION]
> A restauração **substitui todos os dados atuais**. Faça backup antes.

```bash
# 1. Parar a API
docker compose -f docker-compose.client.yml stop api

# 2. Restaurar
gunzip -c backup_20260507.sql.gz | docker exec -i assetdock-db psql -U assetdock -d assetdock

# 3. Reiniciar
docker compose -f docker-compose.client.yml up -d
```

### Política de Backup Recomendada

| Nível | Frequência | Responsável | Destino |
|---|---|---|---|
| Automático (AssetDock) | Diário | Sistema | Volume Docker local |
| Cópia externa | Semanal | TI | Servidor de arquivo / NAS / pendrive |
| Backup full da máquina | Mensal | TI | Imagem do disco / snapshot |

---

## 9. Segurança

### O que já está implementado

| Proteção | Como funciona |
|---|---|
| **RBAC** | 4 níveis de acesso (Admin, Gestor, Auditor, Visualizador) |
| **Bloqueio por tentativas** | Conta bloqueada após 5 tentativas de login erradas |
| **Rate limiting** | Limitação de requisições por IP em endpoints sensíveis |
| **Logs de auditoria** | Toda ação registrada (quem, quando, o quê) — imutável |
| **Sessão com timeout** | Sessão inativa expira em 30 minutos, máximo absoluto 8 horas |
| **CSRF protection** | Token anti-falsificação em todas as requisições |
| **Sanitização de erros** | Mensagens de erro genéricas (sem stack traces em produção) |
| **Rede isolada** | Apenas a porta 3000 é exposta; banco e API ficam internos |
| **Lock pessimista** | Operações concorrentes no banco protegidas contra race conditions |

### Boas práticas operacionais

1. **JWT_SECRET:** nunca compartilhar, armazenar apenas no `.env`
2. **DB_PASSWORD:** trocar o valor padrão antes do primeiro uso
3. **Acesso SSH:** desabilitar login com senha, usar chave RSA
4. **Atualizações:** aplicar patches de segurança do SO regularmente
5. **Monitoramento:** ativar Uptime Kuma (opcional, via `docker-compose.monitoring.yml`)

---

## 10. Troubleshooting

### O sistema não inicia

```bash
# Ver logs detalhados
docker compose -f docker-compose.client.yml logs

# Verificar se o banco está saudável
docker compose -f docker-compose.client.yml logs postgres

# Se o banco não iniciar, verificar espaço em disco
df -h  # Linux
Get-PSDrive C  # Windows
```

### "Connection refused" ao acessar pelo navegador

1. Verificar se os containers estão rodando: `docker compose -f docker-compose.client.yml ps`
2. Verificar se a porta está aberta: `curl http://localhost:3000`
3. Verificar firewall: `sudo ufw status` (Linux) ou `netsh advfirewall show allprofiles` (Windows)
4. Verificar se o IP está correto: `ip addr` (Linux) ou `ipconfig` (Windows)

### API demora para iniciar na primeira vez

Normal. O Spring Boot leva ~30-60 segundos para inicializar. O container `web` aguarda automaticamente (via `depends_on` + `healthcheck`).

### Esqueci a senha do admin

Outro administrador pode resetar pelo menu **Users** > selecionar o usuário > **Reset Password**.

Se não houver outro admin, restaure de um backup que tenha a senha antiga.

### Disco cheio

```bash
# Ver uso de disco do Docker
docker system df

# Limpar imagens antigas (não remove dados)
docker system prune -f
```

---

## 11. Estimativa de Custos

### Hardware (opção mais barata)

| Item | Opção | Custo |
|---|---|---|
| Máquina | PC desktop reutilizado | R$ 0 |
| SSD 120 GB (se não tiver) | Kingston A400 | ~R$ 80 |
| Ubuntu Server | Gratuito | R$ 0 |
| Docker Engine | Gratuito | R$ 0 |
| **Total** | | **R$ 0 – R$ 80** |

### Hardware (opção dedicada)

| Item | Opção | Custo |
|---|---|---|
| Mini PC (Intel N100, 8GB, 256GB SSD) | Beelink, MinisForum | ~R$ 800–1.200 |
| Ubuntu Server | Gratuito | R$ 0 |
| **Total** | | **R$ 800 – R$ 1.200** |

### Licenciamento de Software

| Software | Custo |
|---|---|
| AssetDock (API + Web) | Desenvolvimento interno — R$ 0 |
| Ubuntu Server 24.04 LTS | Gratuito |
| Docker Engine | Gratuito (Linux) |
| PostgreSQL 17 | Gratuito |
| Nginx | Gratuito |
| **Total de licenças** | **R$ 0** |

> [!NOTE]
> Se optar por Windows: Docker Desktop é gratuito para empresas com menos de 250 funcionários **e** faturamento anual inferior a US$ 10 milhões. Caso contrário, a licença do Docker Desktop Business custa US$ 24/mês por máquina.

---

## 12. Checklist Pré-Deploy

Use esta checklist antes de colocar o sistema em produção:

### Infraestrutura
- [ ] Máquina host definida (IP fixo na rede)
- [ ] Docker instalado e funcionando
- [ ] Porta 3000 liberada no firewall
- [ ] IP acessível de outros computadores da rede

### Configuração
- [ ] Arquivo `.env` criado a partir do `.env.client.example`
- [ ] `JWT_SECRET` gerado com valor aleatório forte (≥ 32 caracteres)
- [ ] `DB_PASSWORD` definida com senha forte
- [ ] `FRONTEND_URL` configurada com IP real da máquina (não localhost)

### Validação
- [ ] `docker compose -f docker-compose.client.yml up -d` executado com sucesso
- [ ] Todos os 4 containers rodando (`docker compose ps`)
- [ ] Setup Wizard acessível via navegador
- [ ] Organização e admin criados com sucesso
- [ ] Login funcionando
- [ ] Acesso de outro computador da rede confirmado

### Segurança
- [ ] Valor padrão de `DB_PASSWORD` alterado
- [ ] `JWT_SECRET` não é o valor de exemplo
- [ ] Arquivo `.env` com permissões restritas (`chmod 600 .env` em Linux)

### Backup
- [ ] Container `assetdock-backup` rodando
- [ ] Teste de extração de backup realizado
- [ ] Rotina de cópia externa definida (semanal)

---

## 13. Contatos e Suporte

| Assunto | Responsável | Contato |
|---|---|---|
| Problemas no sistema | Equipe de Desenvolvimento | (definir) |
| Problemas de rede/infra | Equipe de TI | (definir) |
| Solicitação de funcionalidades | Gestão | (definir) |

---

## Anexo A — Resumo para Compra (se aplicável)

Se for necessário justificar a aquisição de hardware:

> **Justificativa:** Implantação do sistema AssetDock para controle patrimonial de ativos de TI.
>
> **Requisitos mínimos:** Computador x64 com 4 GB RAM, 120 GB SSD, conectividade Ethernet.
>
> **Opção recomendada:** Mini PC com Intel N100 ou superior, 8 GB RAM, 256 GB SSD (~R$ 1.000).
>
> **Custo de software:** Zero (todas as tecnologias são open source).
>
> **Benefício:** Rastreabilidade completa de ativos, eliminação de planilhas manuais, auditoria automática, acesso multi-usuário com controle de permissões.

---

*AssetDock v1.1.0 — Sistema de Gestão de Patrimônio Self-Hosted*
