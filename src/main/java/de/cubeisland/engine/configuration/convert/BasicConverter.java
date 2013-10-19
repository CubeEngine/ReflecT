package de.cubeisland.engine.configuration.convert;

import de.cubeisland.engine.configuration.node.Node;

public abstract class BasicConverter<T> implements Converter<T>
{
    @Override
    @SuppressWarnings("unchecked")
    public Node toNode(T object) throws ConversionException
    {
        Class<T> clazz = (Class<T>)object.getClass();
        if (clazz.isPrimitive()
            || Number.class.isAssignableFrom(clazz)
            || CharSequence.class.isAssignableFrom(clazz)
            || Boolean.class.isAssignableFrom(clazz))
        {
            return Convert.wrapIntoNode(object);
        }
        throw new ConversionException("Illegal object type");
    }
}
