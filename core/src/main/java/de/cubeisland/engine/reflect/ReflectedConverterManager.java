/**
 * The MIT License
 * Copyright (c) 2013 Cube Island
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.cubeisland.engine.reflect;

import java.lang.reflect.Field;

import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.converter.node.MapNode;
import de.cubeisland.engine.converter.node.Node;

public class ReflectedConverterManager extends ConverterManager
{
    private final Reflected reflected;

    public ReflectedConverterManager(Reflected reflected)
    {
        super(null);
        this.reflected = reflected;
    }

    /**
     * Returns the Reflected owning this ConverterManager
     *
     * @return the Reflected or null if not owned
     */
    public Reflected getReflected()
    {
        return reflected;
    }


    public void fillReflected(MapNode node, Reflected reflected) throws ConversionException
    {
        if (reflected.isChild())
        {
            node.inheritFrom(convertReflected(reflected.getDefault()));
        }

        Object converted = this.convertFromNode(node, reflected.getClass());
        for (Field field : getConverterByClass(SectionConverter.class).getReflectedFields(reflected.getClass()))
        {
            try
            {
                field.set(reflected, field.get(converted));
            }
            catch (IllegalAccessException e)
            {
                throw new IllegalStateException(e); // impossible since it worked before
            }
        }
    }

    public Node convertReflected(Reflected reflected) throws ConversionException
    {
        return convertToNode(reflected);
    }
}
