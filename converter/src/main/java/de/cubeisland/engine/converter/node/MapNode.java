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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A MapNode
 * <p>It can map KeyNodes onto other Nodes
 */
public class MapNode extends ContainerNode<Map<String, Node>>
{
    private Map<String, Node> mappedNodes = new LinkedHashMap<String, Node>();
    /**
     * LowerCase trimmed -> Original
     */
    private Map<String, String> keys = new HashMap<String, String>();
    private Map<Node, String> reverseMappedNodes = new LinkedHashMap<Node, String>();

    public MapNode()
    {
    }

    @Override
    public Map<String, Node> getValue()
    {
        return this.getMappedNodes();
    }

    /**
     * Creates an empty MapNode
     *
     * @return an empty MapNode
     */
    public static MapNode emptyMap()
    {
        return new MapNode();
    }

    @Override
    public Node get(String key)
    {
        return this.mappedNodes.get(key.trim().toLowerCase());
    }

    @Override
    public final Node set(String key, Node node)
    {
        String loweredKey = key.trim().toLowerCase();
        if (loweredKey.isEmpty())
        {
            throw new IllegalArgumentException("The key for the following node is empty!" + node.toString());
        }
        this.keys.put(loweredKey, key);
        this.reverseMappedNodes.put(node, loweredKey);
        return this.mappedNodes.put(loweredKey, node);
    }

    @Override
    protected final Node remove(String key)
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

    public String getOriginalKey(String lowerCasedKey)
    {
        return this.keys.get(lowerCasedKey);
    }

    public Map<String, Node> getMappedNodes()
    {
        return mappedNodes;
    }

    @Override
    public boolean isEmpty()
    {
        return this.mappedNodes.isEmpty();
    }

    @Override
    public void cleanUpEmptyNodes()
    {
        Set<String> nodesToRemove = new HashSet<String>();
        for (String key : this.mappedNodes.keySet())
        {
            if (this.mappedNodes.get(key) instanceof ContainerNode)
            {
                ((ContainerNode)this.mappedNodes.get(key)).cleanUpEmptyNodes();
                if (((ContainerNode)this.mappedNodes.get(key)).isEmpty())
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
    public String asString()
    {
        StringBuilder sb = new StringBuilder("MapNode=[");
        for (Entry<String, Node> entry : this.mappedNodes.entrySet())
        {
            sb.append("\n").append(entry.getKey()).append(": ").append(entry.getValue().asString());
        }
        sb.append("]MapEnd");
        return sb.toString();
    }

    /**
     * Returns the first key of this MapNode or null if the map is empty
     *
     * @return the first key or null
     */
    public String getFirstKey()
    {
        if (this.mappedNodes.isEmpty())
        {
            return null;
        }
        return this.mappedNodes.keySet().iterator().next();
    }

    public void inheritFrom(Node node)
    {
        if (node instanceof MapNode)
        {
            Map<String, Node> inheritFrom = ((MapNode)node).getMappedNodes();
            for (Entry<String, Node> entry : inheritFrom.entrySet())
            {
                Node mapped = mappedNodes.get(entry.getKey());
                if (mapped == null || mapped instanceof NullNode)
                {
                    Node inherited = entry.getValue();
                    inherited.setInherited(true);
                    this.mappedNodes.put(entry.getKey(), inherited);
                }
                if (mapped instanceof MapNode)
                {
                    ((MapNode)mapped).inheritFrom(entry.getValue());
                }
            }
        }
    }
}
