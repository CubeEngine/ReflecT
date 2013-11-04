package de.cubeisland.engine.configuration.convert.converter;

import java.util.Locale;

import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.convert.Converter;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.StringNode;

public class LocaleConverter implements Converter<Locale>
{
    public Node toNode(Locale locale) throws ConversionException
    {
        return StringNode.of(locale.getLanguage().toLowerCase(Locale.ENGLISH) + '_' + locale.getCountry().toUpperCase(Locale.ENGLISH));
    }

    public Locale fromNode(Node node) throws ConversionException
    {
        if (node instanceof StringNode)
        {
            String localeTag = ((StringNode)node).getValue().trim();

            localeTag = localeTag.replace("-", "_");

            String[] parts = localeTag.split("_", 2);
            String language = parts[0];
            String country = "";

            if (language.length() > 3)
            {
                language = language.substring(0, 2);
            }
            if (parts.length > 1)
            {
                country = parts[1];
                if (country.length() > 2)
                {
                    country = country.substring(0, 2);
                }
            }
            return new Locale(language, country);
        }
        throw new ConversionException("Locales can only be loaded from a string node!");
    }
}