package de.cubeisland.engine.configuration.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to attach Comments to values inside maps.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface MapComment
{
    /**
     * Needed to add a Comment at given path
     *
     * @return the path
     */
    public String path();

    /**
     * Needed to add a Comment at given path
     *
     * @return the comment
     */
    public String text();
}
