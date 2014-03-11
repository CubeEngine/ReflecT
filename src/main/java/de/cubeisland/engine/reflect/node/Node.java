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

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A reflected Node
 */
public abstract class Node<V>
{
    private ParentNode parentNode;
    private String[] comments = null;

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
    public ReflectedPath getPath()
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

    /**
     * Gets the comments of this node
     *
     * @return the comments or null
     */
    public String[] getComments()
    {
        return this.comments == null ? null : this.comments.clone();
    }

    /**
     * Sets the comments of this node
     *
     * @param comments the comments to set
     */
    public void setComments(String[] comments)
    {
        this.comments = comments == null ? null : comments.clone();
    }

    public abstract String toString();

    /**
     * Wraps a serialized Object into a Node
     *
     * @param o a serialized Object
     *
     * @return the wrapped object
     */
    public static Node wrapIntoNode(Object o)
    {
        if (o == null)
        {
            return NullNode.emptyNode();
        }
        if (o instanceof Map)
        {
            return new MapNode((Map<?, ?>)o);
        }
        if (o instanceof Collection)
        {
            return new ListNode((List<?>)o);
        }
        if (o.getClass().isArray())
        {
            return new ListNode((Object[])o);
        }
        if (o instanceof String)
        {
            return new StringNode((String)o);
        }
        if (o instanceof Byte || o.getClass() == byte.class)
        {
            return new ByteNode((Byte)o);
        }
        if (o instanceof Short || o.getClass() == short.class)
        {
            return new ShortNode((Short)o);
        }
        if (o instanceof Integer || o.getClass() == int.class)
        {
            return new IntNode((Integer)o);
        }
        if (o instanceof Long || o.getClass() == long.class)
        {
            return new LongNode((Long)o);
        }
        if (o instanceof Float || o.getClass() == float.class)
        {
            return new FloatNode((Float)o);
        }
        if (o instanceof Double || o.getClass() == double.class)
        {
            return new DoubleNode((Double)o);
        }
        if (o instanceof Boolean || o.getClass() == boolean.class)
        {
            return BooleanNode.of((Boolean)o);
        }
        if (o instanceof Character || o.getClass() == char.class)
        {
            return new CharNode((Character)o);
        }
        throw new IllegalArgumentException("Cannot wrap into Node: " + o.getClass());
    }
}
