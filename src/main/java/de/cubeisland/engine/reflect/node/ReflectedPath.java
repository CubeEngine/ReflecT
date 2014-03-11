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
package de.cubeisland.engine.reflect.node;

/**
 * The Path to a value inside a Node
 */
public class ReflectedPath
{
    /**
     * A basePath indicating a list will start with this String
     */
    static final String LIST = "[";

    private final String basePath;
    private final ReflectedPath subPath;

    /**
     * Create a new ReflectedPath for given name
     *
     * @param name the name
     */
    private ReflectedPath(String name)
    {
        if (name.contains("."))
        {
            this.basePath = name.substring(0, name.indexOf('.'));
            this.subPath = new ReflectedPath(name.substring(name.indexOf('.') + 1));
        }
        else
        {
            this.basePath = name;
            this.subPath = null;
        }
    }

    /**
     * Creates a new ReflectedPath as SubPath of given basePath
     *
     * @param basePath the basePath
     * @param path     the subPath
     */
    private ReflectedPath(String basePath, ReflectedPath path)
    {
        this.basePath = basePath;
        this.subPath = path;
    }

    /**
     * Create a new ReflectedPath as SubPath of given basePath
     */
    public final ReflectedPath asSubPath(String basePath)
    {
        return new ReflectedPath(basePath, this);
    }

    /**
     * Creates a ReflectedPath for given name. The Path will be splitted at every . into basePath and a subPath
     *
     * @param name the textual path
     *
     * @return the ReflectedPath
     */
    public final static ReflectedPath forName(String name)
    {
        return new ReflectedPath(name);
    }

    /**
     * Returns true if this ReflectedPath has no subPath
     *
     * @return true if this ReflectedPath is a basePath
     */
    public final boolean isBasePath()
    {
        return this.subPath == null;
    }

    @Override
    public final String toString()
    {
        if (subPath == null)
        {
            return basePath;
        }
        return basePath + "." + subPath.toString();
    }

    /**
     * Returns the basePath of this ReflectedPath
     *
     * @return the basePath
     */
    public final String getBasePath()
    {
        return basePath;
    }

    /**
     * Gets the subPath of this ReflectedPath
     *
     * @return the subPath or null if {@link #isBasePath()}
     */
    public final ReflectedPath getSubPath()
    {
        return subPath;
    }

    /**
     * Returns true if the basePath of this ReflectedPath points to a list
     *
     * @return whether this path points to a list next
     */
    public final boolean isListPath()
    {
        return this.basePath.startsWith(LIST);
    }

    /**
     * Gets the last part of this path
     *
     * @return the last subPath or if {@link #isBasePath()} the basePath
     */
    public final String getLastSubPath()
    {
        if (this.isBasePath())
        {
            return basePath;
        }
        return this.subPath.getLastSubPath();
    }
}
