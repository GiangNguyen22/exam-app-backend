package com.android.app.exam_app_backend.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to check if caller has at least one of the specified permissions.
 * Usage:
 *   {@code @RequireAnyPermission({PermissionConstants.EXAM_REVIEW, PermissionConstants.EXAM_APPROVE})}
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RequireAnyPermission {
    String[] value();
}
