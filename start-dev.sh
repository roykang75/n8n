#!/bin/bash
# =============================================================================
# n8n Development Server Startup Script (macOS/Linux)
# =============================================================================
# This script starts both the Spring Boot backend and Vite frontend dev server.
# 
# Prerequisites:
#   - Node.js >= 22.x, pnpm installed globally
#   - Java 21+ (Azul Zulu or similar)
#   - MySQL running on localhost:3306 (or use docker-compose)
#
# Usage:
#   ./start-dev.sh              # Start both servers (foreground)
#   ./start-dev.sh --background # Start both servers (background)
#   ./start-dev.sh --stop       # Stop all servers
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# Configuration
BACKEND_PORT=5678
FRONTEND_PORT=8080
BACKEND_DIR="$PROJECT_ROOT/springboot"
FRONTEND_DIR="$PROJECT_ROOT/packages/frontend/editor-ui"

print_banner() {
    echo -e "${BLUE}"
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║           n8n Development Server (Spring Boot)                ║"
    echo "║              Backend: $BACKEND_PORT  |  Frontend: $FRONTEND_PORT                    ║"
    echo "╚═══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

check_port() {
    lsof -Pi :$1 -sTCP:LISTEN -t >/dev/null 2>&1
}

kill_port() {
    local pids=$(lsof -ti:$1 2>/dev/null)
    if [ -n "$pids" ]; then
        echo -e "${YELLOW}Stopping process on port $1...${NC}"
        echo "$pids" | xargs kill -9 2>/dev/null || true
        sleep 1
    fi
}

stop_all() {
    echo -e "${YELLOW}Stopping all development servers...${NC}"
    kill_port $BACKEND_PORT
    kill_port $FRONTEND_PORT
    pkill -f "gradlew bootRun" 2>/dev/null || true
    pkill -f "vite" 2>/dev/null || true
    echo -e "${GREEN}All servers stopped.${NC}"
}

start_docker_if_needed() {
    if [ -f "$PROJECT_ROOT/docker-compose-dev.yml" ]; then
        echo -e "${BLUE}Starting Docker services (MySQL, Redis)...${NC}"
        docker-compose -f "$PROJECT_ROOT/docker-compose-dev.yml" up -d 2>/dev/null || true
        sleep 3
    fi
}

start_backend() {
    echo -e "${GREEN}Starting Spring Boot backend...${NC}"
    if check_port $BACKEND_PORT; then
        kill_port $BACKEND_PORT
    fi
    cd "$BACKEND_DIR"
    ./gradlew bootRun &
    BACKEND_PID=$!
    echo $BACKEND_PID > "$PROJECT_ROOT/.backend.pid"
}

start_frontend() {
    echo -e "${GREEN}Starting Vite frontend dev server...${NC}"
    if check_port $FRONTEND_PORT; then
        kill_port $FRONTEND_PORT
    fi
    cd "$FRONTEND_DIR"
    pnpm dev &
    FRONTEND_PID=$!
    echo $FRONTEND_PID > "$PROJECT_ROOT/.frontend.pid"
}

wait_for_ready() {
    echo -ne "${YELLOW}Waiting for servers..."
    for i in {1..30}; do
        if check_port $BACKEND_PORT && check_port $FRONTEND_PORT; then
            echo -e " ${GREEN}Ready!${NC}"
            return 0
        fi
        echo -n "."
        sleep 1
    done
    echo -e " ${RED}Timeout${NC}"
}

print_status() {
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${GREEN}  ✅ Development servers are running!${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════════════════════════${NC}"
    echo ""
    echo -e "  ${BLUE}🌐 Frontend:${NC} http://localhost:$FRONTEND_PORT"
    echo -e "  ${BLUE}🔧 Backend:${NC}  http://localhost:$BACKEND_PORT"
    echo -e "  ${BLUE}📊 Actuator:${NC} http://localhost:$BACKEND_PORT/actuator/health"
    echo ""
    echo -e "  ${YELLOW}Press Ctrl+C to stop all servers${NC}"
    echo ""
}

# Cleanup on exit
cleanup() {
    echo ""
    echo -e "${YELLOW}🛑 Stopping all services...${NC}"
    stop_all
    exit 0
}

# Main
print_banner

case "${1:-}" in
    --stop|-s)
        stop_all
        exit 0
        ;;
    --background|-b)
        start_docker_if_needed
        start_backend
        start_frontend
        wait_for_ready
        print_status
        echo -e "  ${YELLOW}Running in background. Use './start-dev.sh --stop' to stop.${NC}"
        exit 0
        ;;
    *)
        trap cleanup INT TERM
        start_docker_if_needed
        start_backend
        start_frontend
        wait_for_ready
        print_status
        wait
        ;;
esac