package org.bigdata.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebFilter(filterName = "AuthorizationFilter", urlPatterns = {
    "/manage.html",
    "/blog-editor.html",
    "/html/manage.html",
    "/html/blog-editor.html"
})
public class AuthorizationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        HttpSession session = req.getSession(false);

        Boolean isAdmin = (session != null) ? (Boolean) session.getAttribute("isAdmin") : null;

        if (isAdmin == null || !isAdmin) {
            String contextPath = req.getContextPath();
            resp.sendRedirect(contextPath + "/html/show-report.html");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }
}
