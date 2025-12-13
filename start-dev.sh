#!/bin/bash

echo "🚀 Starting n8n Development Environment..."

# Start MySQL and Redis
echo "📦 Starting MySQL and Redis..."
docker-compose -f docker-compose-dev.yml up -d

# Wait for MySQL to be ready
echo "⏳ Waiting for MySQL to be ready..."
sleep 10

# Reset database
echo "🗄️ Resetting database..."
docker exec n8n-mysql-dev mysql -uroot -proot -e "DROP DATABASE IF EXISTS n8n_dev; CREATE DATABASE n8n_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# Start Spring Boot backend
echo "🌱 Starting Spring Boot backend..."
cd springboot
./gradlew bootRun &
BACKEND_PID=$!

# Wait for backend to start
echo "⏳ Waiting for backend to start..."
sleep 20

# Check if backend is running
if curl -s http://localhost:8080/api/actuator/health > /dev/null; then
    echo "✅ Backend is running!"
else
    echo "❌ Backend failed to start"
    kill $BACKEND_PID 2>/dev/null
    exit 1
fi

# Start frontend
echo "🎨 Starting frontend..."
cd ..
pnpm dev:frontend &
FRONTEND_PID=$!

echo "✅ Development environment is ready!"
echo "Frontend: http://localhost:5678"
echo "Backend:  http://localhost:8080/api"
echo "Backend Health: http://localhost:8080/api/actuator/health"
echo ""
echo "Press Ctrl+C to stop all services"

# Wait for interrupt
trap "echo '🛑 Stopping all services...'; kill $BACKEND_PID 2>/dev/null; kill $FRONTEND_PID 2>/dev/null; docker-compose -f docker-compose-dev.yml down; exit" INT
wait