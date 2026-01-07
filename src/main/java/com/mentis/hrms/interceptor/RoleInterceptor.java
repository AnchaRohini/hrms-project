package com.mentis.hrms.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        String uri = request.getRequestURI();

        // Allow login page without session
        if (uri.equals("/candidate/login") || uri.equals("/candidate/auth/login")) {
            return true;
        }


        // 2. Check if user is logged in
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect("/candidate/login?error=Please+login+first");
            return false;
        }

        String role = (String) session.getAttribute("userRole");
        String empId = (String) session.getAttribute("userId");

        // 3. Strict Role-Based Path Protection

        // Protect Admin Routes
        if (uri.startsWith("/dashboard/admin") && !"SUPER_ADMIN".equals(role)) {
            return redirectToHomeBasedOnRole(role, empId, response);
        }

        // Protect HR Routes
        if (uri.startsWith("/dashboard/hr") && !"HR".equals(role) && !"SUPER_ADMIN".equals(role)) {
            return redirectToHomeBasedOnRole(role, empId, response);
        }

        // Protect Candidate/Employee Routes
        if (uri.startsWith("/candidate/dashboard") && !uri.contains("/dashboard/" + empId)) {
            // Prevent Employee A from seeing Employee B's dashboard
            if ("EMPLOYEE".equals(role)) {
                response.sendRedirect("/candidate/dashboard/" + empId);
                return false;
            }
        }

        // Prevent Employee from accessing generic HR /dashboard routes
        if (uri.startsWith("/dashboard") && !uri.startsWith("/dashboard/admin") && !uri.startsWith("/dashboard/hr") && "EMPLOYEE".equals(role)) {
            response.sendRedirect("/candidate/dashboard/" + empId);
            return false;
        }

        return true;
    }

    private boolean redirectToHomeBasedOnRole(String role, String empId, HttpServletResponse response) throws Exception {
        if ("SUPER_ADMIN".equals(role)) response.sendRedirect("/dashboard/admin");
        else if ("HR".equals(role)) response.sendRedirect("/dashboard/hr");
        else response.sendRedirect("/candidate/dashboard/" + empId);
        return false;
    }}