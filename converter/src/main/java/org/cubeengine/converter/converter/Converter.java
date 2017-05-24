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

import java.lang.reflect.Type;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.converter.node.Node;

/**
 * Provides Methods to convert a <code>T</code> into a <code>Node</code> and back
 */
public interface Converter<ConvertT, TypeT extends Type>
{
    /**
     * Converts the object into a serializable Node
     *
     * @param object  the object to convert
     * @param manager the ConverterManager
     *
     * @return the converted object
     */
    Node toNode(ConvertT object, ConverterManager manager) throws ConversionException;

    /**
     * Converts the node back into the original object
     *
     * @param node    the node to convert
     * @param type    the type to convert to
     * @param manager the manager
     *
     * @return the converted node
     */
    ConvertT fromNode(Node node, TypeT type, ConverterManager manager) throws ConversionException;
}
