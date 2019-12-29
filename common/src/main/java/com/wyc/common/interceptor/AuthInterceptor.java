package com.wyc.common.interceptor;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.wyc.common.annotation.Auth;
import com.wyc.common.context.UserContext;
import com.wyc.common.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class AuthInterceptor extends HandlerInterceptorAdapter {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {

        return true;

    }

    /**
     * 处理401.
     */
    private void unauthorized(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);// 401
        response.getWriter()
                .write("{\"success\":false,\"code\":401,\"message\":\"没有登录.\"}");
        response.getWriter().close();
    }

    /**
     * 处理403.
     */
    private void forbidden(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);// 403
        response.getWriter()
                .write("{\"success\":false,\"code\":403,\"message\":\"没有权限.\"}");
        response.getWriter().close();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }

}
