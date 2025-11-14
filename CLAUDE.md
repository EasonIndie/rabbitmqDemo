# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a comprehensive RabbitMQ demonstration project that showcases enterprise-level message queue capabilities with Spring Boot. The project includes practical implementations of advanced RabbitMQ features including priority queues, dead letter queues, delayed messaging, retry mechanisms, and transactional messages.

## Development Commands

### Build and Run
```bash
# Compile the project
mvn clean compile

# Run tests
mvn test

# Package the application
mvn clean package

# Run the application locally
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker Operations
```bash
# Build Docker image
docker build -t rabbitmq-demo .

# Run with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f rabbitmq-demo
```

### API Testing
```bash
# Test basic order creation
curl -X POST http://localhost:8080/api/orders/create \
  -H "Content-Type: application/json" \
  -d '{"userId":"USER001","items":[{"productId":"PROD001","productName":"Test Product","price":99.99,"quantity":1}],"priority":5}'

# Test priority queue
curl -X POST http://localhost:8080/api/orders/priority/create \
  -H "Content-Type: application/json" \
  -d '{"userId":"VIP001","items":[{"productId":"VIP001","productName":"VIP Product","price":999.99,"quantity":1}],"priority":10}'

# Test batch creation
curl -X POST http://localhost:8080/api/orders/create-batch?count=20

# Test all scenarios
curl -X POST http://localhost:8080/api/orders/test-scenarios
```

## Architecture Overview

### Core Components

1. **Configuration Layer** (`config/`)
   - `RabbitMQConfig.java`: Centralized RabbitMQ configuration including queues, exchanges, and bindings
   - Defines multiple queue types: standard, priority, delayed, dead-letter, and retry queues

2. **Message Producers** (`producer/`)
   - `OrderProducer.java`: Handles message publishing with various features like priority setting and delays
   - Implements publisher confirms and returns callbacks

3. **Message Consumers** (`consumer/`)
   - `OrderConsumer.java`: Processes orders with manual acknowledgment and error handling
   - `PriorityDemoConsumer.java`: Demonstrates priority queue processing
   - `AnnotationRetryListener.java`: Shows Spring Retry annotation usage

4. **REST Controllers** (`controller/`)
   - `OrderController.java`: Provides REST endpoints for order management
   - `PriorityDemoController.java`: Specialized endpoints for priority queue testing

5. **Data Models** (`model/`)
   - `Order.java`: Main order entity with validation and priority support
   - `OrderItem.java`: Order item entity

### Key Features Demonstrated

1. **Priority Queues**: Messages with higher priority are processed first
2. **Dead Letter Queues**: Failed messages are routed to DLQ for analysis
3. **Delayed Messaging**: Messages are delayed before processing
4. **Retry Mechanisms**: Both manual and annotation-based retry strategies
5. **Transactional Messages**: Ensuring message consistency
6. **Message Acknowledgments**: Manual and automatic acknowledgment modes

## Configuration Details

### RabbitMQ Configuration
- **Connection**: Custom RabbitMQ server connection (configured in application.yml)
- **Queues**: Multiple queue types with different purposes
- **Exchanges**: Direct and Fanout exchanges for routing
- **Message Converter**: Jackson-based JSON converter with Java 8 time support

### Application Profiles
- **dev**: Development environment with debug logging
- **prod**: Production environment settings
- **docker**: Docker-specific configurations

## Monitoring and Management

### Health Checks
- Application health: http://localhost:8080/actuator/health
- RabbitMQ health: Integrated into application health checks

### Metrics
- Prometheus metrics: http://localhost:8080/actuator/prometheus
- Custom metrics for queue depths and processing rates

### Management Interfaces
- RabbitMQ Management UI: Available on RabbitMQ server port 15672
- H2 Database Console: http://localhost:8080/h2-console

## Development Guidelines

### Adding New Queues
1. Define queue name in `application.yml` under `rabbitmq.queue.*`
2. Create Queue bean in `RabbitMQConfig.java`
3. Create appropriate Exchange and Binding beans
4. Add consumer with `@RabbitListener` annotation
5. Add producer methods in appropriate Producer class

### Error Handling
- Use dead letter queues for failed messages
- Implement proper retry logic with exponential backoff
- Monitor queue depths to detect processing issues
- Use proper exception handling in consumers

### Performance Considerations
- Configure appropriate prefetch counts based on message processing time
- Use manual acknowledgment for better control
- Monitor connection and channel usage
- Consider consumer concurrency for high-throughput scenarios

## Testing

### Unit Tests
- Test message production and consumption logic
- Verify error handling and retry mechanisms
- Test configuration beans and bindings

### Integration Tests
- Test end-to-end message flows
- Verify queue configurations with test RabbitMQ instance
- Test REST endpoints with actual message processing

### Load Testing
- Use the batch creation endpoint for load testing
- Monitor queue depths and processing rates
- Test system behavior under high message volumes

## Troubleshooting

### Common Issues
1. **Connection Failures**: Check RabbitMQ server status and network connectivity
2. **Message Not Routed**: Verify exchange and routing key configurations
3. **Consumer Not Processing**: Check queue bindings and consumer health
4. **Memory Issues**: Monitor queue depths and implement proper message TTL

### Debug Tools
- RabbitMQ Management UI for queue monitoring
- Application logs with DEBUG level for AMQP operations
- Actuator endpoints for health and metrics

## Dependencies

### Core Dependencies
- Spring Boot Starter AMQP
- Spring Boot Starter Web
- Jackson for JSON processing
- H2 Database for testing

### Development Dependencies
- Spring Boot Starter Test
- Spring AMQP Test
- Lombok for reduced boilerplate

## Security Notes

- The current configuration uses default RabbitMQ credentials
- In production, ensure proper authentication and authorization
- Consider using SSL/TLS for secure communication
- Validate all incoming message data to prevent injection attacks