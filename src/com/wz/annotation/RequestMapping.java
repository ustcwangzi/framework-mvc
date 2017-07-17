package com.wz.annotation;

import java.lang.annotation.*;

/**
 * Created by wz on 2017-07-17.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestMapping {
    String value() default "";
}
