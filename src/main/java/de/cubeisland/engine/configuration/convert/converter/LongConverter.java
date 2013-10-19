package de.cubeisland.engine.configuration.convert.converter;

import de.cubeisland.engine.configuration.convert.BasicConverter;
import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.node.LongNode;
import de.cubeisland.engine.configuration.node.Node;

public class LongConverter extends BasicConverter<Long>
{
    @Override
    public Long fromNode(Node node) throws ConversionException
    {
        if (node instanceof LongNode)
        {
            return ((LongNode)node).getValue();
        }
        String s = node.asText();
        try
        {
            return Long.parseLong(s);
        }
        catch (NumberFormatException e)
        {
            throw new ConversionException("Invalid Node!" + node.getClass(), e);
        }
    }
}
