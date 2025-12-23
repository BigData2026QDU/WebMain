package org.example.filter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AuthenticationFilter implements Filter {

    private final Set<String> allowedUris = new HashSet<>(Arrays.asList(
            "/login.html",
            "/login",
            "/register.html",
            "/register",
            "/index.jsp"
    ));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false); // false: don't create new session

        String requestURI = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

        // Allow access to login page, static resources (css, js, etc.), and other public pages
        if (isPublicResource(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        boolean loggedIn = (session != null && session.getAttribute("userId") != null);

        if (loggedIn) {
            // User is logged in, continue with the request
            chain.doFilter(request, response);
        } else {
            // User is not logged in, redirect to login page
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.html");
        }
    }

    private boolean isPublicResource(String requestURI) {
        if (allowedUris.contains(requestURI)) {
            return true;
        }
        // Allow access to static resources
        return requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/"); // Assuming you might have an images folder
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization code, if needed
    }

    @Override
    public void destroy() {
        // Cleanup code, if needed
    }
}
