# TradeForge 

A **microservices-based stock trading platform** built with Java and Spring Boot.

TradeForge is an educational backend project focused on building a stock trading system using independently developed services.

## 🛠 Tech Stack

- **Language:** Java
- **Framework:** Spring Boot
- **Database:** PostgreSQL
- **Caching:** Redis
- **Containerization:** Docker and Docker Compose
- **Build Tool:** Maven

##  Project Structure

```text
stock-trading-app/
├── ai-service/
├── api-gateway-service/
├── market-data-service/
├── notification-service/
├── order-service/
├── portfolio-service/
├── user-service/
└── docker-compose.yml
```

## Services

- **User Service:** User management and authentication.
- **API Gateway Service:** Entry point for client requests.
- **Market Data Service:** Stock market data management.
- **Order Service:** Stock order management.
- **Portfolio Service:** Portfolio and holdings management.
- **Notification Service:** User notifications.
- **AI Service:** AI-related functionality.

##  Getting Started

### Prerequisites

- Java
- Maven
- Docker
- Docker Compose

### Run the Project

Clone the repository:

```bash
git clone <your-repository-url>
cd stock-trading-app
```

Start the services using Docker Compose:

```bash
docker compose up --build
```

Configure the required environment variables and service settings before running the application.


## Disclaimer

This project is intended for educational purposes and does not execute real stock market transactions.

## License

To be decided.
