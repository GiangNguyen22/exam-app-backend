package com.android.app.exam_app_backend.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to check if the caller has a specific permission.
 * Handled cleanly by {@link com.android.app.exam_app_backend.security.aspect.PermissionAspect}.
 *
 * Usage:
 *   {@code @RequirePermission(PermissionConstants.EXAM_REVIEW)}
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface RequirePermission {
    String value();
}
