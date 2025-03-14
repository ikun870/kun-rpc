package com.kunclass.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({java.lang.annotation.ElementType.METHOD})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface TryTimes {
    int tryTimes() default 3;
    int interval() default 2000;
}
