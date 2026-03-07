#!/bin/bash

# 官龙量化一体化交易平台 - 启动脚本

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/local.yml"
ENV_FILE="${SCRIPT_DIR}/.env"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
print_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_step() { echo -e "${BLUE}[STEP]${NC} $1"; }

show_usage() {
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  start       启动所有服务"
    echo "  stop        停止所有服务"
    echo "  restart     重启所有服务"
    echo "  status      查看服务状态"
    echo "  logs        查看日志"
    echo "  clean       清理所有数据（危险操作）"
    echo "  init        初始化 Kafka Topics"
    echo ""
    echo "示例:"
    echo "  $0 start"
    echo "  $0 logs kafka"
}

check_prerequisites() {
    print_step "检查前置条件..."

    if ! command -v docker &> /dev/null; then
        print_error "Docker 未安装"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose 未安装"
        exit 1
    fi

    print_info "前置条件检查通过"
}

start_services() {
    print_step "启动服务..."

    if [ -f "$ENV_FILE" ]; then
        docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
    else
        docker compose -f "$COMPOSE_FILE" up -d
    fi

    print_info "服务启动完成"
    show_status
}

stop_services() {
    print_step "停止服务..."
    docker compose -f "$COMPOSE_FILE" down
    print_info "服务已停止"
}

restart_services() {
    stop_services
    start_services
}

show_status() {
    print_step "服务状态:"
    docker compose -f "$COMPOSE_FILE" ps
}

show_logs() {
    local service=$1
    if [ -z "$service" ]; then
        docker compose -f "$COMPOSE_FILE" logs -f
    else
        docker compose -f "$COMPOSE_FILE" logs -f "$service"
    fi
}

clean_data() {
    print_warn "此操作将删除所有数据！"
    read -p "确认继续？(yes/no) " -r
    echo
    if [[ $REPLY == "yes" ]]; then
        print_step "清理数据..."
        docker compose -f "$COMPOSE_FILE" down -v --remove-orphans
        print_info "数据已清理"
    else
        print_info "操作已取消"
    fi
}

init_kafka() {
    print_step "初始化 Kafka Topics..."

    TOPICS=(
        "futu.rebalance.raw"
        "futu.rebalance.qs"
        "futu.trade.events"
    )

    for topic in "${TOPICS[@]}"; do
        print_info "创建 Topic: $topic"
        docker exec guanlong-kafka kafka-topics --create \
            --topic "$topic" \
            --bootstrap-server localhost:9092 \
            --partitions 3 \
            --replication-factor 1 \
            --if-not-exists || print_warn "Topic $topic 已存在"
    done

    print_step "验证 Topics:"
    docker exec guanlong-kafka kafka-topics --list --bootstrap-server localhost:9092

    print_info "Kafka Topics 初始化完成"
}

# 主程序
case "${1:-}" in
    start)
        check_prerequisites
        start_services
        ;;
    stop)
        stop_services
        ;;
    restart)
        check_prerequisites
        restart_services
        ;;
    status)
        show_status
        ;;
    logs)
        show_logs "$2"
        ;;
    clean)
        clean_data
        ;;
    init)
        init_kafka
        ;;
    *)
        show_usage
        exit 1
        ;;
esac
