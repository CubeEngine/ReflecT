package de.cubeisland.engine.configuration.convert.converter;

import de.cubeisland.engine.configuration.convert.BasicConverter;
import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.node.IntNode;
import de.cubeisland.engine.configuration.node.Node;

public class IntegerConverter extends BasicConverter<Integer>
{
    @Override
    public Integer fromNode(Node node) throws ConversionException
    {
        if (node instanceof IntNode)
        {
            return ((IntNode)node).getValue();
        }
        String s = node.asText();
        try
        {
            return Integer.parseInt(s);
        }
        catch (NumberFormatException e)
        {
            throw new ConversionException("Invalid Node!" + node.getClass(), e);
        }
    }
}
