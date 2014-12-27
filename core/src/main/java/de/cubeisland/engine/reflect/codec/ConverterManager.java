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

import de.cubeisland.engine.reflect.Reflected;
import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.codec.converter.BooleanConverter;
import de.cubeisland.engine.reflect.codec.converter.ByteConverter;
import de.cubeisland.engine.reflect.codec.converter.ClassConverter;
import de.cubeisland.engine.reflect.codec.converter.Converter;
import de.cubeisland.engine.reflect.codec.converter.DateConverter;
import de.cubeisland.engine.reflect.codec.converter.DoubleConverter;
import de.cubeisland.engine.reflect.codec.converter.FloatConverter;
import de.cubeisland.engine.reflect.codec.converter.IntegerConverter;
import de.cubeisland.engine.reflect.codec.converter.LevelConverter;
import de.cubeisland.engine.reflect.codec.converter.LocaleConverter;
import de.cubeisland.engine.reflect.codec.converter.LongConverter;
import de.cubeisland.engine.reflect.codec.converter.SectionConverter;
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
import de.cubeisland.engine.reflect.util.SectionFactory;

import static java.util.Map.Entry;

/**
 * This Class manages all Converter for a CodecManager or a Codec
 */
public final class ConverterManager
{
    private Map<Class<?>, Converter> converters = new ConcurrentHashMap<Class<?>, Converter>();
    private Map<Class<? extends Converter>, Converter> annotationConverters = new ConcurrentHashMap<Class<? extends Converter>, Converter>();
    private MapConverter mapConverter;
    private ArrayConverter arrayConverter;
    private CollectionConverter collectionConverter;
    private ConverterManager fallbackManager;
    private Reflected reflected = null;
    private SectionConverter sectionConverter;

    private ConverterManager(ConverterManager defaultConverters)
    {
        this.fallbackManager = defaultConverters;
        this.mapConverter = new MapConverter();
        this.arrayConverter = new ArrayConverter();
        this.collectionConverter = new CollectionConverter();
    }

    public ConverterManager(Reflected reflected)
    {
        this((ConverterManager)null);
        this.reflected = reflected;
    }

    /**
     * Returns a ConverterManager with the default converters
     *
     * @return the manager
     */
    static ConverterManager defaultManager()
    {
        ConverterManager convert = new ConverterManager((ConverterManager)null);
        // Register Default Converters
        convert.registerDefaultConverters();
        return convert;
    }

    /**
     * Returns an empty ConverterManager using the given Manager as fallback
     *
     * @param fallback the fallback Manager
     *
     * @return the manager
     */
    static ConverterManager emptyManager(ConverterManager fallback)
    {
        return new ConverterManager(fallback);
    }

    public static ConverterManager reflectedManager(Reflected reflected)
    {
        return new ConverterManager(reflected);
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
        this.registerConverter(Class.class, new ClassConverter());

        this.sectionConverter = new SectionConverter();
    }

    /**
     * Registers a Converter for given Class
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
        annotationConverters.put(converter.getClass(), converter);
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
     * Matches a registered Converter
     *
     * @param clazz the class to match for
     *
     * @return a matching converter
     */
    @SuppressWarnings("unchecked")
    public final <T> Converter<T> matchConverter(Class<? extends T> clazz) throws ConverterNotFoundException
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
        if (converter != null)
        {
            return (Converter<T>)converter;
        }
        throw new ConverterNotFoundException("Converter not found for: " + clazz.getName());
    }

    private Converter getConverter(Class<?> clazz)
    {
        Converter converter = this.converters.get(clazz);
        if (converter == null && this.fallbackManager != null)
        {
            converter = this.fallbackManager.getConverter(clazz);
        }
        return converter;
    }
    public Converter getAnnotationConverter(Class<? extends Converter> clazz)
    {
        return this.annotationConverters.get(clazz);
    }

    private Converter findConverter(Class clazz)
    {
        for (Entry<Class<?>, Converter> entry : converters.entrySet())
        {
            if (entry.getKey().isAssignableFrom(clazz))
            {
                Converter converter = entry.getValue();
                registerConverter(clazz, converter);
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
    public final <T> Node convertToNode(T object) throws ConversionException
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
        else
        {
            try
            {
                return matchConverter(object.getClass()).toNode(object, this);
            }
            catch (ConverterNotFoundException e)
            {
                if (object instanceof Section)
                {
                    return getSectionConverter().toNode((Section)object, this);
                }
                throw e;
            }
        }
    }

    /**
     * Fills the section with the values from the Node
     *
     * @param node    the node
     * @param section the section
     */
    public final void convertFromNode(MapNode node, MapNode defaultNode, Section section) throws ConversionException
    {
        this.getSectionConverter().fromNode(section, node, defaultNode, this);
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
        if (type instanceof Class)
        {
            if (Section.class.isAssignableFrom((Class<?>)type))
            {
                Section section = SectionFactory.newSectionInstance((Class<? extends Section>)type, null);
                this.convertFromNode((MapNode)node, (MapNode)node, section);
                return (T)section;
            }
            if (((Class)type).isArray())
            {
                if (node instanceof ListNode)
                {
                    return (T)arrayConverter.fromNode((Class<T[]>)type, (ListNode)node, this);
                }
                else
                {
                    throw ConversionException.of(arrayConverter, node,
                                                 "Cannot convert to Array! Node is not a ListNode!");
                }
            }
            return matchConverter((Class<T>)type).fromNode(node, this);
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
                        return (T)collectionConverter.<Object, Collection>fromNode(ptype, (ListNode)node, this);
                    }
                    else
                    {
                        throw ConversionException.of(collectionConverter, node,
                                                     "Cannot convert to Collection! Node is not a ListNode!");
                    }
                }
                if (Map.class.isAssignableFrom((Class)ptype.getRawType()))
                {
                    if (node instanceof MapNode)
                    {
                        return (T)mapConverter.<Object, Object, Map>fromNode(ptype, (MapNode)node, this);
                    }
                    else
                    {
                        throw ConversionException.of(mapConverter, node,
                                                     "Cannot convert to Map! Node is not a MapNode!");
                    }
                }
                return matchConverter((Class<T>)ptype.getRawType()).fromNode(node, this);
            }
        }
        throw new IllegalArgumentException("Unknown Type: " + type);
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
     * Returns the Reflected owning this ConverterManager
     *
     * @return the Reflected or null if not owned
     */
    public Reflected getReflected()
    {
        return reflected;
    }

    /**
     * Returns the SectionConverter or null if not found
     *
     * @return the SectionConverter
     */
    public SectionConverter getSectionConverter()
    {
        if (this.sectionConverter == null)
        {
            return this.fallbackManager.getSectionConverter();
        }
        return this.sectionConverter;
    }
}
