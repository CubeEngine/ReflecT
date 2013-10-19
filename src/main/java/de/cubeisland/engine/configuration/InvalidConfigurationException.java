/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Anselm Brehme
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
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
