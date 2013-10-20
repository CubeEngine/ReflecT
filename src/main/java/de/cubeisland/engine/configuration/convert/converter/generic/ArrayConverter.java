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
package de.cubeisland.engine.configuration.convert.converter.generic;

import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.node.ListNode;
import de.cubeisland.engine.configuration.node.Node;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedList;

import static de.cubeisland.engine.configuration.Configuration.convertToNode;
import static de.cubeisland.engine.configuration.Configuration.convertFromNode;

public class ArrayConverter
{
    public ListNode toNode(Object[] array) throws ConversionException
    {
        ListNode result = ListNode.emptyList();
        if (array.length == 0)
        {
            return result;
        }
        for (Object value : array)
        {
            result.addNode(convertToNode(value));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <V> V[] fromNode(Class<V[]> arrayType, ListNode listNode) throws ConversionException
    {
        Class<V> valueType = (Class<V>)arrayType.getComponentType();
        try
        {
            Collection<V> result = new LinkedList<>();
            for (Node node : listNode.getListedNodes())
            {
                V value = convertFromNode(node, valueType);
                result.add(value);
            }
            return result.toArray((V[])Array.newInstance((Class)valueType, result.size()));
        }
        catch (ConversionException ex)
        {
            throw new IllegalStateException("Array-conversion failed: Error while converting the values in the array.");
        }
    }
}
