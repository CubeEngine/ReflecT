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
package de.cubeisland.engine.configuration;

import de.cubeisland.engine.configuration.codec.ConfigurationCodec;
import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.convert.Converter;
import de.cubeisland.engine.configuration.convert.ConverterNotFoundException;
import de.cubeisland.engine.configuration.convert.converter.BooleanConverter;
import de.cubeisland.engine.configuration.convert.converter.ByteConverter;
import de.cubeisland.engine.configuration.convert.converter.DateConverter;
import de.cubeisland.engine.configuration.convert.converter.DoubleConverter;
import de.cubeisland.engine.configuration.convert.converter.FloatConverter;
import de.cubeisland.engine.configuration.convert.converter.IntegerConverter;
import de.cubeisland.engine.configuration.convert.converter.LongConverter;
import de.cubeisland.engine.configuration.convert.converter.ShortConverter;
import de.cubeisland.engine.configuration.convert.converter.StringConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.ArrayConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.CollectionConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.MapConverter;
import de.cubeisland.engine.configuration.node.BooleanNode;
import de.cubeisland.engine.configuration.node.ByteNode;
import de.cubeisland.engine.configuration.node.CharNode;
import de.cubeisland.engine.configuration.node.DoubleNode;
import de.cubeisland.engine.configuration.node.ErrorNode;
import de.cubeisland.engine.configuration.node.FloatNode;
import de.cubeisland.engine.configuration.node.IntNode;
import de.cubeisland.engine.configuration.node.ListNode;
import de.cubeisland.engine.configuration.node.LongNode;
import de.cubeisland.engine.configuration.node.MapNode;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.NullNode;
import de.cubeisland.engine.configuration.node.ShortNode;
import de.cubeisland.engine.configuration.node.StringNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Date;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * This abstract class represents a configuration.
 */
public abstract class Configuration<Codec extends ConfigurationCodec> implements Section
{
    protected static Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    public static void setLogger(Logger logger)
    {
        LOGGER = logger;
    }

    private final Codec codec;
    private File file;

    public Configuration()
    {
        this.codec = getCodec(this.getClass());
    }

    /**
     * Tries to get the Codec of a configuration implementation.
     *
     * @param clazz    the clazz of the configuration
     * @param <C>      the CodecType
     * @param <Config> the ConfigType
     *
     * @return the Codec
     *
     * @throws InvalidConfigurationException if no Codec was defined through the GenericType
     */
    @SuppressWarnings("unchecked")
    private static <C extends ConfigurationCodec, Config extends Configuration> C getCodec(Class<Config> clazz)
    {
        Type genericSuperclass = clazz.getGenericSuperclass(); // Get generic superclass
        Class<C> codecClass = null;
        try
        {
            if (genericSuperclass.equals(Configuration.class))
            {
                // superclass is this class -> No Codec set as GenericType
                throw new InvalidConfigurationException("Configuration has no Codec set! A configuration needs to have a coded defined in its GenericType");
            }
            else if (genericSuperclass instanceof ParameterizedType) // check if genericsuperclass is ParamereizedType
            {
                codecClass = (Class<C>)((ParameterizedType)genericSuperclass).getActualTypeArguments()[0]; // Get Type
                return codecClass.newInstance(); // Create Instance
            }
            // else lookup next superclass
        }
        catch (ClassCastException e) // Somehow the configuration has a GenericType that is not a Codec
        {
            if (!(genericSuperclass instanceof Class))
            {
                throw new IllegalStateException("Something went wrong!", e);
            }
        }
        catch (Exception ex)
        {
            throw new InvalidConfigurationException("Could not instantiate the Codec! " + codecClass, ex);
        }
        return getCodec((Class<? extends Configuration>)genericSuperclass); // Lookup next superclass
    }

    /**
     * Saves this configuration in a file for given Path
     *
     * @param target the Path to the file to save into
     */
    public void save(File target)
    {
        if (target == null)
        {
            throw new IllegalArgumentException("A configuration cannot be saved without a valid file!");
        }
        this.codec.save(this, target);
        this.onSaved(target);
    }

    /**
     * Reloads the configuration from file
     * <p>This will only work if the file of the configuration got set previously (usually through loading from file)
     */
    public final void reload()
    {
        this.reload(false);
    }

    /**
     * Reloads the configuration from file
     * <p>This will only work if the file of the configuration got set previously (usually through loading from file)
     *
     * @param save true if the configuration should be saved after loading
     *
     * @return true when a new file got created while saving
     *
     * @throws InvalidConfigurationException if an error occurs while loading
     */
    public boolean reload(boolean save) throws InvalidConfigurationException
    {
        if (this.file == null)
        {
            throw new IllegalArgumentException("The file must not be null in order to load the configuration!");
        }
        boolean result = false;
        try
        {
            InputStream is = new FileInputStream(this.file);
            try
            {
                this.loadFrom(is);
            }
            catch (RuntimeException e)
            {
                throw new InvalidConfigurationException("Could not load configuration from file!", e);
            }
            finally
            {
                try
                {
                    is.close();
                }
                catch (IOException ignored)
                {}
            }
            if (save)
            {
                this.save();
            }
        }
        catch (FileNotFoundException e)
        {
            if (save)
            {
                result = true;
            }
            else
            {
                throw new InvalidConfigurationException("Could not load configuration from file!", e);
            }
        }
        return result;
    }

    /**
     * Loads the configuration using the given InputStream
     *
     * @param is the InputStream to load from
     */
    public void loadFrom(InputStream is)
    {
        assert is != null : "You hae to provide a InputStream to load from";
        this.showLoadErrors(this.codec.load(this, is));//load config in maps -> updates -> sets fields
        this.onLoaded(file);
    }

    protected void showLoadErrors(Collection<ErrorNode> errors)
    {
        if (!errors.isEmpty())
        {
            LOGGER.warning(errors.size() + " ErrorNodes were encountered while loading the configuration!");
            for (ErrorNode error : errors)
            {
                LOGGER.warning(error.getErrorMessage());
            }
        }
    }

    /**
     * Saves the configuration to the set file.
     */
    public final void save()
    {
        this.save(this.file);
    }

    /**
     * Returns the Codec
     *
     * @return the ConfigurationCodec defined in the GenericType of the Configuration
     */
    public final Codec getCodec()
    {
        return this.codec;
    }

    /**
     * Sets the path to load from
     *
     * @param file the path the configuration will load from
     */
    public final void setFile(File file)
    {
        assert file != null : "The file must not be null!";
        this.file = file;
    }

    /**
     * Returns the path this config will be saved to and loaded from by default
     *
     * @return the path of this config
     */
    public final File getFile()
    {
        return this.file;
    }

    /**
     * This method gets called right after the configuration got loaded.
     */
    public void onLoaded(File loadedFrom)
    {}

    /**
     * This method gets called right after the configuration get saved.
     */
    public void onSaved(File savedTo)
    {}

    /**
     * Returns the lines to be added in front of the Configuration.
     * <p>not every Codec may use this
     *
     * @return the head
     */
    public String[] head()
    {
        return null;
    }

    /**
     * Returns the lines to be added at the end of the Configuration.
     * <p>not every Codec may use this
     *
     * @return the head
     */
    public String[] tail()
    {
        return null;
    }

    /**
     * Creates an instance of this given configuration-class.
     * <p>The configuration has to have the default Constructor for this to work!
     *
     * @param clazz the configurations class
     * @param <T>   The Type of the returned configuration
     *
     * @return the created configuration
     */
    public static <T extends Configuration> T create(Class<T> clazz)
    {
        try
        {
            return clazz.newInstance();
        }
        catch (Exception e)
        {
            throw new InvalidConfigurationException("Failed to create an instance of " + clazz.getName(), e);
        }
    }

    /**
     * Loads the configuration from given file and optionally saves it afterwards
     *
     * @param clazz the configurations class
     * @param file  the file to load from and save to
     * @param save  whether to save the configuration or not
     *
     * @return the loaded Configuration
     */
    public static <T extends Configuration> T load(Class<T> clazz, File file, boolean save)
    {
        T config = create(clazz); // loading
        config.setFile(file); // IMPORTANT TO SET BEFORE LOADING!
        config.reload(save);
        return config;
    }

    /**
     * Loads the configuration from given file and saves it afterwards
     *
     * @param clazz the configurations class
     * @param file  the file to load from and save to
     *
     * @return the loaded Configuration
     */
    public static <T extends Configuration> T load(Class<T> clazz, File file)
    {
        return load(clazz, file, true);
    }

    /**
     * Loads the configuration from the InputStream
     *
     * @param clazz the configurations class
     * @param is    the InputStream to load from
     *
     * @return the loaded configuration
     */
    public static <T extends Configuration> T load(Class<T> clazz, InputStream is)
    {
        T config = create(clazz);
        config.loadFrom(is);
        return config;
    }


    // -------------- CONVERTER METHODS ------------

    private static Map<Class, Converter> converters;
    private static MapConverter mapConverter;
    private static ArrayConverter arrayConverter;
    private static CollectionConverter collectionConverter;

    static
    {
        converters = new ConcurrentHashMap<Class, Converter>();
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

    /**
     * registers a converter to check for when converting
     *
     * @param clazz     the class
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
     *
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
     *
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
            return new ByteNode((Byte)o);
        }
        if (o instanceof Short || o.getClass() == short.class)
        {
            return new ShortNode((Short)o);
        }
        if (o instanceof Integer || o.getClass() == int.class)
        {
            return new IntNode((Integer)o);
        }
        if (o instanceof Long || o.getClass() == long.class)
        {
            return new LongNode((Long)o);
        }
        if (o instanceof Float || o.getClass() == float.class)
        {
            return new FloatNode((Float)o);
        }
        if (o instanceof Double || o.getClass() == double.class)
        {
            return new DoubleNode((Double)o);
        }
        if (o instanceof Boolean || o.getClass() == boolean.class)
        {
            return BooleanNode.of((Boolean)o);
        }
        if (o instanceof Character || o.getClass() == char.class)
        {
            return new CharNode((Character)o);
        }
        throw new IllegalArgumentException("Cannot wrap into Node: " + o.getClass());
    }

    /**
     * Converts a convertible Object into a Node
     *
     * @param object the Object
     *
     * @return the serialized Node
     */
    @SuppressWarnings("unchecked")
    public static <T> Node convertToNode(T object) throws ConversionException
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
     *
     * @return the original object
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertFromNode(Node node, Type type) throws ConversionException
    {
        if (node == null || node instanceof NullNode || type == null)
        { return null; }
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
                        return (T)collectionConverter.<Object, Collection<Object>>fromNode(ptype, (ListNode)node);
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
                        return (T)mapConverter.<Object, Object, Map<Object, Object>>fromNode(ptype, (MapNode)node);
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
