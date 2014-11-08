package de.cubeisland.engine.reflect.codec.converter;

import de.cubeisland.engine.reflect.Section;

public class IllegalDefaultSectionException extends RuntimeException
{
    public IllegalDefaultSectionException(Section section, Section defaultSection)
    {
        super("Default Section has divergent type! Type was: " + defaultSection.getClass().getName() + " Expected: " + section.getClass().getName());
    }
}
