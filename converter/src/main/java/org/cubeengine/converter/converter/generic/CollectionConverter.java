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
package org.cubeengine.converter.converter.generic;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.converter.node.ListNode;
import org.cubeengine.converter.node.Node;

/**
 * Converts ListNode to Collection and vice versa
 */
public class CollectionConverter implements GenericConverter<Collection>
{
    public static Collection getCollectionFor(ParameterizedType ptype) throws IllegalAccessException, InstantiationException
    {
        Class collectionType = (Class)ptype.getRawType();
        if (!collectionType.isInterface() && !Modifier.isAbstract(collectionType.getModifiers()))
        {
            return (Collection)collectionType.newInstance();
        }
        if (!Set.class.isAssignableFrom(collectionType))
        {
            // if (List.class.isAssignableFrom(collectionType)) // or other collection
            return new LinkedList();
        }
        if (SortedSet.class.isAssignableFrom(collectionType))
        {
            return new TreeSet();
        }
        return new HashSet();
    }

    public ListNode toNode(Collection collection, ConverterManager manager) throws ConversionException
    {
        ListNode result = ListNode.emptyList();
        if (collection == null || collection.isEmpty())
        {
            return result;
        }
        for (Object value : collection)
        {
            result.addNode(manager.convertToNode(value));
        }
        return result;
    }

    public Collection fromNode(Node node, ParameterizedType pType, ConverterManager manager) throws ConversionException
    {
        if (!(node instanceof ListNode))
        {
            throw ConversionException.of(this, node, "Cannot convert to Collection! Node is not a ListNode!");
        }
        if (!(pType.getRawType() instanceof Class))
        {
            throw new IllegalArgumentException("Unknown Collection-Type: " + pType);
        }
        try
        {
            return fillCollection(getCollectionFor(pType), pType, (ListNode)node, manager);
        }
        catch (IllegalAccessException e)
        {
            throw ConversionException.of(this, node, "Could not create Collection", e);
        }
        catch (InstantiationException e)
        {
            throw ConversionException.of(this, node, "Could not create Collection", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Collection fillCollection(Collection result, ParameterizedType pType, ListNode listNode, ConverterManager manager) throws ConversionException
    {
        Type subType = pType.getActualTypeArguments()[0];

        for (Node node : listNode.getValue())
        {
            result.add(manager.convertFromNode(node, subType));
        }

        return result;
    }
}
