package de.cubeisland.engine.configuration.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used declare a field to be loaded by the configuration.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface Option
{
    /**
     * The path to save this Field in a config
     *
     * @return the path
     */
    public String value();

    /**
     * If true this option will not be shown in the config with disabled advanced mode.
     * Keep in mind you will need a field:
     *
     * \@Option("advanced")
     * public boolean advanced
     * in your config for this to work.
     *
     * @return if this option is an advanced option
     */
    public boolean advanced() default false;
}
