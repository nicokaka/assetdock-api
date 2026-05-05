$ErrorActionPreference = "Stop"

# Verifica se o arquivo .env existe. Se sim, carrega as variáveis localmente para a validação.
if (Test-Path ".env") {
    Get-Content ".env" | Where-Object { $_ -match '^\w+=' } | ForEach-Object {
        $name, $value = $_.Split('=', 2)
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
} else {
    Write-Host "AVISO: Arquivo .env nao encontrado na raiz do projeto."
}

$required = @("DB_URL", "DB_USERNAME", "DB_PASSWORD", "JWT_SECRET", "LOCAL_FRONTEND_ORIGINS")
$missing = @()

foreach ($var in $required) {
    if (-not [Environment]::GetEnvironmentVariable($var)) {
        $missing += $var
    }
}

if ($missing.Count -gt 0) {
    Write-Error "ERRO: Variaveis de ambiente obrigatorias nao definidas: $($missing -join ', ')"
    Write-Host "Copie .env.example para .env e preencha os valores antes de executar a aplicacao."
    exit 1
}

Write-Host "Ambiente validado com sucesso. Iniciando aplicacao..."
