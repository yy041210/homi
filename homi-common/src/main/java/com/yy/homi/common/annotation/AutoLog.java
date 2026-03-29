package com.yy.homi.common.annotation;

import com.yy.homi.common.enums.BusinessType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoLog {
    /** 操作标题 */
    String title() default "";

    /** 业务类型 */
    BusinessType businessType() default BusinessType.OTHER;
}