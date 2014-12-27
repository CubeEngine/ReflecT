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
package de.cubeisland.engine.converter;

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

import de.cubeisland.engine.converter.converter.BooleanConverter;
import de.cubeisland.engine.converter.converter.ByteConverter;
import de.cubeisland.engine.converter.converter.ClassConverter;
import de.cubeisland.engine.converter.converter.Converter;
import de.cubeisland.engine.converter.converter.EnumConverter;
import de.cubeisland.engine.converter.converter.ClassedConverter;
import de.cubeisland.engine.converter.converter.DateConverter;
import de.cubeisland.engine.converter.converter.DoubleConverter;
import de.cubeisland.engine.converter.converter.FloatConverter;
import de.cubeisland.engine.converter.converter.IntegerConverter;
import de.cubeisland.engine.converter.converter.LevelConverter;
import de.cubeisland.engine.converter.converter.LocaleConverter;
import de.cubeisland.engine.converter.converter.LongConverter;
import de.cubeisland.engine.converter.converter.ShortConverter;
import de.cubeisland.engine.converter.converter.StringConverter;
import de.cubeisland.engine.converter.converter.UUIDConverter;
import de.cubeisland.engine.converter.converter.generic.ArrayConverter;
import de.cubeisland.engine.converter.converter.generic.CollectionConverter;
import de.cubeisland.engine.converter.converter.generic.GenericConverter;
import de.cubeisland.engine.converter.converter.generic.MapConverter;
import de.cubeisland.engine.converter.node.Node;
import de.cubeisland.engine.converter.node.NullNode;

import static java.util.Map.Entry;

/**
 * This Class manages all Converter for a CodecManager or a Codec
 */
public class ConverterManager
{
    private ConverterManager fallbackManager;

    private Map<Class<?>, Converter> converters = new ConcurrentHashMap<Class<?>, Converter>();
    private Map<Class, Converter> convertersByClass = new ConcurrentHashMap<Class, Converter>();

    protected ConverterManager(ConverterManager fallbackManager)
    {
        this.fallbackManager = fallbackManager;
    }

    /**
     * Returns a ConverterManager with the default converters
     *
     * @return the manager
     */
    public static ConverterManager defaultManager()
    {
        ConverterManager convert = new ConverterManager(null);
        // Register Default Converters
        convert.registerDefaultConverters();
        return convert;
    }

    /**
     * Returns a new ConverterManager using this manager as fallback
     *
     * @return the new ConverterManager
     */
    public final ConverterManager subManager()
    {
        return new ConverterManager(this);
    }

    private void registerDefaultConverters()
    {
        ClassedConverter converter = new IntegerConverter();
        this.registerConverter(converter, Integer.class);
        this.registerConverter(converter, int.class);
        converter = new ShortConverter();
        this.registerConverter(converter, Short.class);
        this.registerConverter(converter, short.class);
        converter = new ByteConverter();
        this.registerConverter(converter, Byte.class);
        this.registerConverter(converter, byte.class);
        converter = new DoubleConverter();
        this.registerConverter(converter, Double.class);
        this.registerConverter(converter, double.class);
        converter = new FloatConverter();
        this.registerConverter(converter, Float.class);
        this.registerConverter(converter, float.class);
        converter = new LongConverter();
        this.registerConverter(converter, Long.class);
        this.registerConverter(converter, long.class);
        converter = new BooleanConverter();
        this.registerConverter(converter, Boolean.class);
        this.registerConverter(converter, boolean.class);
        this.registerConverter(new StringConverter(), String.class);
        this.registerConverter(new DateConverter(), Date.class);
        this.registerConverter(new UUIDConverter(), UUID.class);
        this.registerConverter(new LocaleConverter(), Locale.class);
        this.registerConverter(new LevelConverter(), Level.class);
        this.registerConverter(new ClassConverter(), Class.class);
        this.registerConverter(new EnumConverter(), Enum.class);

        // Generic Converters:
        this.registerConverter(new MapConverter(), Map.class);
        this.registerConverter(new CollectionConverter(), Collection.class);
        this.registerConverter(new ArrayConverter());
    }

    /**
     * Registers a Converter for given Class
     *
     * @param converter the converter
     * @param classes   the class
     */
    public final void registerConverter(Converter converter, Class... classes)
    {
        if (classes == null || converter == null)
        {
            return;
        }
        for (Class aClass : classes)
        {
            converters.put(aClass, converter);
        }
        convertersByClass.put(converter.getClass(), converter);
    }

    /**
     * Removes a Converter from this manager
     *
     * @param clazz the class of the converter to remove
     */
    public final void removeConverter(Class clazz)
    {
        Iterator<Entry<Class<?>, Converter>> iter = converters.entrySet().iterator();
        Entry<Class<?>, Converter> entry;
        while (iter.hasNext())
        {
            entry = iter.next();
            if (entry.getKey() == clazz || entry.getValue().getClass() == clazz)
            {
                iter.remove();
                convertersByClass.remove(entry.getValue().getClass());
            }
        }
    }

    /**
     * Removes all registered converters
     */
    public final void removeConverters()
    {
        converters.clear();
        convertersByClass.clear();
    }

    /**
     * Matches a registered Converter
     *
     * @param clazz the class to match for
     *
     * @return a matching converter
     */
    @SuppressWarnings("unchecked")
    public final Converter matchConverter(Class clazz) throws ConverterNotFoundException
    {
        if (clazz == null)
        {
            return null;
        }
        Converter converter = this.getConverter(clazz);
        if (converter == null)
        {
            converter = this.findConverter(clazz);
        }
        if (converter == null)
        {
            throw new ConverterNotFoundException("Converter not found for: " + clazz.getName());
        }
        return converter;
    }

    private Converter getConverter(Class clazz)
    {
        Converter converter = this.converters.get(clazz);
        if (converter == null && this.fallbackManager != null)
        {
            converter = this.fallbackManager.getConverter(clazz);
        }
        return converter;
    }

    private Converter findConverter(Class clazz)
    {
        for (Entry<Class<?>, Converter> entry : converters.entrySet())
        {
            if (entry.getKey().isAssignableFrom(clazz))
            {
                Converter converter = entry.getValue();
                registerConverter(converter, clazz);
                return converter;
            }
        }
        if (this.fallbackManager != null)
        {
            return this.fallbackManager.findConverter(clazz);
        }
        return null;
    }

    /**
     * Converts a convertible Object into a Node
     *
     * @param object the Object
     *
     * @return the serialized Node
     */
    @SuppressWarnings("unchecked")
    public final <T> Node convertToNode(T object) throws ConversionException
    {
        if (object == null)
        {
            return NullNode.emptyNode();
        }
        try
        {
            return matchConverter(object.getClass()).toNode(object, this);
        }
        catch (ConverterNotFoundException e)
        {
            Node node = toNode(object);
            if (node != null)
            {
                return node;
            }
            throw e;
        }
    }

    /**
     * When no converter was found directly this method is called in order to try to convert the object anyways.
     * Primarily used to convert Arrays.
     *
     * @param object the object to convert
     *
     * @return the converted object or null if not converted
     */
    protected <T> Node toNode(T object) throws ConversionException
    {
        if (object.getClass().isArray())
        {
            return getConverterByClass(ArrayConverter.class).toNode(object, this);
        }
        return null;
    }

    /**
     * Converts a Node into an Object of given Type
     *
     * @param node the node
     * @param type the type of the object
     *
     * @return the converted Node
     */
    @SuppressWarnings("unchecked")
    public final <T> T convertFromNode(Node node, Type type) throws ConversionException
    {
        if (node == null || node instanceof NullNode || type == null)
        {
            return null;
        }
        try
        {
            Class<T> clazz;
            if (type instanceof ParameterizedType)
            {
                clazz = (Class<T>)((ParameterizedType)type).getRawType();
            }
            else
            {
                clazz = (Class<T>)type;
            }
            Converter converter = matchConverter(clazz);
            if (type instanceof ParameterizedType && !(converter instanceof GenericConverter))
            {
                type = ((ParameterizedType)type).getRawType();
            }
            return (T)converter.fromNode(node, type, this);
        }
        catch (ConverterNotFoundException e)
        {
            return (T)fromNode(node, type);
        }
    }

    /**
     * When no converter was found directly this method is called in order to try to convert the object anyways.
     * Primarily used to convert Arrays.
     *
     * @param node the node to convert
     * @param type the type to convert to
     *
     * @return the converted node or null if not converted
     */
    protected Object fromNode(Node node, Type type) throws ConversionException
    {
        if (type instanceof Class)
        {
            if (((Class)type).isArray())
            {
                return getConverterByClass(ArrayConverter.class).fromNode(node, (Class)type, this);
            }
        }
        return null;
    }

    /**
     * Changes the fallback ConverterManager of this converter
     *
     * @param defaultManager the ConverterManager to fallback to
     *
     * @return fluent interface
     */
    public ConverterManager withFallback(ConverterManager defaultManager)
    {
        this.fallbackManager = defaultManager;
        return this;
    }

    /**
     * Returns the converter of given class
     *
     * @param clazz        the class
     * @param <ConverterT> the converter Type
     *
     * @return the converter for given class or null if not registered or found
     */
    @SuppressWarnings("unchecked")
    public final <ConverterT> ConverterT getConverterByClass(Class<ConverterT> clazz)
    {
        ConverterT converter = (ConverterT)this.convertersByClass.get(clazz);
        if (converter == null && fallbackManager != null)
        {
            return fallbackManager.getConverterByClass(clazz);
        }
        return converter;
    }
}
