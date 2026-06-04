param(
    [string]$OutputDirectory = "secrets",
    [switch]$Force
)

$ErrorActionPreference = "Stop"

if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
    throw "Node.js is required to generate JWT keys with this script."
}

$resolvedOutput = Join-Path (Get-Location) $OutputDirectory
$privateKeyPath = Join-Path $resolvedOutput "privateKey.pem"
$publicKeyPath = Join-Path $resolvedOutput "publicKey.pem"

if ((Test-Path $privateKeyPath -or Test-Path $publicKeyPath) -and -not $Force) {
    Write-Host "JWT key files already exist in $resolvedOutput."
    Write-Host "Use -Force to replace them."
    exit 0
}

New-Item -ItemType Directory -Force -Path $resolvedOutput | Out-Null

$generator = @"
const fs = require('fs');
const crypto = require('crypto');
const outputDirectory = process.argv[2];
const { publicKey, privateKey } = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
fs.writeFileSync(`${outputDirectory}/privateKey.pem`, privateKey.export({ type: 'pkcs8', format: 'pem' }));
fs.writeFileSync(`${outputDirectory}/publicKey.pem`, publicKey.export({ type: 'spki', format: 'pem' }));
"@

node -e $generator $resolvedOutput

Write-Host "Generated JWT development keys:"
Write-Host "  $privateKeyPath"
Write-Host "  $publicKeyPath"
Write-Host "These files are ignored by Git and should not be committed."
