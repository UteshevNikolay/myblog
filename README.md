# MyBlog

A simple blog application built with Spring Framework, providing REST API for managing blog posts, comments, and images.

## Features

- Create, read, update, and delete blog posts
- Add and manage comments on posts
- Upload and download images for posts
- Search posts by title or content
- Pagination support for posts
- Like posts

## Technologies Used

- **Java**: 21
- **Spring Framework**: 6.1.0
- **Spring Data JPA**: 3.3.4
- **Hibernate**: 6.6.1.Final
- **PostgreSQL**: 16
- **Liquibase**: 4.29.2 (for database migrations)
- **Maven**: For build management
- **Tomcat**: For deployment
- **Docker**: For PostgreSQL containerization
- **JUnit 5**: For unit testing
- **Mockito**: For mocking in tests
- **Testcontainers**: For integration testing with real PostgreSQL

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- Docker and Docker Compose
- Apache Tomcat 10.1+ (for deployment)

## Installation and Setup

### 0. Frontend Setup (Optional)
To use fronted download dist and run docker-compose like in lesson instruction

### 1. Clone the Repository

```bash
git clone <repository-url>
cd myblog
```

### 2. Start PostgreSQL Database

Use Docker Compose to start the PostgreSQL database:

```bash
docker-compose up -d
```

This will start a PostgreSQL container with the following configuration:
- Database: `testdb`
- User: `user`
- Password: `password`
- Port: `5432`

### 3. Run Database Migrations

Apply Liquibase migrations to set up the database schema:

```bash
mvn liquibase:update
```
**Note**: Liquibase can also be run automatically during application startup if configured.

### 4. Build the Application

Compile and package the application:

```bash
mvn clean package
```

This will generate a `target/ROOT.war` file.

### 5. Deploy to Tomcat
Download and install Apache Tomcat if you haven't already.
Use the provided deployment script:

```bash
chmod +x deploy.sh
./deploy.sh
```

`check-deployment.sh`
Run this to diagnose deployment issues:
```bash
./check-deployment.sh
```

This script will:
- Build the WAR file
- Copy it to Tomcat's webapps directory
- Start Tomcat

**Note**: Update the `TOMCAT_HOME` path in `deploy.sh` to match your Tomcat installation directory.

## Usage

Once deployed, the application will be available at `http://localhost:8080/`.

### API Endpoints

#### Posts

- `GET /api/posts` - Get all posts (with optional search, pagination)
- `GET /api/posts/{postId}` - Get a specific post
- `POST /api/posts` - Create a new post
- `PUT /api/posts/{postId}` - Update a post
- `DELETE /api/posts/{postId}` - Delete a post
- `POST /api/posts/{postId}/likes` - Like a post

#### Comments

- `GET /api/posts/{postId}/comments` - Get comments for a post
- `GET /api/posts/{postId}/comments/{commentId}` - Get a specific comment
- `POST /api/posts/{postId}/comments` - Add a comment to a post
- `PUT /api/posts/{postId}/comments/{commentId}` - Update a comment

#### Images

- `GET /api/posts/{postId}/image` - Download image for a post
- `PUT /api/posts/{postId}/image` - Upload image for a post

### Request/Response Examples

#### Create a Post

```bash
curl -X POST http://localhost:8080/myblog/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My First Post",
    "content": "This is the content of my first blog post."
  }'
```

#### Get Posts with Search

```bash
curl "http://localhost:8080/myblog/api/posts?search=first&pageNumber=0&pageSize=10"
```

## Testing

Run the tests using Maven:

```bash
mvn test
```

The project includes:
- Unit tests for services
- Integration tests for repositories and controllers
- Tests use Testcontainers for real database testing

## Configuration

### Database Configuration

Database settings are configured in `src/main/resources/liquibase.properties`:

- URL: `jdbc:postgresql://localhost:5432/testdb`
- Username: `user`
- Password: `password`

## Project Structure

```
src/
├── main/
│   ├── java/org/myblog/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── entity/          # JPA entities
│   │   ├── mapper/          # MapStruct mappers
│   │   ├── repository/      # JPA repositories
│   │   └── service/         # Business logic services
│   ├── resources/
│   │   ├── db/              # Liquibase changelogs
│   │   └── liquibase.properties
│   └── webapp/              # Web application resources
└── test/
    └── java/org/myblog/      # Test classes
```