package com.wz.annotation;

import java.lang.annotation.*;

/**
 * Created by wz on 2017-07-17.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {
    String value() default "";
}
