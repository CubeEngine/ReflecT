package de.cubeisland.engine.configuration.exception;

/**
 * This exception is thrown when a configuration could not be instantiated
 */
public class ConfigurationInstantiationException extends InvalidConfigurationException
{
    public ConfigurationInstantiationException(Class clazz, Throwable t)
    {
        super("Failed to create an instance of " + clazz.getName(), t);
    }
}
