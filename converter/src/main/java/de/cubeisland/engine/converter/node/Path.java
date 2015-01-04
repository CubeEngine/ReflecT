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
package de.cubeisland.engine.converter.node;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents a path to a node.
 */
public class Path
{
    private final List<String> parts;

    /**
     * Constructs a path using given parts.
     *
     * @param parts the parts
     */
    public Path(String... parts)
    {
        this(Arrays.asList(parts));
    }

    /**
     * Constructs a path using given separator and path.
     *
     * @param separator the separator
     * @param path      the path
     */
    public Path(String separator, String path)
    {
        this(path.split(Pattern.quote(separator)));
    }

    /**
     * Constructs a path using given separator and path.
     *
     * @param separator the separator
     * @param path      the path
     */
    public Path(char separator, String path)
    {
        this(String.valueOf(separator), path);
    }

    /**
     * Constructs a path using give parts
     *
     * @param parts the parts
     */
    public Path(List<String> parts)
    {
        this.parts = Collections.unmodifiableList(parts);
    }

    /**
     * Gets the parts making up this part
     *
     * @return the parts
     */
    public List<String> getParts()
    {
        return parts;
    }

    /**
     * Gets the amount of parts of this path
     *
     * @return the amount of parts
     */
    public int getSize()
    {
        return this.parts.size();
    }

    /**
     * Gets a String representation of this Path with given separator
     *
     * @param separator the separator
     *
     * @return the String representation
     */
    public String asString(String separator)
    {
        String sep = "";
        StringBuilder sb = new StringBuilder();
        for (String part : this.parts)
        {
            sb.append(sep).append(part);
            sep = separator;
        }
        return sb.toString();
    }

    /**
     * Gets a String representation of this Path with given separator
     *
     * @param separator the separator
     *
     * @return the String representation
     */
    public String asString(char separator)
    {
        return asString(String.valueOf(separator));
    }

    /**
     * Returns the last part of this path
     *
     * @return the last part
     */
    public String getLast()
    {
        return this.parts.get(getSize() - 1);
    }

    /**
     * Returns the first part of this path
     *
     * @return the first part
     */
    public String getFirst()
    {
        return this.parts.get(0);
    }

    /**
     * Returns whether this path has only one part
     *
     * @return true when this path has one part
     */
    public boolean isBasePath()
    {
        return getSize() == 1;
    }

    // TODO needed?
    public Path subPath()
    {
        return new Path(parts.subList(1, getSize()));
    }
}
