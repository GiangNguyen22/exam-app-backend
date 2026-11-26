package com.android.app.exam_app_backend.security.aspect;

import com.android.app.exam_app_backend.security.PermissionEvaluatorService;
import com.android.app.exam_app_backend.security.annotation.RequireAnyPermission;
import com.android.app.exam_app_backend.security.annotation.RequirePermission;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    private final PermissionEvaluatorService permissionEvaluatorService;

    public PermissionAspect(PermissionEvaluatorService permissionEvaluatorService) {
        this.permissionEvaluatorService = permissionEvaluatorService;
    }

    @Before("@annotation(requirePermission)")
    public void checkPermission(RequirePermission requirePermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean allowed = permissionEvaluatorService.hasPermission(authentication, requirePermission.value());
        if (!allowed) {
            throw new AccessDeniedException("Access Denied: missing permission " + requirePermission.value());
        }
    }

    @Before("@annotation(requireAnyPermission)")
    public void checkAnyPermission(RequireAnyPermission requireAnyPermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean allowed = permissionEvaluatorService.hasAnyPermission(authentication, requireAnyPermission.value());
        if (!allowed) {
            throw new AccessDeniedException("Access Denied: missing required permissions");
        }
    }
}
