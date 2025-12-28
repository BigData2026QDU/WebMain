package org.example.Filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 授权过滤器
 * 拦截需要管理员权限的页面
 *
 * 拦截的页面：
 * - /manage.html - 报告管理页面
 * - /blog-editor.html - 博客编辑器
 *
 * 注意：此 Filter 必须在 AuthenticationFilter 之后执行
 * AuthenticationFilter 确保 Session 存在，此 Filter 检查权限
 */
@WebFilter(filterName = "AuthorizationFilter", urlPatterns = {"/manage.html", "/blog-editor.html"})
public class AuthorizationFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化（无需操作）
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // 获取 Session（AuthenticationFilter 已确保其存在）
        HttpSession session = req.getSession(false);

        // 获取 isAdmin 属性
        Boolean isAdmin = (session != null) ? (Boolean) session.getAttribute("isAdmin") : null;

        // 检查是否为管理员
        if (isAdmin == null || !isAdmin) {
            // 非管理员，重定向到普通用户报告展示页面
            String contextPath = req.getContextPath();
            resp.sendRedirect(contextPath + "/show-report.html");
            return;
        }

        // 管理员，放行请求
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // 清理资源（无需操作）
    }
}
