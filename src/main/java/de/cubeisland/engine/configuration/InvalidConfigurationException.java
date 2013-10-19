package de.cubeisland.engine.configuration;

import java.lang.reflect.Field;
import java.nio.file.Path;

/**
 * This exception is thrown when a configuration is invalid.
 */
public class InvalidConfigurationException extends RuntimeException
{
    private static final long serialVersionUID = -492268712863444129L;

    public InvalidConfigurationException(String message)
    {
        super(message);
    }

    public InvalidConfigurationException(String msg, Throwable t)
    {
        super(msg, t);
    }

    public static InvalidConfigurationException of(String msg, Path file, String path, Class<? extends Configuration> clazz, Field field , Throwable t)
    {
        if (file != null)
        {
            msg += "\nFile: " + file.toAbsolutePath();
        }
        msg += "\nPath: " + path;
        msg += "\nConfig: " + clazz.toString();
        msg += "\nField: " + field.getName();
        return new InvalidConfigurationException(msg,t);
    }
}
