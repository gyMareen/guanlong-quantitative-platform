#!/bin/bash

# Longport Platform - 初始化脚本
# 用于安装依赖并启动开发服务器

set -e

echo "=========================================="
echo "  Longport Platform - 初始化脚本"
echo "=========================================="

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        print_error "$1 未安装，请先安装 $1"
        exit 1
    fi
}

# 检查环境
print_info "检查运行环境..."

# 检查 Java
check_command java
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    print_error "Java 版本需要 17 或更高，当前版本: $JAVA_VERSION"
    exit 1
fi
print_info "Java 版本: $(java -version 2>&1 | head -n 1)"

# 检查 Maven
check_command mvn
print_info "Maven 版本: $(mvn -version | head -n 1)"

# 检查 Node.js
check_command node
NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 18 ]; then
    print_error "Node.js 版本需要 18 或更高，当前版本: $(node -v)"
    exit 1
fi
print_info "Node.js 版本: $(node -v)"

# 检查 npm
check_command npm
print_info "npm 版本: $(npm -v)"

echo ""
print_info "环境检查通过！"
echo ""

# 安装后端依赖
print_info "=========================================="
print_info "安装后端依赖..."
print_info "=========================================="
cd backend
mvn clean install -DskipTests
cd ..
print_info "后端依赖安装完成！"
echo ""

# 安装前端依赖
print_info "=========================================="
print_info "安装前端依赖..."
print_info "=========================================="
cd frontend
npm install
cd ..
print_info "前端依赖安装完成！"
echo ""

# 检查数据库
print_warn "=========================================="
print_warn "请确保已执行以下操作："
print_warn "1. PostgreSQL 已启动"
print_warn "2. 已创建 longport 数据库"
print_warn "3. 已执行 backend/src/main/resources/db/init.sql"
print_warn "4. Redis 已启动"
print_warn "=========================================="
echo ""

read -p "是否现在启动开发服务器？(y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_info "启动后端服务..."
    cd backend
    mvn spring-boot:run &
    BACKEND_PID=$!
    cd ..

    print_info "等待后端启动..."
    sleep 10

    print_info "启动前端服务..."
    cd frontend
    npm run dev &
    FRONTEND_PID=$!
    cd ..

    echo ""
    print_info "=========================================="
    print_info "服务已启动："
    print_info "  前端: http://localhost:5173"
    print_info "  后端: http://localhost:8080"
    print_info "  API文档: http://localhost:8080/doc.html"
    print_info "=========================================="
    print_info "按 Ctrl+C 停止所有服务"

    # 等待用户中断
    trap "kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" INT TERM
    wait
fi

print_info "初始化完成！"
