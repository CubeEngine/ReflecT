/*
 * The MIT License
 * Copyright © 2013 Cube Island
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
package org.cubeengine.converter.converter;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.converter.node.Node;
import org.cubeengine.converter.node.StringNode;

/**
 * A converter for generic enums
 */
public class EnumConverter implements ClassedConverter<Enum>
{
    public Node toNode(Enum object, ConverterManager manager) throws ConversionException
    {
        return StringNode.of(object.name());
    }

    public Enum fromNode(Node node, Class<? extends Enum> enumClass, ConverterManager manager) throws ConversionException
    {
        for (Enum enumT : enumClass.getEnumConstants())
        {
            if (enumT.name().equalsIgnoreCase(node.asText()))
            {
                return enumT;
            }
        }
        throw ConversionException.of(this, node, "Enum value not found!");
    }
}
