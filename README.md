# Full Stack Student Management App

Technologies:
- HTML + CSS + JavaScript frontend
- Node.js + Express API gateway
- Java HTTP backend
- JDBC
- MySQL
- Maven

## Requirements
1. JDK 17 or newer
2. Node.js 18 or newer
3. MySQL Server
4. VS Code
5. Internet access the first time Maven downloads the MySQL JDBC driver and Node downloads Express

## 1. Database
Open MySQL Workbench and run `database.sql`.
Default MySQL settings expected by this project:
- host: localhost
- port: 3306
- username: root
- password: your MySQL password

If your password is different, edit:
`java-backend/src/main/java/com/example/studentapp/StudentServer.java`

Change:
`DB_PASSWORD = "YOUR_MYSQL_PASSWORD";`

## 2. Start Java + JDBC backend
Open a terminal in `java-backend`:

Windows:
    mvn clean compile
    mvn exec:java

The Java backend runs at:
http://localhost:8080

## 3. Start Node.js server
Open another terminal in `node-server`:

    npm install
    npm start

The web application runs at:
http://localhost:3000

Open that address in your browser.

## Application features
- Add student
- View students
- Delete student
- Search students
- Java/JDBC performs database work
- Node.js provides the frontend-facing API

## Architecture

Browser (HTML/JS)
        |
        v
Node.js :3000
        |
        v
Java :8080
        |
        v
JDBC
        |
        v
MySQL :3306
