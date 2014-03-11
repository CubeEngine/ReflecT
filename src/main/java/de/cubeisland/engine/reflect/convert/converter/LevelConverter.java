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

import java.util.logging.Level;

import de.cubeisland.engine.reflect.codec.ConverterManager;
import de.cubeisland.engine.reflect.convert.Converter;
import de.cubeisland.engine.reflect.exception.ConversionException;
import de.cubeisland.engine.reflect.node.BooleanNode;
import de.cubeisland.engine.reflect.node.Node;
import de.cubeisland.engine.reflect.node.StringNode;

public class LevelConverter implements Converter<Level>
{
    public Node toNode(Level object, ConverterManager manager) throws ConversionException
    {
        return StringNode.of(object.toString());
    }

    public Level fromNode(Node node, ConverterManager manager) throws ConversionException
    {
        if (node instanceof StringNode)
        {
            Level lv = Level.parse(((StringNode)node).getValue());
            if (lv == null)
            {
                throw ConversionException.of(this, node, "Unknown Level: " + ((StringNode)node).getValue());
            }
            return lv;
        }
        else if (node instanceof BooleanNode && !((BooleanNode)node).getValue())
        { // OFF is interpreted as a boolean false, ALL as a boolean true
            if ((Boolean)node.getValue())
            {
                return Level.ALL;
            }
            else
            {
                return Level.OFF;
            }
        }
        throw ConversionException.of(this, node, "Node is not a StringNode OR BooleanNode!");
    }
}
