/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Anselm Brehme, Phillip Schichtel
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
package de.cubeisland.engine.reflect.util;

public class StringUtils
{
    private StringUtils() {}

    /**
     * This method merges an array of strings to a single string
     *
     * @param delimiter the delimiter
     * @param strings   the strings to implode
     *
     * @return the imploded string
     */
    public static String implode(String delimiter, String[] strings)
    {
        if (strings.length == 0)
        {
            return "";
        }
        else
        {
            StringBuilder sb = new StringBuilder(strings[0]);
            for (int i = 1; i < strings.length; i++)
            {
                sb.append(delimiter).append(strings[i]);
            }
            return sb.toString();
        }
    }

    /**
     * Converts a fieldName into a readable path
     *
     * @param fieldName the field name
     *
     * @return the converted field
     */
    public static String fieldNameToPath(String fieldName)
    {
        final StringBuilder path = new StringBuilder();
        boolean lastUpper = true;
        for (char c : fieldName.toCharArray())
        {
            if (c == '_')
            {
                lastUpper = true;
                path.append('.');
                continue;
            }
            if (Character.isUpperCase(c))
            {
                c = Character.toLowerCase(c);
                if (!lastUpper)
                {
                    lastUpper = true;
                    path.append('-');
                }
            }
            else
            {
                lastUpper = false;
            }
            path.append(c);
        }
        return path.toString();
    }

    public static boolean isEmpty(String string)
    {
        return "".equals(string);
    }
}
