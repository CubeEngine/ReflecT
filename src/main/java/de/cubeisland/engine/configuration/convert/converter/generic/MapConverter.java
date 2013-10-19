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
