/*
 * The MIT License
 * Copyright © 2013 Cube Island
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.converter.node.MapNode;
import org.cubeengine.converter.node.Node;
import org.cubeengine.converter.node.StringNode;

/**
 * Converts MapNode to Map and vice versa
 */
public class MapConverter implements GenericConverter<Map>
{
    /**
     * Returns a new map of given {@link ParameterizedType}
     *
     * @param pType the type
     *
     * @throws IllegalAccessException when reflection fails
     * @throws InstantiationException when reflection fails
     * @return the map of given type
     */
    public static Map getMapFor(ParameterizedType pType) throws IllegalAccessException, InstantiationException
    {
        Class mapType = (Class)pType.getRawType();
        if (mapType.isInterface() || Modifier.isAbstract(mapType.getModifiers()))
        {
            return new LinkedHashMap();
        }
        else
        {
            return (Map)mapType.newInstance();
        }
    }

    public Node toNode(Map map, ConverterManager manager) throws ConversionException
    {
        MapNode result = MapNode.emptyMap();
        if (map == null || map.isEmpty())
        {
            return result;
        }

        @SuppressWarnings("unchecked")
        Set<Entry> entrySet = map.entrySet();
        for (Entry entry : entrySet)
        {
            Node keyNode = manager.convertToNode(entry.getKey());
            result.set(keyNode.asText(), manager.convertToNode(entry.getValue()));
        }
        return result;
    }

    public Map fromNode(Node node, ParameterizedType ptype, ConverterManager manager) throws ConversionException
    {
        if (!(node instanceof MapNode))
        {
            throw ConversionException.of(this, node, "Cannot convert to Map! Node is not a MapNode!");
        }
        if (!(ptype.getRawType() instanceof Class))
        {
            throw new IllegalArgumentException("Unknown Map-Type: " + ptype);
        }
        try
        {
            return fillMap(getMapFor(ptype), ptype, (MapNode)node, manager);
        }
        catch (IllegalAccessException e)
        {
            throw ConversionException.of(this, node, "Could not create Map", e);
        }
        catch (InstantiationException e)
        {
            throw ConversionException.of(this, node, "Could not create Map", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map fillMap(Map result, ParameterizedType pType, MapNode mapNode,
                        ConverterManager manager) throws ConversionException
    {
        Type keyType = pType.getActualTypeArguments()[0];
        Type valType = pType.getActualTypeArguments()[1];

        for (Entry<String, Node> entry : mapNode.getMappedNodes().entrySet())
        {
            // preserve Casing in Key
            StringNode keyNode = new StringNode(mapNode.getOriginalKey(entry.getKey()));
            Object newKey = manager.convertFromNode(keyNode, keyType);
            Object newVal = manager.convertFromNode(entry.getValue(), valType);
            result.put(newKey, newVal);
        }
        return result;
    }
}
