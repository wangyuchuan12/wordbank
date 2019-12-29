package com.wyc.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
    enum Generator{
        NATIVE,UUID,SEQUENCE,MAX_TO_INT
    }
    Generator generator()default Generator.NATIVE;

    String sequence()default "";
    boolean auto() default false;
}
