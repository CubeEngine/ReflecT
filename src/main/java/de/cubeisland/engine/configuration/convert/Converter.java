
package de.cubeisland.engine.configuration.convert;

import de.cubeisland.engine.configuration.node.Node;

public interface Converter<T extends Object>
{
    /**
     * Converts this class to an serializable object
     *
     * @param object the fieldvalue
     * @return the fieldvalue as node
     */
    public Node toNode(T object) throws ConversionException;

    /**
     * Converts the given object to this class
     *
     * @param node the node to deserialize
     * @return the deserialized fieldvalue
     */
    public T fromNode(Node node) throws ConversionException;
}
