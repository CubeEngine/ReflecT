package de.cubeisland.engine.configuration.convert.converter;

import de.cubeisland.engine.configuration.convert.BasicConverter;
import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.ShortNode;

public class ShortConverter extends BasicConverter<Short>
{
    @Override
    public Short fromNode(Node node) throws ConversionException
    {
        if (node instanceof ShortNode)
        {
            return ((ShortNode)node).getValue();
        }
        String s = node.asText();
        try
        {
            return Short.parseShort(s);
        }
        catch (NumberFormatException e)
        {
            throw new ConversionException("Invalid Node!" + node.getClass(), e);
        }
    }
}
