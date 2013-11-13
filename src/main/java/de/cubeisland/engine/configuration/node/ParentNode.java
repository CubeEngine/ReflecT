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

/**
 * A Node that can be a parent of another Node
 */
public abstract class ParentNode<V> extends Node<V>
{
    /**
     * Sets this Node for given path
     *
     * @param path the path
     * @param node Node to set
     *
     * @return the previously mapped Node or null if not set
     */
    public Node setNodeAt(ConfigPath path, Node node)
    {
        if (path.isBasePath())
        {
            return this.setExactNode(path.getBasePath(), node);
        }
        else
        {
            ParentNode parentNode = this.getNodeAt(path).getParentNode();
            return parentNode.setExactNode(path.getLastSubPath(), node);
        }
    }

    /**
     * Gets the Node under the specified path,
     * using pathSeparator to separate
     * <p>Will create new Nodes if not found!
     *
     * @param path the path
     *
     * @return the Node at given path
     */
    public Node getNodeAt(ConfigPath path)
    {
        if (path.isBasePath())
        {
            Node node = this.getExactNode(path.getBasePath());
            if (node == null)
            {
                node = NullNode.emptyNode();
            }
            if (node instanceof NullNode)
            {
                node.setParentNode(this);
                this.setExactNode(path.getBasePath(), node);
            }
            return node;
        }
        else
        {
            ConfigPath subPath = path.getSubPath();
            Node baseNode = this.getExactNode(path.getBasePath());
            if (baseNode instanceof NullNode) // Node not found -> create new Node
            {
                if (subPath.isListPath()) //  baseNode is a List!
                {
                    baseNode = ListNode.emptyList();
                }
                else
                {
                    baseNode = MapNode.emptyMap();
                }
                baseNode.setParentNode(this);
                this.setExactNode(path.getBasePath(), baseNode);
            }
            else if (!(baseNode instanceof ParentNode))
            {
                return new ErrorNode("Could not resolve path (" + path + ") for " + baseNode + "\nIs your configuration outdated?");
            }
            return ((ParentNode)baseNode).getNodeAt(subPath);
        }
    }

    /**
     * Removes the Node for given path
     *
     * @param path the path
     *
     * @return the previously mapped Node or null if not set
     */
    public Node removeNode(ConfigPath path)
    {
        if (path.isBasePath())
        {
            return this.removeExactNode(path.getBasePath());
        }
        else
        {

            Node baseNode = this.getExactNode(path.getBasePath());
            if (baseNode instanceof ParentNode)
            {
                return ((ParentNode)baseNode).removeNode(path.getSubPath());
            }
            return null; // Node not found
        }
    }

    /**
     * Sets this Node for given direct key
     * <p>The key will be lowercased!
     *
     * @param key  the key
     * @param node the Node to set
     *
     * @return the previously mapped Node or null if not set
     */
    protected abstract Node setExactNode(String key, Node node);

    /**
     * Generates the path for a Node having this Node as ParentNode
     *
     * @param node the Node to get the path for
     * @param path the current path
     *
     * @return the path to given Node OR if path is not empty to the Node pointed in that path
     *
     * @throws IllegalArgumentException when the Node is not managed by this ParentNode
     */
    protected abstract ConfigPath getPathOfSubNode(Node node, ConfigPath path);

    /**
     * Generates the path for a Node having this Node as ParentNode
     *
     * @param node the Node to get the path for
     *
     * @return the path to given Node
     *
     * @throws IllegalArgumentException when the Node is not managed by this ParentNode
     */
    public abstract ConfigPath getPathOfSubNode(Node node);

    /**
     * Returns the Node for given direct key (without pathseparators).
     * <p>The key will be lowercased!
     *
     * @param key the key
     *
     * @return the matched Node or null
     */
    public abstract Node getExactNode(String key);

    /**
     * Removes the Node for given direct key (without pathseparators).
     * <p>The key will be lowercased!
     *
     * @param key the key
     *
     * @return the previously mapped Node or null if not set
     */
    protected abstract Node removeExactNode(String key);

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

    public abstract boolean removeNode(Node node);

    @Override
    public String asText()
    {
        throw new UnsupportedOperationException("ParentNodes cannot be serialized to a simple String! Use toString() if you want a textual representation of this node.");
    }
}
