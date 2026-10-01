package com.example.studentapp;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentServer {

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/studentdb?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    // CHANGE THIS to your MySQL root password.
    private static final String DB_PASSWORD = "YOUR_MYSQL_PASSWORD";

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");

        HttpServer server = HttpServer.create(
                new InetSocketAddress("localhost", 8080), 0);

        server.createContext("/api/students", StudentServer::handleStudents);
        server.createContext("/api/health", StudentServer::handleHealth);

        server.setExecutor(null);
        server.start();

        System.out.println("Java + JDBC backend running at http://localhost:8080");
        System.out.println("Connected database: studentdb");
    }

    private static void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        send(exchange, 200, "{\"status\":\"Java/JDBC backend is running\"}");
    }

    private static void handleStudents(HttpExchange exchange) throws IOException {
        addCors(exchange);

        try {
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                send(exchange, 200, getStudents());
            } else if ("POST".equalsIgnoreCase(method)) {
                String body = readBody(exchange);
                String name = jsonValue(body, "name");
                String email = jsonValue(body, "email");
                String course = jsonValue(body, "course");

                if (name.isBlank() || email.isBlank() || course.isBlank()) {
                    send(exchange, 400, "{\"error\":\"All fields are required\"}");
                    return;
                }

                int id = addStudent(name, email, course);
                send(exchange, 201,
                        "{\"message\":\"Student added\",\"id\":" + id + "}");
            } else if ("DELETE".equalsIgnoreCase(method)) {
                String query = exchange.getRequestURI().getQuery();
                int id = query == null ? -1 : getId(query);

                if (id <= 0) {
                    send(exchange, 400, "{\"error\":\"Valid id is required\"}");
                    return;
                }

                if (deleteStudent(id)) {
                    send(exchange, 200, "{\"message\":\"Student deleted\"}");
                } else {
                    send(exchange, 404, "{\"error\":\"Student not found\"}");
                }
            } else if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods",
                        "GET,POST,DELETE,OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                        "Content-Type");
                send(exchange, 204, "");
            } else {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }

        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500,
                    "{\"error\":\"Server/database error: " +
                            jsonEscape(e.getMessage()) + "\"}");
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static String getStudents() throws SQLException {
        List<String> rows = new ArrayList<>();

        String sql = "SELECT id, name, email, course FROM students ORDER BY id DESC";

        try (Connection con = connection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                rows.add("{"
                        + "\"id\":" + rs.getInt("id") + ","
                        + "\"name\":\"" + jsonEscape(rs.getString("name")) + "\","
                        + "\"email\":\"" + jsonEscape(rs.getString("email")) + "\","
                        + "\"course\":\"" + jsonEscape(rs.getString("course")) + "\""
                        + "}");
            }
        }

        return "[" + String.join(",", rows) + "]";
    }

    private static int addStudent(String name, String email, String course)
            throws SQLException {

        String sql = "INSERT INTO students(name, email, course) VALUES (?, ?, ?)";

        try (Connection con = connection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, course);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }

        throw new SQLException("Could not create student");
    }

    private static boolean deleteStudent(int id) throws SQLException {
        String sql = "DELETE FROM students WHERE id = ?";

        try (Connection con = connection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private static int getId(String query) {
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && pair[0].equals("id")) {
                try {
                    return Integer.parseInt(pair[1]);
                } catch (NumberFormatException ignored) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    // Small JSON helper for this learning project.
    private static String jsonValue(String json, String key) {
        String marker = "\"" + key + "\"";
        int keyPos = json.indexOf(marker);
        if (keyPos < 0) return "";

        int colon = json.indexOf(':', keyPos + marker.length());
        if (colon < 0) return "";

        int start = json.indexOf('"', colon + 1);
        if (start < 0) return "";

        StringBuilder value = new StringBuilder();
        boolean escaped = false;

        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                value.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                value.append(c);
            }
        }

        return value.toString().trim();
    }

    private static String jsonEscape(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static void addCors(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods",
                "GET,POST,DELETE,OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                "Content-Type");
    }

    private static void send(HttpExchange exchange, int status, String body)
            throws IOException {

        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        if (status == 204) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            exchange.getResponseHeaders().set("Content-Type",
                    "application/json; charset=UTF-8");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
        }

        exchange.close();
    }
}
