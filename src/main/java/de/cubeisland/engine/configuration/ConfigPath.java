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

/**
 * The Path to a value inside a Node
 */
public class ConfigPath
{
    private static final String LIST = "[";

    private final String basePath;
    private final ConfigPath subPath;

    private ConfigPath(String name)
    {
        if (name.contains("."))
        {
            this.basePath = name.substring(0, name.indexOf("."));
            this.subPath = new ConfigPath(name.substring(name.indexOf(".") + 1));
        }
        else
        {
            this.basePath = name;
            this.subPath = null;
        }
    }

    /**
     * Creates a new ConfigPath
     *
     * @param basepath the basePath
     * @param path the subPath
     */
    public ConfigPath(String basepath, ConfigPath path)
    {
        this.basePath = basepath;
        this.subPath = path;
    }

    /**
     * Creates a ConfigPath for given name. The Path will be splitted at every . into basePath and a subPath
     *
     * @param name the textual path
     * @return the ConfigPath
     */
    public static ConfigPath forName(String name)
    {
        return new ConfigPath(name);
    }

    /**
     * Returns true if this ConfigPath has no subPath
     *
     * @return true if this ConfigPath is a basePath
     */
    public boolean isBasePath()
    {
        return this.subPath == null;
    }

    @Override
    public String toString()
    {
        if (subPath == null)
        {
            return basePath;
        }
        return basePath + "." + subPath.toString();
    }

    /**
     * Returns the basePath of this ConfigPath
     *
     * @return the basePath
     */
    public String getBasePath()
    {
        return basePath;
    }

    /**
     * Gets the subPath of this ConfigPath
     *
     * @return the subPath or null if {@link #isBasePath()}
     */
    public ConfigPath getSubPath()
    {
        return subPath;
    }

    /**
     * Returns true if the basePath of this ConfigPath points to a list
     *
     * @return whether this path points to a list next
     */
    public boolean isListPath()
    {
        return this.basePath.startsWith(LIST);
    }

    /**
     * Gets the last part of this path
     *
     * @return the last subPath or if {@link #isBasePath()} the basePath
     */
    public String getLastSubPath()
    {
        if (this.isBasePath())
        {
            return basePath;
        }
        return this.subPath.getLastSubPath();
    }
}
