package de.cubeisland.engine.configuration;

import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.convert.Converter;
import de.cubeisland.engine.configuration.convert.ConverterNotFoundException;
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

public class Convert
{
    private Map<Class, Converter> converters = new ConcurrentHashMap<Class, Converter>();
    private MapConverter mapConverter = new MapConverter();
    private ArrayConverter arrayConverter = new ArrayConverter();
    private CollectionConverter collectionConverter = new CollectionConverter();

    Convert()  // Register Default Converters
    {
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
        registerConverter(UUID.class, new UUIDConverter());
        registerConverter(Locale.class, new LocaleConverter());
    }

    /**
     * registers a converter to check for when converting
     *
     * @param clazz     the class
     * @param converter the converter
     */
    public void registerConverter(Class clazz, Converter converter)
    {
        if (clazz == null || converter == null)
        {
            return;
        }
        converters.put(clazz, converter);
    }

    public void removeConverter(Class clazz)
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

    public void removeConverters()
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
    public <T> Converter<T> matchConverter(Class<? extends T> objectClass) throws ConverterNotFoundException
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
    public Node wrapIntoNode(Object o)
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
    public <T> Node convertToNode(T object) throws ConversionException
    {
        if (object == null)
        {
            return NullNode.emptyNode();
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
    public <T> T convertFromNode(Node node, Type type) throws ConversionException
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
