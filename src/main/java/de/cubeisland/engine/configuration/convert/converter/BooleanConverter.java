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
package de.cubeisland.engine.configuration.convert.converter;

import de.cubeisland.engine.configuration.codec.ConverterManager;
import de.cubeisland.engine.configuration.convert.BasicConverter;
import de.cubeisland.engine.configuration.exception.ConversionException;
import de.cubeisland.engine.configuration.node.BooleanNode;
import de.cubeisland.engine.configuration.node.Node;

public class BooleanConverter extends BasicConverter<Boolean>
{
    public Boolean fromNode(ConverterManager manager, Node node) throws ConversionException
    {
        if (node instanceof BooleanNode)
        {
            return ((BooleanNode)node).getValue();
        }
        String s = node.asText();
        if (s == null)
        {
            return null;
        }
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("on")
            || s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("1"))
        {
            return true;
        }
        if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("off")
            || s.equalsIgnoreCase("no") || s.equalsIgnoreCase("0"))
        {
            return false;
        }

        throw ConversionException.of(this, node, "Node incompatible with Boolean!");
    }
}
