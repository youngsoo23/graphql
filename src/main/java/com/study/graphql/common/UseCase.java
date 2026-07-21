package com.study.graphql.common;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an application service as the implementation of one or more inbound ports
 * (use cases). Semantically equivalent to {@link Service}, kept as a distinct
 * stereotype so application-layer classes are easy to spot in a hexagonal codebase.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Service
public @interface UseCase {

    @AliasFor(annotation = Service.class)
    String value() default "";
}
