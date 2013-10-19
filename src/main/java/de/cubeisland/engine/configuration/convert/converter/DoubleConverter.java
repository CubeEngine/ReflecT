
package de.cubeisland.engine.configuration.convert.converter;

import de.cubeisland.engine.configuration.convert.BasicConverter;
import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.node.DoubleNode;
import de.cubeisland.engine.configuration.node.Node;

public class DoubleConverter extends BasicConverter<Double>
{
    @Override
    public Double fromNode(Node node) throws ConversionException
    {
        if (node instanceof DoubleNode)
        {
            return ((DoubleNode)node).getValue();
        }
        String s = node.asText();
        try
        {
            return Double.parseDouble(s);
        }
        catch (NumberFormatException e)
        {
            throw new ConversionException("Invalid Node!" + node.getClass(), e);
        }
    }
}
