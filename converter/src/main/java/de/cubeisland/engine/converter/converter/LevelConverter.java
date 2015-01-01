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
package de.cubeisland.engine.converter.converter;

import java.util.logging.Level;

import de.cubeisland.engine.converter.ConversionException;
import de.cubeisland.engine.converter.node.BooleanNode;
import de.cubeisland.engine.converter.node.Node;
import de.cubeisland.engine.converter.node.StringNode;

/**
 * A Converter for {@link java.util.logging.Level}
 */
public class LevelConverter extends SimpleConverter<Level>
{
    @Override
    public Node toNode(Level object) throws ConversionException
    {
        return StringNode.of(object.toString());
    }

    @Override
    public Level fromNode(Node node) throws ConversionException
    {
        if (node instanceof StringNode)
        {
            try
            {
                return Level.parse(((StringNode)node).getValue());
            }
            catch (IllegalArgumentException e)
            {
                throw ConversionException.of(this, node, "Unknown Level: " + ((StringNode)node).getValue(), e);
            }
        }
        else if (node instanceof BooleanNode && !((BooleanNode)node).getValue())
        {
            // OFF is interpreted as a boolean false, ALL as a boolean true
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
