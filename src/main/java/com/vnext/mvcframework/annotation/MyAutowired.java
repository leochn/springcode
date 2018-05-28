package com.vnext.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author leo
 * @version 2018/5/27 6:28
 * @since 1.0.0
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {
    String value() default "";

}