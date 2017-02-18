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
package org.cubeengine.converter.node;

import org.cubeengine.converter.InvalidPathException;

/**
 * A Node that can be a parent of another Node
 */
public abstract class ContainerNode<V> extends Node<V>
{
    /**
     * Sets this Node for given path
     *
     * @param path the path
     * @param node Node to set
     *
     * @return the previously mapped Node
     *
     * @throws java.lang.IllegalArgumentException when the given path is not valid e.g. because a node on the path is not a ContainerNode
     */
    public Node set(Path path, Node node)
    {
        if (path.isBasePath())
        {
            return this.set(path.getFirst(), node);
        }
        Node child = this.get(path.getFirst());
        if (child == null)
        {
            child = MapNode.emptyMap();
            this.set(path.getFirst(), child);
        }
        if (child instanceof ContainerNode)
        {
            return ((ContainerNode)child).set(path.subPath(), node);
        }
        throw new InvalidPathException("Invalid Path: " + path.asString("/"));
    }

    /**
     * Gets the Node for given path or null if not found
     *
     * @param path the path
     *
     * @return the Node at given path
     *
     * @throws java.lang.IllegalArgumentException when the given path is not valid e.g. because a node on the path is not a ContainerNode
     */
    public Node get(Path path)
    {
        Node child = this.get(path.getFirst());

        if (child == null || path.isBasePath())
        {
            return child;
        }
        if (child instanceof ContainerNode)
        {
            return ((ContainerNode)child).get(path.subPath());
        }
        throw new InvalidPathException("Invalid Path: " + path.asString("/"));
    }

    /**
     * Removes the Node for given path
     *
     * @param path the path
     *
     * @return the previously mapped Node or null if not set
     */
    public Node remove(Path path)
    {
        if (path.isBasePath())
        {
            return this.remove(path.getFirst());
        }

        Node child = this.get(path.getFirst());
        if (child == null)
        {
            return null;
        }
        if (child instanceof ContainerNode)
        {
            return ((ContainerNode)child).remove(path.subPath());
        }
        throw new InvalidPathException("Invalid Path: " + path.asString("/"));
    }

    /**
     * Sets this Node for given key
     * <p>The key will be lowercased!
     *
     * @param key  the key
     * @param node the Node to set
     *
     * @return the previously mapped Node or null if not set
     */
    public abstract Node set(String key, Node node);

    /**
     * Returns the Node for given direct key (without pathseparators).
     * <p>The key will be lowercased!
     *
     * @param key the key
     *
     * @return the matched Node or null
     */
    public abstract Node get(String key);

    /**
     * Removes the Node for given direct key (without pathseparators).
     * <p>The key will be lowercased!
     *
     * @param key the key
     *
     * @return the previously mapped Node or null if not set
     */
    protected abstract Node remove(String key);

    /**
     * Searches for ParentNodes that do not contain data and deletes them
     */
    public abstract void cleanUpEmptyNodes();

    /**
     * Returns whether this Node contains data
     *
     * @return true if this Node contains no data
     */
    public abstract boolean isEmpty();

    @Override
    public String asText()
    {
        throw new UnsupportedOperationException("ParentNodes cannot be serialized to a simple String! Use toString() if you want a textual representation of this node.");
    }
}
