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
package de.cubeisland.engine.reflect.codec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Date;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import de.cubeisland.engine.reflect.codec.converter.BooleanConverter;
import de.cubeisland.engine.reflect.codec.converter.ByteConverter;
import de.cubeisland.engine.reflect.codec.converter.Converter;
import de.cubeisland.engine.reflect.codec.converter.DateConverter;
import de.cubeisland.engine.reflect.codec.converter.DoubleConverter;
import de.cubeisland.engine.reflect.codec.converter.FloatConverter;
import de.cubeisland.engine.reflect.codec.converter.IntegerConverter;
import de.cubeisland.engine.reflect.codec.converter.LevelConverter;
import de.cubeisland.engine.reflect.codec.converter.LocaleConverter;
import de.cubeisland.engine.reflect.codec.converter.LongConverter;
import de.cubeisland.engine.reflect.codec.converter.ShortConverter;
import de.cubeisland.engine.reflect.codec.converter.StringConverter;
import de.cubeisland.engine.reflect.codec.converter.UUIDConverter;
import de.cubeisland.engine.reflect.codec.converter.generic.ArrayConverter;
import de.cubeisland.engine.reflect.codec.converter.generic.CollectionConverter;
import de.cubeisland.engine.reflect.codec.converter.generic.MapConverter;
import de.cubeisland.engine.reflect.exception.ConversionException;
import de.cubeisland.engine.reflect.exception.ConverterNotFoundException;
import de.cubeisland.engine.reflect.node.ListNode;
import de.cubeisland.engine.reflect.node.MapNode;
import de.cubeisland.engine.reflect.node.Node;
import de.cubeisland.engine.reflect.node.NullNode;

public final class ConverterManager
{
    private Map<Class, Converter> converters = new ConcurrentHashMap<Class, Converter>();
    private MapConverter mapConverter;
    private ArrayConverter arrayConverter;
    private CollectionConverter collectionConverter;
    private ConverterManager defaultConverters;

    private ConverterManager(ConverterManager defaultConverters)
    {
        this.defaultConverters = defaultConverters;
        this.mapConverter = new MapConverter();
        this.arrayConverter = new ArrayConverter();
        this.collectionConverter = new CollectionConverter();
    }

    static ConverterManager defaultManager()
    {
        // Register Default Converters
        ConverterManager convert = new ConverterManager(null);
        convert.registerDefaultConverters();
        return convert;
    }

    static ConverterManager emptyManager(ConverterManager defaultConverters)
    {
        return new ConverterManager(defaultConverters);
    }

    private void registerDefaultConverters()
    {
        Converter<?> converter = new IntegerConverter();
        this.registerConverter(Integer.class, converter);
        this.registerConverter(int.class, converter);
        converter = new ShortConverter();
        this.registerConverter(Short.class, converter);
        this.registerConverter(short.class, converter);
        converter = new ByteConverter();
        this.registerConverter(Byte.class, converter);
        this.registerConverter(byte.class, converter);
        converter = new DoubleConverter();
        this.registerConverter(Double.class, converter);
        this.registerConverter(double.class, converter);
        converter = new FloatConverter();
        this.registerConverter(Float.class, converter);
        this.registerConverter(float.class, converter);
        converter = new LongConverter();
        this.registerConverter(Long.class, converter);
        this.registerConverter(long.class, converter);
        converter = new BooleanConverter();
        this.registerConverter(Boolean.class, converter);
        this.registerConverter(boolean.class, converter);
        this.registerConverter(String.class, new StringConverter());
        this.registerConverter(Date.class, new DateConverter());
        this.registerConverter(UUID.class, new UUIDConverter());
        this.registerConverter(Locale.class, new LocaleConverter());
        this.registerConverter(Level.class, new LevelConverter());
    }

    /**
     * registers a converter to check for when converting
     *
     * @param clazz     the class
     * @param converter the converter
     */
    public final void registerConverter(Class clazz, Converter converter)
    {
        if (clazz == null || converter == null)
        {
            return;
        }
        converters.put(clazz, converter);
    }

    /**
     * Removes a converter from this manager
     *
     * @param clazz the class of the converter to remove
     */
    public final void removeConverter(Class clazz)
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

    /**
     * Removes all registered converters
     */
    public final void removeConverters()
    {
        converters.clear();
    }

    /**
     * Searches matching Converter
     *
     * @param objectClass the class to search for
     *
     * @return a matching converter or null if not found
     */
    @SuppressWarnings("unchecked")
    public final <T> Converter<T> matchConverter(Class<? extends T> objectClass) throws ConverterNotFoundException
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
         || Map.class.isAssignableFrom(objectClass))
        {
            return null;
        }
        throw new ConverterNotFoundException("Converter not found for: " + objectClass.getName());
    }


    /**
     * Converts a convertible Object into a Node
     *
     * @param object the Object
     *
     * @return the serialized Node
     */
    public final <T> Node convertToNode(T object) throws ConversionException
    {
        try
        {
            return this.convertToNode0(object);
        }
        catch (ConverterNotFoundException e)
        {
            if (this.defaultConverters == null)
            {
                throw e;
            }
            return this.defaultConverters.convertToNode(object);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Node convertToNode0(T object) throws ConversionException
    {
        if (object == null)
        {
            return NullNode.emptyNode();
        }
        if (object.getClass().isArray())
        {
            return arrayConverter.toNode((Object[])object, this);
        }
        else if (object instanceof Collection)
        {
            return collectionConverter.toNode((Collection)object, this);
        }
        else if (object instanceof Map)
        {
            return mapConverter.toNode((Map)object, this);
        }
        Converter<T> converter = (Converter<T>)matchConverter(object.getClass());
        return converter.toNode(object, this);
    }

    /**
     * Converts a Node back into the original Object
     *
     * @param node the node
     * @param type the type of the object
     *
     * @return the original object
     */
    public final <T> T convertFromNode(Node node, Type type) throws ConversionException
    {
        try
        {
            return this.convertFromNode0(node, type);
        }
        catch (ConverterNotFoundException e)
        {
            if (this.defaultConverters == null)
            {
                throw e;
            } // else ignore
            return this.defaultConverters.convertFromNode0(node, type);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T convertFromNode0(Node node, Type type) throws ConversionException
    {
        if (node == null || node instanceof NullNode || type == null)
        {
            return null;
        }
        if (type instanceof Class)
        {
            if (((Class)type).isArray())
            {
                if (node instanceof ListNode)
                {
                    return (T)arrayConverter.fromNode((Class<T[]>)type, (ListNode)node, this);
                }
                else
                {
                    throw ConversionException.of(arrayConverter, node, "Cannot convert to Array! Node is not a ListNode!");
                }
            }
            Converter<T> converter = matchConverter((Class<T>)type);
            return converter.fromNode(node, this);
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
                        return (T)collectionConverter.fromNode(ptype, (ListNode)node, this);
                    }
                    else
                    {
                        throw ConversionException.of(collectionConverter, node, "Cannot convert to Collection! Node is not a ListNode!");
                    }
                }
                if (Map.class.isAssignableFrom((Class)ptype.getRawType()))
                {
                    if (node instanceof MapNode)
                    {
                        return (T)mapConverter.fromNode(ptype, (MapNode)node, this);
                    }
                    else
                    {
                        throw ConversionException.of(mapConverter, node, "Cannot convert to Map! Node is not a MapNode!");
                    }
                }
            }
        }
        throw new IllegalArgumentException("Unknown Type: " + type);
    }
}
