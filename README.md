# RepCheck - Workout Tracking API

A Kotlin-based backend API for tracking workouts, sets, and reps with JWT authentication.

## Features

- 🔐 JWT Authentication
- 📝 Workout and set tracking
- 🗄️ PostgreSQL database with Flyway migrations
- 🚀 Built with Ktor
- 🔄 JSON API

## Prerequisites

- Java 21+
- PostgreSQL 15+
- Maven 3.8+

## Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/RepCheck.git
   cd RepCheck
   ```

2. **Set up the database**
   ```sql
   CREATE DATABASE repcheck;
   CREATE USER your_username WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE repcheck TO your_username;
   ```

3. **Configure environment variables**
   Create a `.env` file in the project root:
   ```env
   DB_URL=jdbc:postgresql://localhost:5432/repcheck
   DB_USER=your_username
   DB_PASSWORD=your_password
   JWT_SECRET=your_secure_secret_key_here
   ```

## Running the Application

### Development
```bash
# Install dependencies
mvn clean install

# Run the application
mvn exec:java -Dexec.mainClass="com.repcheck.ApplicationKt"
```

The server will start on `http://localhost:8080`

## API Documentation

### Authentication

#### Register a new user
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securepassword123"
}
```

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securepassword123"
}
```

#### Get current user
```http
GET /api/v1/auth/me
Authorization: Bearer your_jwt_token
```

## Project Structure

```
src/
├── main/
│   ├── kotlin/
│   │   └── com/repcheck/
│   │       ├── config/     # Configuration classes
│   │       ├── features/   # Feature modules (auth, workouts, etc.)
│   │       └── Application.kt
│   └── resources/
│       ├── application.conf
│       └── db/migration/   # Database migrations
└── test/                   # Unit and integration tests
```

## Development

### Running Tests
```bash
mvn test
```

### Code Formatting
```bash
mvn spotless:apply
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Built with ❤️ by [Your Name]
