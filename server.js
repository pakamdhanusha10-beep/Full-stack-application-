const express = require("express");
const path = require("path");

const app = express();
const PORT = 3000;
const JAVA_URL = "http://localhost:8080";

app.use(express.json());
app.use(express.static(path.join(__dirname, "../frontend")));

async function callJava(url, options = {}) {
    const response = await fetch(JAVA_URL + url, options);
    const text = await response.text();

    let data;
    try {
        data = JSON.parse(text);
    } catch {
        data = { error: text || "Java backend returned no data" };
    }

    return { response, data };
}

app.get("/api/students", async (req, res) => {
    try {
        const { response, data } = await callJava("/api/students");
        res.status(response.status).json(data);
    } catch (error) {
        res.status(503).json({
            error: "Cannot reach Java backend. Start the Java server first."
        });
    }
});

app.post("/api/students", async (req, res) => {
    try {
        const { response, data } = await callJava("/api/students", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(req.body)
        });

        res.status(response.status).json(data);
    } catch (error) {
        res.status(503).json({
            error: "Cannot reach Java backend. Start the Java server first."
        });
    }
});

app.delete("/api/students/:id", async (req, res) => {
    try {
        const { response, data } =
            await callJava(`/api/students?id=${encodeURIComponent(req.params.id)}`, {
                method: "DELETE"
            });

        res.status(response.status).json(data);
    } catch (error) {
        res.status(503).json({
            error: "Cannot reach Java backend. Start the Java server first."
        });
    }
});

app.get("/api/health", async (req, res) => {
    try {
        const { response, data } = await callJava("/api/health");
        res.status(response.status).json(data);
    } catch {
        res.status(503).json({
            error: "Java backend is not running"
        });
    }
});

app.get("*", (req, res) => {
    res.sendFile(path.join(__dirname, "../frontend/index.html"));
});

app.listen(PORT, () => {
    console.log(`Web application: http://localhost:${PORT}`);
    console.log(`Node API gateway: http://localhost:${PORT}/api`);
});
