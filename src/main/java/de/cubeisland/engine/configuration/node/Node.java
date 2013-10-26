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
package de.cubeisland.engine.configuration.node;

import de.cubeisland.engine.configuration.ConfigPath;

/**
 * A config Node
 */
public abstract class Node<V>
{
    private ParentNode parentNode;

    private String[] comments;

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
     * Constructs a path down too the root Node for this Node
     *
     * @return the path or null if this Node is a root-Node
     */
    public ConfigPath getPath()
    {
        if (this.getParentNode() == null)
        {
            return null;
        }
        return this.getParentNode().getPathOfSubNode(this);
    }

    /**
     * Tries to convert the value of the Node into a string
     *
     * @return the converted Node value
     */
    public abstract String asText();

    /**
     * Gets the Value contained in this Node
     *
     * @return the NodeValue
     */
    public abstract V getValue();

    public String[] getComments()
    {
        return this.comments;
    }

    public void setComments(String[] comments)
    {
        this.comments = comments;
    }

    public abstract String toString();
}
