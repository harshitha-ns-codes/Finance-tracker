# Backend

Spring Boot REST API for the Finance Tracker.

## Run

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Listens on **http://localhost:8081** (avoids clashing with other apps on 8080).

## Build JAR (deploy)

```powershell
cd backend
.\mvnw.cmd -DskipTests package
java -jar target\finance-tracker-0.0.1-SNAPSHOT.jar
```
