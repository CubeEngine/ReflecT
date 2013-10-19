package de.cubeisland.engine.configuration.convert.converter.generic;

import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.convert.Convert;
import de.cubeisland.engine.configuration.node.ListNode;
import de.cubeisland.engine.configuration.node.Node;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedList;

public class ArrayConverter
{
    public ListNode toNode(Object[] array) throws ConversionException
    {
        ListNode result = ListNode.emptyList();
        if (array.length == 0)
        {
            return result;
        }
        for (Object value : array)
        {
            result.addNode(Convert.toNode(value));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public <V> V[] fromNode(Class<V[]> arrayType, ListNode listNode) throws ConversionException
    {
        Class<V> valueType = (Class<V>)arrayType.getComponentType();
        try
        {
            Collection<V> result = new LinkedList<>();
            for (Node node : listNode.getListedNodes())
            {
                V value = Convert.fromNode(node, valueType);
                result.add(value);
            }
            return result.toArray((V[])Array.newInstance((Class)valueType, result.size()));
        }
        catch (ConversionException ex)
        {
            throw new IllegalStateException("Array-conversion failed: Error while converting the values in the array.");
        }
    }
}
