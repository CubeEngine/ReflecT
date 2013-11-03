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

import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.node.ListNode;
import de.cubeisland.engine.configuration.node.Node;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static de.cubeisland.engine.configuration.Configuration.CONVERTERS;

public class CollectionConverter
{
    /**
     * Returns the converted collection
     *
     * @param collection the collection to convert
     *
     * @return the converted collection
     *
     * @throws ConversionException
     */
    public ListNode toNode(Collection collection) throws ConversionException
    {
        ListNode result = ListNode.emptyList();
        if (collection == null || collection.isEmpty())
        {
            return result;
        }
        for (Object value : collection)
        {
            result.addNode(CONVERTERS.convertToNode(value));
        }
        return result;
    }

    /**
     * Deserializes an object back to a collection
     *
     * @param <V>      the ValueType
     * @param <S>      the Type of collection
     * @param pType    the Type of the collection
     * @param listNode the Node to convert
     *
     * @return the converted collection
     *
     * @throws ConversionException
     */
    @SuppressWarnings("unchecked")
    public <V, S extends Collection<V>> S fromNode(ParameterizedType pType, ListNode listNode) throws ConversionException
    {
        try
        {
            if (pType.getRawType() instanceof Class)
            {
                S result = getCollectionFor(pType);
                Type subType = pType.getActualTypeArguments()[0];
                for (Node node : listNode.getListedNodes())
                {
                    V value = CONVERTERS.convertFromNode(node, subType);
                    result.add(value);
                }
                return result;
            }
            throw new IllegalArgumentException("Unknown Collection-Type: " + pType);
        }
        catch (ConversionException ex)
        {
            throw new IllegalStateException("Collection-conversion failed: Error while converting the values in the collection.", ex);
        }
    }

    @SuppressWarnings("unchecked")
    public static <S extends Collection> S getCollectionFor(ParameterizedType ptype)
    {
        try
        {
            Class<S> collectionType = (Class)ptype.getRawType();
            S result;
            if (collectionType.isInterface() || Modifier.isAbstract(collectionType.getModifiers()))
            {
                if (Set.class.isAssignableFrom(collectionType))
                {
                    if (SortedSet.class.isAssignableFrom(collectionType))
                    {
                        result = (S)new TreeSet();
                    }
                    else
                    {
                        result = (S)new HashSet();
                    }
                }
                else if (List.class.isAssignableFrom(collectionType))
                {
                    result = (S)new LinkedList();
                }
                else
                {
                    result = (S)new LinkedList(); // other collection
                }
            }
            else
            {
                result = collectionType.newInstance();
            }
            return result;
        }
        catch (IllegalAccessException ex)
        {
            throw new IllegalArgumentException("Collection-conversion failed: Could not access the default constructor of: " + ptype.getRawType(), ex);
        }
        catch (InstantiationException ex)
        {
            throw new IllegalArgumentException("Collection-conversion failed: Could not create an instance of: " + ptype.getRawType(), ex);
        }
    }
}
