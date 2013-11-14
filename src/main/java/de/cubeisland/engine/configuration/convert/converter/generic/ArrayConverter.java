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
package de.cubeisland.engine.configuration.convert.converter.generic;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedList;

import de.cubeisland.engine.configuration.codec.ConverterManager;
import de.cubeisland.engine.configuration.exception.ConversionException;
import de.cubeisland.engine.configuration.node.ListNode;
import de.cubeisland.engine.configuration.node.Node;

public class ArrayConverter
{
    private ConverterManager converters;

    public ArrayConverter(ConverterManager converters)
    {
        this.converters = converters;
    }

    public ListNode toNode(Object[] array) throws ConversionException
    {
        ListNode result = ListNode.emptyList();
        if (array == null || array.length == 0)
        {
            return result;
        }
        for (Object value : array)
        {
            result.addNode(converters.convertToNode(value));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <V> V[] fromNode(Class<V[]> arrayType, ListNode listNode) throws ConversionException
    {
        Class<V> valueType = (Class<V>)arrayType.getComponentType();
        Collection<V> result = new LinkedList<V>();
        for (Node node : listNode.getListedNodes())
        {
            V value = converters.convertFromNode(node, valueType);
            result.add(value);
        }
        return result.toArray((V[])Array.newInstance((Class)valueType, result.size()));
    }
}
