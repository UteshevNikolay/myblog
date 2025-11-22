# MyBlog on Boot

A RESTful blog application built with Spring Boot, providing API for managing blog posts, comments, and images.

## Features

- Create, read, update, and delete blog posts
- Add and manage comments on posts
- Upload and download images for posts
- Search posts by title or content
- Pagination support for posts
- Like posts functionality
- Tag support for posts

## Technologies Used

- **Java**: 21
- **Spring Boot**: 4.0.0
- **Spring Data JDBC**: For database operations
- **PostgreSQL**: 16
- **Liquibase**: For database migrations
- **Gradle**: For build management
- **Lombok**: For reducing boilerplate code
- **MapStruct**: For object mapping
- **Docker**: For PostgreSQL containerization
- **JUnit 5**: For unit testing
- **Mockito**: For mocking in tests
- **Testcontainers**: For integration testing with real PostgreSQL

## Prerequisites

- Java 21 or higher
- Docker and Docker Compose (for PostgreSQL)
- Gradle (wrapper included)

## Installation and Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd myblogonboot
```

### 2. Start PostgreSQL Database

Use Docker Compose to start the PostgreSQL database:

```bash
docker-compose up -d
```

This will start a PostgreSQL container with the following configuration:
- **Database**: `testdb`
- **User**: `user`
- **Password**: `password`
- **Port**: `5432`

### 3. Build and Run the Application

#### Option 1: Run directly with Gradle

```bash
./gradlew bootRun
```

#### Option 2: Build executable JAR and run it

Build the executable JAR:

```bash
./gradlew clean bootJar
```

This will create an executable JAR file at `build/libs/myblogonboot-0.0.1-SNAPSHOT.jar` (~32 MB).

Run the JAR:

```bash
java -jar build/libs/myblogonboot-0.0.1-SNAPSHOT.jar
```

**Note**: 
- The executable JAR contains all dependencies and can be deployed anywhere with Java 21+
- Liquibase migrations will run automatically on application startup, creating all necessary database tables
- To rebuild the JAR after making changes, run `./gradlew clean bootJar` again

### 4. Access the Application

The application will be available at:
- **API Base URL**: `http://localhost:8080/api/posts`
- **Server Port**: `8080`

### 5. Run Tests

Run all tests (including integration tests with Testcontainers):

```bash
./gradlew test
```

## API Endpoints

### Posts

- `GET /api/posts` - Get all posts (with optional search and pagination)
  - Query params: `search` (optional), `pageNumber` (default: 0), `pageSize` (default: 20)
- `GET /api/posts/{postId}` - Get a specific post by ID
- `POST /api/posts` - Create a new post
- `PUT /api/posts/{postId}` - Update an existing post
- `DELETE /api/posts/{postId}` - Delete a post
- `POST /api/posts/{postId}/likes` - Increment likes for a post

### Comments

- `GET /api/posts/{postId}/comments` - Get all comments for a post
- `GET /api/posts/{postId}/comments/{commentId}` - Get a specific comment
- `POST /api/posts/{postId}/comments` - Add a comment to a post
- `PUT /api/posts/{postId}/comments/{commentId}` - Update a comment

### Images

- `GET /api/posts/{postId}/image` - Download image for a post
- `PUT /api/posts/{postId}/image` - Upload image for a post (multipart/form-data)

## Request/Response Examples

### Create a Post

```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{
    "title": "My First Post",
    "content": "This is the content of my first blog post.",
    "tags": ["spring", "java"]
  }'
```

### Get Posts with Search and Pagination

```bash
curl "http://localhost:8080/api/posts?search=first&pageNumber=0&pageSize=10"
```

### Upload an Image

```bash
curl -X PUT http://localhost:8080/api/posts/1/image \
  -F "image=@/path/to/image.jpg"
```

### Add a Comment

```bash
curl -X POST http://localhost:8080/api/posts/1/comments \
  -H "Content-Type: application/json" \
  -d '{
    "author": "John Doe",
    "content": "Great post!"
  }'
```

## Configuration

### Database Configuration

Database settings are configured in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/testdb
    username: user
    password: password
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
    enabled: true
```

You can override these values using environment variables:
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

## Project Structure

```
myblogonboot/
├── src/
│   ├── main/
│   │   ├── java/com/my/blog/project/myblogonboot/
│   │   │   ├── MyblogonbootApplication.java
│   │   │   └── myblog/
│   │   │       ├── config/          # Configuration classes (CORS)
│   │   │       ├── controller/      # REST controllers
│   │   │       ├── dto/             # Data Transfer Objects
│   │   │       │   ├── comment/
│   │   │       │   ├── post/
│   │   │       │   └── search/
│   │   │       ├── entity/          # Domain entities
│   │   │       ├── mapper/          # MapStruct mappers
│   │   │       ├── repository/      # Spring Data JDBC repositories
│   │   │       └── service/         # Business logic services
│   │   └── resources/
│   │       ├── application.yml      # Application configuration
│   │       └── db/
│   │           └── changelog/       # Liquibase changesets
│   └── test/
│       ├── java/                    # Test classes
│       └── resources/
│           └── application-test.yml # Test configuration
├── build.gradle                     # Gradle build configuration
├── docker-compose.yml               # PostgreSQL container setup
└── README.md
```