package com.minecraft.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking a class as a command handler
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {
    /**
     * The name of the command
     */
    String name();
    
    /**
     * The description of the command
     */
    String description() default "";
    
    /**
     * The usage syntax of the command
     */
    String usage() default "";
    
    /**
     * Aliases for the command
     */
    String[] aliases() default {};
}