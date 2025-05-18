// ./Core-Utils/src/main/java/com/minecraft/core/command/annotation/SubCommand.java
package com.minecraft.core.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking a method as a subcommand handler
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubCommand {
    /**
     * The name of the subcommand
     * Use an empty string for the default command (invoked when no subcommand is provided)
     */
    String name() default "";
    
    /**
     * The description of the subcommand
     */
    String description() default "";
    
    /**
     * The usage syntax of the subcommand
     */
    String usage() default "";
    
    /**
     * The permission needed to use this subcommand
     */
    String permission() default "";
    
    /**
     * Aliases for the subcommand
     */
    String[] aliases() default {};
    
    /**
     * The minimum number of arguments required
     */
    int minArgs() default 0;
    
    /**
     * The maximum number of arguments allowed
     * Set to -1 for unlimited arguments
     */
    int maxArgs() default -1;
    
    /**
     * Whether this is the default subcommand (invoked when no subcommand is provided)
     */
    boolean isDefault() default false;
}