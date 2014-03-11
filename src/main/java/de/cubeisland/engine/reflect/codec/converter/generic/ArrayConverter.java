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
package de.cubeisland.engine.reflect.codec.converter.generic;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedList;

import de.cubeisland.engine.reflect.codec.ConverterManager;
import de.cubeisland.engine.reflect.exception.ConversionException;
import de.cubeisland.engine.reflect.node.ListNode;
import de.cubeisland.engine.reflect.node.Node;

public class ArrayConverter
{
    public ListNode toNode(Object[] array, ConverterManager manager) throws ConversionException
    {
        ListNode result = ListNode.emptyList();
        if (array == null || array.length == 0)
        {
            return result;
        }
        for (Object value : array)
        {
            result.addNode(manager.convertToNode(value));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <V> V[] fromNode(Class<V[]> arrayType, ListNode listNode, ConverterManager manager) throws ConversionException
    {
        Class<V> valueType = (Class<V>)arrayType.getComponentType();
        Collection<V> result = new LinkedList<V>();
        for (Node node : listNode.getListedNodes())
        {
            V value = manager.convertFromNode(node, valueType);
            result.add(value);
        }
        return result.toArray((V[])Array.newInstance((Class)valueType, result.size()));
    }
}