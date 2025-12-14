# =============================================================================
# n8n Development Server Startup Script (Windows 11)
# =============================================================================
# This script starts both the Spring Boot backend and Vite frontend dev server.
# 
# Prerequisites:
#   - Node.js >= 22.x, pnpm installed globally
#   - Java 21+ (Azul Zulu or similar)
#   - MySQL running on localhost:3306 (or use Docker Desktop)
#
# Usage (Run in PowerShell):
#   .\start-dev.ps1              # Start both servers (foreground)
#   .\start-dev.ps1 -Background  # Start both servers (background)
#   .\start-dev.ps1 -Stop        # Stop all servers
# =============================================================================

param(
    [switch]$Stop,
    [switch]$Background
)

$ErrorActionPreference = "Stop"

# Configuration
$BACKEND_PORT = 5678
$FRONTEND_PORT = 8080
$PROJECT_ROOT = Split-Path -Parent $MyInvocation.MyCommand.Path
$BACKEND_DIR = Join-Path $PROJECT_ROOT "springboot"
$FRONTEND_DIR = Join-Path $PROJECT_ROOT "packages\frontend\editor-ui"

function Write-Banner {
    Write-Host ""
    Write-Host "╔═══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║           n8n Development Server (Spring Boot)                ║" -ForegroundColor Cyan
    Write-Host "║              Backend: $BACKEND_PORT  |  Frontend: $FRONTEND_PORT                    ║" -ForegroundColor Cyan
    Write-Host "╚═══════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
}

function Test-Port {
    param([int]$Port)
    try {
        $connection = New-Object System.Net.Sockets.TcpClient
        $connection.Connect("localhost", $Port)
        $connection.Close()
        return $true
    } catch {
        return $false
    }
}

function Stop-PortProcess {
    param([int]$Port)
    $process = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | 
               Select-Object -ExpandProperty OwningProcess -Unique
    if ($process) {
        Write-Host "Stopping process on port $Port..." -ForegroundColor Yellow
        Stop-Process -Id $process -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 1
    }
}

function Stop-AllServers {
    Write-Host "Stopping all development servers..." -ForegroundColor Yellow
    
    Stop-PortProcess -Port $BACKEND_PORT
    Stop-PortProcess -Port $FRONTEND_PORT
    
    # Kill Gradle and Vite processes
    Get-Process -Name "java" -ErrorAction SilentlyContinue | 
        Where-Object { $_.CommandLine -like "*gradlew*" } | 
        Stop-Process -Force -ErrorAction SilentlyContinue
    
    Get-Process -Name "node" -ErrorAction SilentlyContinue | 
        Where-Object { $_.CommandLine -like "*vite*" } | 
        Stop-Process -Force -ErrorAction SilentlyContinue
    
    Write-Host "All servers stopped." -ForegroundColor Green
}

function Start-DockerIfNeeded {
    $dockerCompose = Join-Path $PROJECT_ROOT "docker-compose-dev.yml"
    if (Test-Path $dockerCompose) {
        Write-Host "Starting Docker services (MySQL, Redis)..." -ForegroundColor Cyan
        try {
            docker-compose -f $dockerCompose up -d 2>$null
            Start-Sleep -Seconds 3
        } catch {
            Write-Host "Docker not available, skipping..." -ForegroundColor Yellow
        }
    }
}

function Start-Backend {
    Write-Host "Starting Spring Boot backend..." -ForegroundColor Green
    
    if (Test-Port -Port $BACKEND_PORT) {
        Stop-PortProcess -Port $BACKEND_PORT
    }
    
    Push-Location $BACKEND_DIR
    $job = Start-Job -ScriptBlock {
        Set-Location $using:BACKEND_DIR
        & .\gradlew.bat bootRun
    }
    Pop-Location
    
    $job.Id | Out-File (Join-Path $PROJECT_ROOT ".backend.job")
    return $job
}

function Start-Frontend {
    Write-Host "Starting Vite frontend dev server..." -ForegroundColor Green
    
    if (Test-Port -Port $FRONTEND_PORT) {
        Stop-PortProcess -Port $FRONTEND_PORT
    }
    
    Push-Location $FRONTEND_DIR
    $job = Start-Job -ScriptBlock {
        Set-Location $using:FRONTEND_DIR
        pnpm dev
    }
    Pop-Location
    
    $job.Id | Out-File (Join-Path $PROJECT_ROOT ".frontend.job")
    return $job
}

function Wait-ForReady {
    Write-Host "Waiting for servers..." -ForegroundColor Yellow -NoNewline
    
    for ($i = 0; $i -lt 30; $i++) {
        if ((Test-Port -Port $BACKEND_PORT) -and (Test-Port -Port $FRONTEND_PORT)) {
            Write-Host " Ready!" -ForegroundColor Green
            return $true
        }
        Write-Host "." -NoNewline
        Start-Sleep -Seconds 1
    }
    
    Write-Host " Timeout" -ForegroundColor Red
    return $false
}

function Write-Status {
    Write-Host ""
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Green
    Write-Host "  ✅ Development servers are running!" -ForegroundColor Green
    Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Green
    Write-Host ""
    Write-Host "  🌐 Frontend: " -ForegroundColor Cyan -NoNewline
    Write-Host "http://localhost:$FRONTEND_PORT"
    Write-Host "  🔧 Backend:  " -ForegroundColor Cyan -NoNewline
    Write-Host "http://localhost:$BACKEND_PORT"
    Write-Host "  📊 Actuator: " -ForegroundColor Cyan -NoNewline
    Write-Host "http://localhost:$BACKEND_PORT/actuator/health"
    Write-Host ""
}

# Main
Write-Banner

if ($Stop) {
    Stop-AllServers
    exit 0
}

Start-DockerIfNeeded

$backendJob = Start-Backend
$frontendJob = Start-Frontend

Wait-ForReady
Write-Status

if ($Background) {
    Write-Host "  Running in background. Use '.\start-dev.ps1 -Stop' to stop." -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host "  Press Ctrl+C to stop all servers" -ForegroundColor Yellow
    Write-Host ""
    
    try {
        # Keep script running and show job output
        while ($true) {
            Receive-Job -Job $backendJob -ErrorAction SilentlyContinue
            Receive-Job -Job $frontendJob -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 1
        }
    } finally {
        Write-Host ""
        Write-Host "Stopping all services..." -ForegroundColor Yellow
        Stop-AllServers
        Remove-Job -Job $backendJob -Force -ErrorAction SilentlyContinue
        Remove-Job -Job $frontendJob -Force -ErrorAction SilentlyContinue
    }
}
