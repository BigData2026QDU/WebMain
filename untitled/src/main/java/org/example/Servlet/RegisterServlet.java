package org.example.Servlet;

import org.example.Tool.JsonUtil;
import org.example.User;
import org.example.Model.Response;
import org.example.service.UserService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RegisterServlet extends HttpServlet {

    private final UserService userService = new UserService();

    // Static nested DTO class for parsing the JSON request body.
    // Being static, it doesn't hold an implicit reference to the outer servlet instance,
    // which allows the Jackson JSON library to instantiate it correctly.
    private static class RegisterRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            JsonUtil.writeJsonResponse(resp, Response.error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error reading request body."));
            return;
        }

        String requestBody = sb.toString();
        System.out.println("Register attempt with body: " + requestBody);

        try {
            if (requestBody.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJsonResponse(resp, Response.error(HttpServletResponse.SC_BAD_REQUEST, "Request body is empty."));
                return;
            }

            RegisterRequest registerRequest = JsonUtil.fromJson(requestBody, RegisterRequest.class);
            String username = registerRequest.getUsername();
            String password = registerRequest.getPassword();

            if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                JsonUtil.writeJsonResponse(resp, Response.error(HttpServletResponse.SC_BAD_REQUEST, "Username and password cannot be empty."));
                return;
            }

            User newUser = userService.register(username, password);

            if (newUser != null) {
                resp.setStatus(HttpServletResponse.SC_CREATED);
                newUser.setPassword(null);
                JsonUtil.writeJsonResponse(resp, Response.success(newUser));
            } else {
                resp.setStatus(HttpServletResponse.SC_CONFLICT);
                JsonUtil.writeJsonResponse(resp, Response.error(HttpServletResponse.SC_CONFLICT, "Username already exists."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            JsonUtil.writeJsonResponse(resp, Response.error(HttpServletResponse.SC_BAD_REQUEST, "Invalid JSON format: " + e.getMessage()));
        }
    }
}
