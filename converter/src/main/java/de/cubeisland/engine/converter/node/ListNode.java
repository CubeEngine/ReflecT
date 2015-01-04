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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A List Node
 * <p>It can contain a list of other Nodes
 */
public class ListNode extends ContainerNode
{
    private List<Node> listedNodes = new ArrayList<Node>();

    /**
     * Creates an Empty ListNode
     *
     * @return the empty listnode
     */
    public static ListNode emptyList()
    {
        return new ListNode();
    }

    @Override
    public List<Node> getValue()
    {
        return this.listedNodes;
    }

    /**
     * Adds a node to the list
     *
     * @param node the node to add
     */
    public void addNode(Node node)
    {
        this.listedNodes.add(node);
    }

    @Override
    public Node set(String key, Node node)
    {
        try
        {
            return this.listedNodes.set(Integer.parseInt(key), node);
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException("Cannot set Node! Could not parse ListPath", ex);
        }
        catch (IndexOutOfBoundsException ex)
        {
            throw new IllegalArgumentException("Cannot set Node! Out of Range!", ex);
        }
    }

    @Override
    public Node get(String key)
    {
        try
        {
            return this.listedNodes.get(Integer.parseInt(key));
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException("Cannot get Node! Could not parse ListPath", ex);
        }
        catch (IndexOutOfBoundsException ex)
        {
            throw new IllegalArgumentException("Cannot get Node! Out of Range!", ex);
        }
    }

    @Override
    protected Node remove(String key)
    {
        try
        {
            return this.listedNodes.remove(Integer.parseInt(key));
        }
        catch (NumberFormatException ex)
        {
            throw new IllegalArgumentException("Cannot remove Node! Could not parse ListPath!", ex);
        }
        catch (IndexOutOfBoundsException ex)
        {
            throw new IllegalArgumentException("Cannot remove Node! Out of Range!", ex);
        }
    }

    @Override
    public boolean isEmpty()
    {
        return this.listedNodes.isEmpty();
    }

    @Override
    public void cleanUpEmptyNodes()
    {
        Set<Node> nodesToRemove = new HashSet<Node>();
        for (Node node : this.getValue())
        {
            if (node instanceof ContainerNode)
            {
                ((ContainerNode)node).cleanUpEmptyNodes();
                if (((ContainerNode)node).isEmpty())
                {
                    nodesToRemove.add(node);
                }
            }
        }
        this.listedNodes.removeAll(nodesToRemove);
    }

    @Override
    public String asString()
    {
        StringBuilder sb = new StringBuilder("ListNode=[");
        for (Node listedNode : this.listedNodes)
        {
            sb.append("\n- ").append(listedNode.asString());
        }
        sb.append("]ListEnd");
        return sb.toString();
    }
}
