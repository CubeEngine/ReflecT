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
package de.cubeisland.engine.configuration.convert;

import de.cubeisland.engine.configuration.convert.converter.*;
import de.cubeisland.engine.configuration.convert.converter.generic.ArrayConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.CollectionConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.MapConverter;
import de.cubeisland.engine.configuration.node.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Date;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class provides the converters.
 */
public class Convert
{
    private static Map<Class, Converter> converters;
    private static MapConverter mapConverter;
    private static ArrayConverter arrayConverter;
    private static CollectionConverter collectionConverter;

    public static void init()
    {
        converters = new ConcurrentHashMap<>();
        mapConverter = new MapConverter();
        arrayConverter = new ArrayConverter();
        collectionConverter = new CollectionConverter();

        Converter<?> converter;
        registerConverter(Integer.class, converter = new IntegerConverter());
        registerConverter(int.class, converter);
        registerConverter(Short.class, converter = new ShortConverter());
        registerConverter(short.class, converter);
        registerConverter(Byte.class, converter = new ByteConverter());
        registerConverter(byte.class, converter);
        registerConverter(Double.class, converter = new DoubleConverter());
        registerConverter(double.class, converter);
        registerConverter(Float.class, converter = new FloatConverter());
        registerConverter(float.class, converter);
        registerConverter(Long.class, converter = new LongConverter());
        registerConverter(long.class, converter);
        registerConverter(Boolean.class, converter = new BooleanConverter());
        registerConverter(boolean.class, converter);
        registerConverter(String.class, new StringConverter());
        registerConverter(Date.class, new DateConverter());
    }

    public synchronized static void cleanup()
    {
        removeConverters();
        converters = null;
        mapConverter = null;
        arrayConverter = null;
        collectionConverter = null;
    }

    /**
     * registers a converter to check for when converting
     *
     * @param clazz the class
     * @param converter the converter
     */
    public static void registerConverter(Class clazz, Converter converter)
    {
        if (clazz == null || converter == null)
        {
            return;
        }
        converters.put(clazz, converter);
    }

    public static void removeConverter(Class clazz)
    {
        Iterator<Map.Entry<Class, Converter>> iter = converters.entrySet().iterator();

        Map.Entry<Class, Converter> entry;
        while (iter.hasNext())
        {
            entry = iter.next();
            if (entry.getKey() == clazz || entry.getValue().getClass() == clazz)
            {
                iter.remove();
            }
        }
    }

    public static void removeConverters()
    {
        converters.clear();
    }

    /**
     * Searches matching Converter
     *
     * @param objectClass the class to search for
     * @return a matching converter or null if not found
     */
    @SuppressWarnings("unchecked")
    public static <T> Converter<T> matchConverter(Class<? extends T> objectClass)
    {
        if (objectClass == null)
        {
            return null;
        }
        Converter converter = converters.get(objectClass);
        if (converter == null)
        {
            for (Map.Entry<Class, Converter> entry : converters.entrySet())
            {
                if (entry.getKey().isAssignableFrom(objectClass))
                {
                    registerConverter(objectClass, converter = entry.getValue());
                    break;
                }
            }
        }
        if (converter != null)
        {
            return (Converter<T>)converter;
        }
        if (objectClass.isArray() || Collection.class.isAssignableFrom(objectClass) || Map.class.isAssignableFrom(objectClass))
        {
            return null;
        }
        throw new ConverterNotFoundException("Converter not found for: " + objectClass.getName());
    }

    /**
     * Wraps a serialized Object into a Node
     *
     * @param o a serialized Object
     * @return the Node
     */
    public static Node wrapIntoNode(Object o)
    {
        if (o == null)
        {
            return NullNode.emptyNode();
        }
        if (o instanceof Map)
        {
            return new MapNode((Map)o);
        }
        if (o instanceof Collection)
        {
            return new ListNode((List)o);
        }
        if (o.getClass().isArray())
        {
            return new ListNode((Object[])o);
        }
        if (o instanceof String)
        {
            return new StringNode((String)o);
        }
        if (o instanceof Byte || o.getClass() == byte.class)
        {
            return new ByteNode((byte)o);
        }
        if (o instanceof Short || o.getClass() == short.class)
        {
            return new ShortNode((short)o);
        }
        if (o instanceof Integer || o.getClass() == int.class)
        {
            return new IntNode((int)o);
        }
        if (o instanceof Long || o.getClass() == long.class)
        {
            return new LongNode((long)o);
        }
        if (o instanceof Float || o.getClass() == float.class)
        {
            return new FloatNode((float)o);
        }
        if (o instanceof Double || o.getClass() == double.class)
        {
            return new DoubleNode((double)o);
        }
        if (o instanceof Boolean || o.getClass() == boolean.class)
        {
            return BooleanNode.of((boolean)o);
        }
        if (o instanceof Character || o.getClass() == char.class)
        {
            return new CharNode((char)o);
        }
        throw new IllegalArgumentException("Cannot wrap into Node: " + o.getClass());

    }

    /**
     * Converts a convertible Object into a Node
     *
     * @param object the Object
     * @return the serialized Node
     */
    public static <T> Node toNode(T object) throws ConversionException
    {
        if (object == null)
        {
            return null;
        }
        if (object.getClass().isArray())
        {
            return arrayConverter.toNode((Object[])object);
        }
        else if (object instanceof Collection)
        {
            return collectionConverter.toNode((Collection)object);
        }
        else if (object instanceof Map)
        {
            return mapConverter.toNode((Map)object);
        }
        Converter<T> converter = (Converter<T>)matchConverter(object.getClass());
        return converter.toNode(object);
    }

    /**
     * Converts a Node back into the original Object
     *
     * @param node the node
     * @param type the type of the object
     * @return
     */
    public static <T> T fromNode(Node node, Type type) throws ConversionException
    {
        if (node == null || node instanceof NullNode || type == null)
            return null;
        if (type instanceof Class)
        {
            if (((Class)type).isArray())
            {
                if (node instanceof ListNode)
                {
                    return (T)arrayConverter.fromNode((Class<T[]>)type, (ListNode)node);
                }
                else
                {
                    throw new ConversionException("Cannot convert to Array! Node is not a ListNode!");
                }
            }
            else
            {
                Converter<T> converter = matchConverter((Class<T>)type);
                return converter.fromNode(node);
            }
        }
        else if (type instanceof ParameterizedType)
        {
            ParameterizedType ptype = (ParameterizedType)type;
            if (ptype.getRawType() instanceof Class)
            {
                if (Collection.class.isAssignableFrom((Class)ptype.getRawType()))
                {
                    if (node instanceof ListNode)
                    {
                        return (T)collectionConverter.fromNode(ptype, (ListNode)node);
                    }
                    else
                    {
                        throw new ConversionException("Cannot convert to Collection! Node is not a ListNode!");
                    }

                }
                else if (Map.class.isAssignableFrom((Class)ptype.getRawType()))
                {
                    if (node instanceof MapNode)
                    {
                        return (T)mapConverter.fromNode(ptype, (MapNode)node);
                    }
                    else
                    {
                        throw new ConversionException("Cannot convert to Map! Node is not a MapNode!");
                    }

                }
            }
        }
        throw new IllegalArgumentException("Unknown Type: " + type);
    }
}
