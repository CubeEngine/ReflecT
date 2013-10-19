package de.cubeisland.engine.configuration.convert.converter;

import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.convert.Convert;
import de.cubeisland.engine.configuration.convert.Converter;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.StringNode;

import java.sql.Date;

public class DateConverter implements Converter<Date>
{
    @Override
    public Node toNode(Date object) throws ConversionException
    {
        return Convert.wrapIntoNode(object.toString());
    }

    @Override
    public Date fromNode(Node node) throws ConversionException
    {
        if (node instanceof StringNode)
        {
            return Date.valueOf(((StringNode)node).getValue());
        }
        throw new ConversionException("Invalid Node!" + node.getClass());
    }
}
