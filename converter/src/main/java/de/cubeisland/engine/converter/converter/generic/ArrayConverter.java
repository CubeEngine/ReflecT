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
package de.cubeisland.engine.converter.converter.generic;

import java.lang.reflect.Array;
import java.util.List;

import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.converter.converter.ClassedConverter;
import de.cubeisland.engine.converter.node.ListNode;
import de.cubeisland.engine.converter.node.Node;

/**
 * Converts ListNode to Array and vice versa
 */
public class ArrayConverter implements ClassedConverter<Object>
{
    public Node toNode(Object array, ConverterManager manager) throws ConversionException
    {
        if (!array.getClass().isArray())
        {
            throw ConversionException.of(this, array, "Object to Convert is not an array");
        }

        ListNode result = ListNode.emptyList();
        int len = Array.getLength(array);
        for (int i = 0; i < len; i++)
        {
            result.addNode(manager.convertToNode(Array.get(array, i)));
        }

        return result;
    }

    public Object fromNode(Node node, Class type, ConverterManager manager) throws ConversionException
    {
        if (!(node instanceof ListNode))
        {
            throw ConversionException.of(this, node, "Cannot convert to Array! Node is not a ListNode!");
        }
        Class arrayType = type.getComponentType();
        if (arrayType == null)
        {
            throw ConversionException.of(this, node, "Given type is not an array: " + type.getName());
        }
        List<Node> listedNodes = ((ListNode)node).getValue();
        Object array = Array.newInstance(arrayType, listedNodes.size());
        for (int i = 0; i < listedNodes.size(); i++)
        {
            Array.set(array, i, manager.convertFromNode(listedNodes.get(i), arrayType));
        }

        return array;
    }
}
