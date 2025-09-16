#!/bin/bash

# CScore Jobe Server Management Script
# Usage: ./jobe-manager.sh [command] [options]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.jobe.yml"
JOBE_URL="http://localhost:4000"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is available
check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose is not installed or not in PATH"
        exit 1
    fi
}

# Start Jobe server
start_jobe() {
    log_info "Starting Jobe server..."
    
    if [ "$1" = "multi" ]; then
        log_info "Starting multiple Jobe instances with load balancer..."
        docker-compose -f "$COMPOSE_FILE" --profile multi-instance up -d
    else
        docker-compose -f "$COMPOSE_FILE" up -d jobe-server
    fi
    
    log_success "Jobe server started successfully"
    wait_for_jobe
}

# Stop Jobe server
stop_jobe() {
    log_info "Stopping Jobe server..."
    docker-compose -f "$COMPOSE_FILE" --profile multi-instance down
    log_success "Jobe server stopped"
}

# Restart Jobe server
restart_jobe() {
    log_info "Restarting Jobe server..."
    stop_jobe
    sleep 2
    start_jobe "$1"
}

# Check Jobe server status
status_jobe() {
    log_info "Checking Jobe server status..."
    
    # Check Docker containers
    echo -e "\n${BLUE}Container Status:${NC}"
    docker-compose -f "$COMPOSE_FILE" ps
    
    # Check Jobe API
    echo -e "\n${BLUE}API Status:${NC}"
    if curl -s "$JOBE_URL/jobe/index.php/restapi/languages" > /dev/null; then
        log_success "Jobe API is responsive"
        
        # Get supported languages
        echo -e "\n${BLUE}Supported Languages:${NC}"
        curl -s "$JOBE_URL/jobe/index.php/restapi/languages" | jq -r '.[] | @tsv' 2>/dev/null || curl -s "$JOBE_URL/jobe/index.php/restapi/languages"
    else
        log_error "Jobe API is not responsive"
    fi
}

# Wait for Jobe server to be ready
wait_for_jobe() {
    log_info "Waiting for Jobe server to be ready..."
    
    local max_attempts=30
    local attempt=0
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -s "$JOBE_URL/jobe/index.php/restapi/languages" > /dev/null; then
            log_success "Jobe server is ready!"
            return 0
        fi
        
        attempt=$((attempt + 1))
        echo -n "."
        sleep 2
    done
    
    log_error "Jobe server failed to start within 60 seconds"
    return 1
}

# Test Jobe server with sample code
test_jobe() {
    log_info "Testing Jobe server with sample code..."
    
    # Test Java
    echo -e "\n${BLUE}Testing Java:${NC}"
    local java_response=$(curl -s -X POST "$JOBE_URL/jobe/index.php/restapi/runs" \
        -H "Content-Type: application/json" \
        -d '{
            "language_id": "java",
            "sourcecode": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello from Java!\");\n    }\n}"
        }')
    
    echo "$java_response" | jq . 2>/dev/null || echo "$java_response"
    
    # Test Python
    echo -e "\n${BLUE}Testing Python:${NC}"
    local python_response=$(curl -s -X POST "$JOBE_URL/jobe/index.php/restapi/runs" \
        -H "Content-Type: application/json" \
        -d '{
            "language_id": "python3",
            "sourcecode": "print(\"Hello from Python!\")"
        }')
    
    echo "$python_response" | jq . 2>/dev/null || echo "$python_response"
    
    # Test C++
    echo -e "\n${BLUE}Testing C++:${NC}"
    local cpp_response=$(curl -s -X POST "$JOBE_URL/jobe/index.php/restapi/runs" \
        -H "Content-Type: application/json" \
        -d '{
            "language_id": "cpp",
            "sourcecode": "#include <iostream>\nusing namespace std;\nint main() {\n    cout << \"Hello from C++!\" << endl;\n    return 0;\n}"
        }')
    
    echo "$cpp_response" | jq . 2>/dev/null || echo "$cpp_response"
}

# View Jobe server logs
logs_jobe() {
    log_info "Showing Jobe server logs..."
    docker-compose -f "$COMPOSE_FILE" logs -f jobe-server
}

# Pull latest Jobe image
update_jobe() {
    log_info "Pulling latest Jobe image..."
    docker-compose -f "$COMPOSE_FILE" pull
    log_success "Jobe image updated"
}

# Clean up Jobe resources
cleanup_jobe() {
    log_info "Cleaning up Jobe resources..."
    docker-compose -f "$COMPOSE_FILE" --profile multi-instance down -v --remove-orphans
    docker system prune -f
    log_success "Cleanup completed"
}

# Show usage
usage() {
    echo "CScore Jobe Server Management Script"
    echo ""
    echo "Usage: $0 [command] [options]"
    echo ""
    echo "Commands:"
    echo "  start [multi]     Start Jobe server (add 'multi' for multiple instances)"
    echo "  stop              Stop Jobe server"
    echo "  restart [multi]   Restart Jobe server"
    echo "  status            Check Jobe server status"
    echo "  test              Test Jobe server with sample code"
    echo "  logs              View Jobe server logs"
    echo "  update            Pull latest Jobe image"
    echo "  cleanup           Clean up Jobe resources"
    echo "  help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 start          # Start single Jobe instance"
    echo "  $0 start multi    # Start multiple instances with load balancer"
    echo "  $0 test           # Test Jobe server functionality"
    echo ""
}

# Main script logic
main() {
    check_docker
    
    case "$1" in
        start)
            start_jobe "$2"
            ;;
        stop)
            stop_jobe
            ;;
        restart)
            restart_jobe "$2"
            ;;
        status)
            status_jobe
            ;;
        test)
            test_jobe
            ;;
        logs)
            logs_jobe
            ;;
        update)
            update_jobe
            ;;
        cleanup)
            cleanup_jobe
            ;;
        help|--help|-h)
            usage
            ;;
        "")
            log_error "No command specified"
            usage
            exit 1
            ;;
        *)
            log_error "Unknown command: $1"
            usage
            exit 1
            ;;
    esac
}

# Run main function with all arguments
main "$@"
