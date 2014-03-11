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

import de.cubeisland.engine.reflect.StringUtils;

import java.util.*;
import java.util.Map.Entry;

public class MapNode extends ParentNode<Map<String, Node>>
{
    private LinkedHashMap<String, Node> mappedNodes = new LinkedHashMap<String, Node>();
    private HashMap<String, String> keys = new HashMap<String, String>(); // LowerCase trimmed -> Original
    private LinkedHashMap<Node, String> reverseMappedNodes = new LinkedHashMap<Node, String>();

    /**
     * Creates a MapNode with given map as values.
     * MapKeys of the MapNode will always be a lowercased and trimmed String.
     *
     * @param map the map to convert into Nodes
     */
    public MapNode(Map<?, ?> map)
    {
        if (map != null)
        {
            for (Map.Entry<?, ?> entry : map.entrySet())
            {
                Node node = wrapIntoNode(entry.getValue());
                node.setParentNode(this);
                this.setExactNode(entry.getKey().toString(), node);
            }
        }
    }

    private MapNode()
    {
    }

    @Override
    public Map<String, Node> getValue()
    {
        return this.getMappedNodes();
    }

    /**
     * Returns an empty MapNode
     * <p>This is equivalent to {@link #MapNode(Map)} with null parameter
     *
     * @return an empty MapNode
     */
    public static MapNode emptyMap()
    {
        return new MapNode();
    }

    @Override
    public Node getExactNode(String key)
    {
        Node node = this.mappedNodes.get(key.trim().toLowerCase());
        if (node == null)
        {
            node = NullNode.emptyNode();
        }
        return node;
    }

    @Override
    public Node setExactNode(String key, Node node)
    {
        String loweredKey = key.trim().toLowerCase();
        if (StringUtils.isEmpty(loweredKey))
        {
            throw new IllegalArgumentException("The key for the following node is empty!" + node.toString());
        }
        this.keys.put(loweredKey, key);
        node.setParentNode(this);
        this.reverseMappedNodes.put(node, loweredKey);
        return this.mappedNodes.put(loweredKey, node);
    }

    @Override
    protected Node removeExactNode(String key)
    {
        Node node = this.mappedNodes.remove(key);
        if (node instanceof NullNode)
        {
            this.reverseMappedNodes.remove(node);
            this.mappedNodes.remove(key);
            return null;
        }
        return node;
    }

    public Node setNode(StringNode keyNode, Node node)
    {
        return this.setExactNode(keyNode.getValue(), node);
    }

    public String getOriginalKey(String lowerCasedKey)
    {
        return this.keys.get(lowerCasedKey);
    }

    public LinkedHashMap<String, Node> getMappedNodes()
    {
        return mappedNodes;
    }

    @Override
    public boolean isEmpty()
    {
        return this.mappedNodes.isEmpty();
    }

    @Override
    public boolean removeNode(Node node)
    {
        return this.mappedNodes.values().remove(node);
    }

    @Override
    protected ConfigPath getPathOfSubNode(Node node, ConfigPath path)
    {
        String key = this.reverseMappedNodes.get(node);
        if (key == null)
        {
            throw new IllegalArgumentException("Parented Node not in map!");
        }
        if (path == null)
        {
            path = ConfigPath.forName(key);
        }
        else
        {
            path = path.asSubPath(key);
        }
        if (this.getParentNode() != null)
        {
            return this.getParentNode().getPathOfSubNode(this, path);
        }
        return path;
    }

    @Override
    public ConfigPath getPathOfSubNode(Node node)
    {
        return this.getPathOfSubNode(node, null);
    }

    @Override
    public void cleanUpEmptyNodes()
    {
        Set<String> nodesToRemove = new HashSet<String>();
        for (String key : this.mappedNodes.keySet())
        {
            if (this.mappedNodes.get(key) instanceof ParentNode)
            {
                ((ParentNode)this.mappedNodes.get(key)).cleanUpEmptyNodes();
                if (((ParentNode)this.mappedNodes.get(key)).isEmpty())
                {
                    nodesToRemove.add(key);
                }
            }
        }
        for (String key : nodesToRemove)
        {
            this.mappedNodes.remove(key);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("MapNode=[");
        for (Entry<String, Node> entry : this.mappedNodes.entrySet())
        {
            sb.append("\n").append(entry.getKey()).append(": ").append(entry.getValue().toString());
        }
        sb.append("]MapEnd");
        return sb.toString();
    }

    public String getFirstKey()
    {
        if (this.mappedNodes.isEmpty())
        {
            return null;
        }
        return this.mappedNodes.keySet().iterator().next();
    }
}
