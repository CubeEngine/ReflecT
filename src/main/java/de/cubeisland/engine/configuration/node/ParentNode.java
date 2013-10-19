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
     * @param pathSeparator the pathSeparator
     * @param node Node to set
     * @return the previously mapped Node or null if not set
     */
    public Node setNodeAt(String path, String pathSeparator, Node node)
    {
        ParentNode parentNode = this.getNodeAt(path, pathSeparator).getParentNode();
        if (parentNode != null)
        {
            return parentNode.setExactNode(getSubKey(path, pathSeparator), node);
        }
        throw new UnsupportedOperationException("Not supported!");
    }

    /**
     * Sets this Node for given direct key (without pathseparators).
     * <p>The key will be lowercased!
     *
     * @param key the key
     * @param node the Node to set
     * @return the previously mapped Node or null if not set
     */
    protected abstract Node setExactNode(String key, Node node);

    /**
     * Gets the Node under the specified path,
     * using pathSeparator to separate
     * <p>Will create new Nodes if not found!
     *
     * @param path the path
     * @param pathSeparator the path-separator
     * @return the Node at given path
     */
    public Node getNodeAt(String path, String pathSeparator)
    {
        if (path.contains(pathSeparator))
        {
            String basePath = getBasePath(path, pathSeparator);
            String subPath = getSubPath(path, pathSeparator);
            Node baseNode = this.getNodeAt(basePath, pathSeparator);
            if (baseNode instanceof NullNode) // Node not found -> create new Node
            {
                if (subPath.startsWith("[")) //  baseNode is a List!
                {
                    baseNode = ListNode.emptyList();
                }
                else
                {
                    baseNode = MapNode.emptyMap();
                }
                baseNode.setParentNode(this);
                this.setExactNode(basePath, baseNode);
            }
            else if (!(baseNode instanceof ParentNode))
            {
                return new ErrorNode("Could not resolve path ("  + path + ") for " + baseNode + "\nIs your configuration outdated?");
            }
            return ((ParentNode)baseNode).getNodeAt(subPath, pathSeparator);
        }
        else
        {
            Node node = this.getExactNode(path);
            if (node == null)
            {
                node = NullNode.emptyNode();
            }
            if (node instanceof NullNode)
            {
                node.setParentNode(this);
                this.setExactNode(path, node);
            }
            return node;
        }
    }

    /**
     * Generates the path for a Node having this Node as ParentNode
     *
     * @param node the Node to get the path for
     * @param path the current path
     * @param pathSeparator the pathSeparator
     * @return the path to given Node OR if path is not empty to the Node pointed in that path
     * @throws IllegalArgumentException when the Node is not managed by this ParentNode
     */
    protected abstract String getPathOfSubNode(Node node, String path, String pathSeparator);

    /**
     * Generates the path for a Node having this Node as ParentNode
     *
     * @param node the Node to get the path for
     * @param pathSeparator the pathSeparator
     * @return the path to given Node
     * @throws IllegalArgumentException when the Node is not managed by this ParentNode
     */
    public abstract String getPathOfSubNode(Node node, String pathSeparator);

    /**
     * Returns the Node for given direct key (without pathseparators).
     * <p>The key will be lowercased!
     *
     * @param key the key
     * @return the matched Node or null
     */
    public abstract Node getExactNode(String key);

    /**
     * Removes the Node for given direct key (without pathseparators).
     * <p>The key will be lowercased!
     *
     * @param key the key
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

    /**
     * Removes the Node for given path
     *
     * @param path the path
     * @param pathSeparator the path-separator
     * @return the previously mapped Node or null if not set
     */
    public Node removeNode(String path, String pathSeparator)
    {
        Node nodeToRemove = this.getNodeAt(path, pathSeparator);
        return nodeToRemove.getParentNode().removeExactNode(getSubKey(path, pathSeparator));
    }

    public abstract boolean removeNode(Node node);

    @Override
    public String asText()
    {
        throw new UnsupportedOperationException("ParentNodes cannot be serialized to a simple String! Use toString() if you want a textual representation of this node.");
    }

}
