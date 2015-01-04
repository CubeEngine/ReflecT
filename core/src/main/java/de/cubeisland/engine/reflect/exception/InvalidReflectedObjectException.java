/**
 * The MIT License
 * Copyright (c) 2013 Cube Island
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.cubeisland.engine.reflect.exception;

import java.lang.reflect.Field;

import de.cubeisland.engine.converter.node.Path;
import de.cubeisland.engine.reflect.Section;

/**
 * This exception is thrown when a reflected object is invalid.
 */
public class InvalidReflectedObjectException extends RuntimeException
{
    private static final long serialVersionUID = -492268712863444129L;

    public InvalidReflectedObjectException(String message)
    {
        super(message);
    }

    public InvalidReflectedObjectException(String msg, Throwable t)
    {
        super(msg, t);
    }

    public static InvalidReflectedObjectException of(String message, Path path, Class<? extends Section> clazz, Field field, Throwable t)
    {
        String msg = message + "\nField: " + field.getName();
        msg += "\nSection: " + clazz.toString();
        msg += "\nPath: " + path;
        if (t == null)
        {
            return new InvalidReflectedObjectException(msg);
        }
        return new InvalidReflectedObjectException(msg, t);
    }
}
