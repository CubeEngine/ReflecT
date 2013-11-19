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
/**
 * This file is part of CubeEngine.
 * CubeEngine is licensed under the GNU General Public License Version 3.
 *
 * CubeEngine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CubeEngine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CubeEngine.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.cubeisland.engine.configuration.convert.converter;

import java.util.logging.Level;

import de.cubeisland.engine.configuration.codec.ConverterManager;
import de.cubeisland.engine.configuration.convert.Converter;
import de.cubeisland.engine.configuration.exception.ConversionException;
import de.cubeisland.engine.configuration.node.BooleanNode;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.StringNode;

public class LevelConverter implements Converter<Level>
{
    public Node toNode(ConverterManager manager, Level object) throws ConversionException
    {
        return StringNode.of(object.toString());
    }

    public Level fromNode(ConverterManager manager, Node node) throws ConversionException
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
