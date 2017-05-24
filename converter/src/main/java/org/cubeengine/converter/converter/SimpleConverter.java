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

/**
 * A simple Converter for a single Class
 */
public abstract class SimpleConverter<ConvertT> implements ClassedConverter<ConvertT>
{
    public final Node toNode(ConvertT object, ConverterManager manager) throws ConversionException
    {
        return toNode(object);
    }

    public final ConvertT fromNode(Node node, Class<? extends ConvertT> type,
                                   ConverterManager manager) throws ConversionException
    {
        return fromNode(node);
    }

    /**
     * Converts the object into a serializable Node
     *
     * @param object the object to convert
     *
     * @return the converted object
     */
    public abstract Node toNode(ConvertT object) throws ConversionException;

    /**
     * Converts the node back into the original object
     *
     * @param node the node to convert
     *
     * @return the converted node
     */
    public abstract ConvertT fromNode(Node node) throws ConversionException;
}
