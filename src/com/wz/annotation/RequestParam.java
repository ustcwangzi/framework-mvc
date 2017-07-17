package com.wz.annotation;

import java.lang.annotation.*;

/**
 * Created by wz on 2017-07-17.
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestParam {
    String value() default "";
    boolean required() default true;
}
