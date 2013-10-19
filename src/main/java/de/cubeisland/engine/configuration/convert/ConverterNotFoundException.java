package de.cubeisland.engine.configuration.convert;

import java.lang.RuntimeException;
import java.lang.String;

public class ConverterNotFoundException extends RuntimeException
{
    public ConverterNotFoundException(String string)
    {
        super(string);
    }
}
