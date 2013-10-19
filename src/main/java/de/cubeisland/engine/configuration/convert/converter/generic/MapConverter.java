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
import de.cubeisland.engine.configuration.convert.Convert;
import de.cubeisland.engine.configuration.node.MapNode;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.StringNode;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

public class MapConverter
{
    /**
     * Makes a map serializable for configs
     *
     * @param map the map to convert
     * @return the serializable map
     * @throws ConversionException
     */
    public MapNode toNode(Map<?, ?> map) throws ConversionException
    {
        MapNode result = MapNode.emptyMap();
        if (map.isEmpty())
        {
            return result;
        }
        for (Object key : map.keySet())
        {
            Node keyNode = Convert.toNode(key);
            if (keyNode instanceof StringNode)
            {
                result.setNode((StringNode)keyNode, Convert.toNode(map.get(key)));
            }
            else
            {
                result.setNode(StringNode.of(keyNode.asText()),Convert.toNode(map.get(key)));
            }
        }
        return result;
    }

    /**
     * Deserializes an object back to a map
     *
     * @param <K>     the KeyType
     * @param <V>     the ValueType
     * @param <S>     the MapType
     * @param ptype   the MapTypeClass
     * @param mapNode  the object to convert
     * @return the converted map
     * @throws ConversionException
     */
    @SuppressWarnings("unchecked")
    public <K, V, S extends Map<K, V>> S fromNode(ParameterizedType ptype, MapNode mapNode) throws ConversionException
    {
        try
        {
            if (ptype.getRawType() instanceof Class)
            {
                Type keyType = ptype.getActualTypeArguments()[0];
                Type valType = ptype.getActualTypeArguments()[1];
                S result = getMapFor(ptype);
                for (Map.Entry<String, Node> entry : mapNode.getMappedNodes().entrySet())
                {
                    StringNode keyNode = new StringNode(mapNode.getOriginalKey(entry.getKey())); // preserve Casing in Key
                    K newKey = Convert.fromNode(keyNode, keyType);
                    V newVal = Convert.fromNode(entry.getValue(), valType);
                    result.put(newKey, newVal);
                }
                return result;
            }
            throw new IllegalArgumentException("Unknown Map-Type: " + ptype);
        }
        catch (ConversionException ex)
        {
            throw new IllegalStateException("Map-conversion failed: Error while converting the values in the map.", ex);
        }
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
        catch (IllegalAccessException ex)
        {
            throw new IllegalArgumentException("Map-conversion failed: Could not access the default constructor of: " + ptype.getRawType(), ex);
        }
        catch (InstantiationException ex)
        {
            throw new IllegalArgumentException("Map-conversion failed: Could not create an instance of: " + ptype.getRawType(), ex);
        }
    }
}
