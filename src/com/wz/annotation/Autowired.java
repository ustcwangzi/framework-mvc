package com.wz.annotation;

import java.lang.annotation.*;

/**
 * Created by wz on 2017-07-17.
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Autowired {
    String value() default "";
}
