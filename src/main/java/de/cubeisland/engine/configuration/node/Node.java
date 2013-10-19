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
package de.cubeisland.engine.configuration.node;

/**
 * A config Node
 */
public abstract class Node<V>
{
    private ParentNode parentNode;

    private String comment;

    /**
     * Gets the ParentNode
     *
     * @return the ParentNode OR null if not set
     */
    public ParentNode getParentNode()
    {
        return parentNode;
    }

    /**
     * Sets a ParentNode for this Node
     *
     * @param parentNode the ParentNode
     */
    public void setParentNode(ParentNode parentNode)
    {
        this.parentNode = parentNode;
    }

    /**
     * Constructs a path down too the root de.cubeisland.engine.configuration.node for this de.cubeisland.engine.configuration.node
     *
     * @param pathSeparator the path-separator to use
     * @return the path or null if this de.cubeisland.engine.configuration.node is a root-de.cubeisland.engine.configuration.node
     */
    public String getPath(String pathSeparator)
    {
        if (this.getParentNode() == null)
        {
            return null;
        }
        return this.getParentNode().getPathOfSubNode(this, pathSeparator);
    }

    /**
     * Tries to convert the value of the de.cubeisland.engine.configuration.node into a string
     *
     * @return the converted de.cubeisland.engine.configuration.node value
     */
    public abstract String asText();

    /**
     * Gets the Value contained in this Node
     *
     * @return the NodeValue
     */
    public abstract V getValue();

    /**
     * Returns the last subKey of this path
     *
     * <p>Example: first.second.third -> third
     *
     * @param path the path
     * @param pathSeparator the pathSeparator
     * @return the last subKey
     */
    public static String getSubKey(String path, String pathSeparator)
    {
        if (path.contains(pathSeparator))
        {
            return path.substring(path.lastIndexOf(pathSeparator) + 1);
        }
        else
        {
            return path;
        }
    }

    /**
     * Returns the subPath of this path
     *
     * <p>Example: first.second.third -> second.third
     *
     * @param path the path
     * @param pathSeparator the pathSeparator
     * @return the subPath
     */
    public static String getSubPath(String path, String pathSeparator)
    {
        if (path.contains(pathSeparator))
        {
            return path.substring(path.indexOf(pathSeparator) + 1);
        }
        else
        {
            return path;
        }
    }

    /**
     * Returns the base path of this path
     * <p>Example: first.second.third -> first
     *
     * @param path the path
     * @param pathSeparator the pathSeparator
     * @return the basePath
     */
    public static String getBasePath(String path, String pathSeparator)
    {
        if (path.contains(pathSeparator))
        {
            return path.substring(0, path.indexOf(pathSeparator));
        }
        else
        {
            return path;
        }
    }

    public String getComment()
    {
        return this.comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public abstract String toString();
}
