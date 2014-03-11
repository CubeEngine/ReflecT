/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Anselm Brehme, Phillip Schichtel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.cubeisland.engine.reflect.convert.converter;

import de.cubeisland.engine.reflect.codec.ConverterManager;
import de.cubeisland.engine.reflect.convert.BasicConverter;
import de.cubeisland.engine.reflect.exception.ConversionException;
import de.cubeisland.engine.reflect.node.FloatNode;
import de.cubeisland.engine.reflect.node.Node;

public class FloatConverter extends BasicConverter<Float>
{
    public Float fromNode(Node node, ConverterManager manager) throws ConversionException
    {
        if (node instanceof FloatNode)
        {
            return ((FloatNode)node).getValue();
        }
        String s = node.asText();
        try
        {
            return Float.parseFloat(s);
        }
        catch (NumberFormatException e)
        {
            throw ConversionException.of(this, node, "Node Incompatible with Float", e);
        }
    }
}
