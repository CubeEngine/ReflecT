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

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import de.cubeisland.engine.reflect.codec.ConverterManager;
import de.cubeisland.engine.reflect.exception.ReflectedInstantiationException;
import de.cubeisland.engine.reflect.exception.ConversionException;
import de.cubeisland.engine.reflect.node.MapNode;
import de.cubeisland.engine.reflect.node.Node;
import de.cubeisland.engine.reflect.node.StringNode;

public class MapConverter
{
    /**
     * Makes a map serializable for reflected
     *
     * @param map the map to convert
     *
     * @return the serializable map
     */
    public MapNode toNode(Map<?, ?> map, ConverterManager manager) throws ConversionException
    {
        MapNode result = MapNode.emptyMap();
        if (map == null || map.isEmpty())
        {
            return result;
        }
        for (Entry entry : map.entrySet())
        {
            Node keyNode = manager.convertToNode(entry.getKey());
            if (keyNode instanceof StringNode)
            {
                result.setNode((StringNode)keyNode, manager.convertToNode(entry.getValue()));
            }
            else
            {
                result.setNode(StringNode.of(keyNode.asText()), manager.convertToNode(entry.getValue()));
            }
        }
        return result;
    }

    /**
     * Deserializes an object back to a map
     *
     * @param ptype   the MapTypeClass
     * @param mapNode the object to convert
     *
     * @return the converted map
     */
    @SuppressWarnings("unchecked")
    public <K, V, S extends Map<K, V>> S fromNode(ParameterizedType ptype, MapNode mapNode, ConverterManager manager) throws ConversionException
    {
        if (ptype.getRawType() instanceof Class)
        {
            Type keyType = ptype.getActualTypeArguments()[0];
            Type valType = ptype.getActualTypeArguments()[1];
            S result = getMapFor(ptype);
            for (Map.Entry<String, Node> entry : mapNode.getMappedNodes().entrySet())
            {
                // preserve Casing in Key
                StringNode keyNode = new StringNode(mapNode.getOriginalKey(entry.getKey()));
                K newKey = manager.convertFromNode(keyNode, keyType);
                V newVal = manager.convertFromNode(entry.getValue(), valType);
                result.put(newKey, newVal);
            }
            return result;
        }
        throw new IllegalArgumentException("Unknown Map-Type: " + ptype);
    }

    @SuppressWarnings("unchecked")
    public static <S extends Map> S getMapFor(ParameterizedType ptype)
    {
        try
        {
            Class<S> mapType = (Class<S>)ptype.getRawType();
            if (mapType.isInterface() || Modifier.isAbstract(mapType.getModifiers()))
            {
                return (S)new LinkedHashMap();
            }
            else
            {
                return mapType.newInstance();
            }
        }
        catch (InstantiationException e)
        {
            throw new ReflectedInstantiationException((Class)ptype.getRawType(), e);
        }
        catch (IllegalAccessException e)
        {
            throw new ReflectedInstantiationException((Class)ptype.getRawType(), e);
        }
    }
}
