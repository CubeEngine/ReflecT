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
package org.cubeengine.reflect.codec;

import org.cubeengine.converter.ConversionException;
import org.cubeengine.converter.ConverterManager;
import org.cubeengine.converter.node.MapNode;
import org.cubeengine.reflect.Reflected;
import org.cubeengine.reflect.Reflector;

/**
 * This abstract Codec can be implemented to read and write reflected objects that allow child-reflected
 */
public abstract class Codec<InputT, OutputT>
{
    private ConverterManager converterManager;
    private Reflector reflector;

    /**
     * Called via registering with the CodecManager
     */
    final void init(ConverterManager converterManager, Reflector reflector)
    {
        this.reflector = reflector;
        if (converterManager == null)
        {
            throw new IllegalArgumentException("The converter manager may not be null!");
        }
        this.converterManager = converterManager;
        onInit();
    }

    protected void onInit()
    {}

    /**
     * Returns the {@link ConverterManager} for this codec, allowing to register custom {@link org.cubeengine.converter.converter.ClassedConverter} for this codec only
     *
     * @return the ConverterManager
     *
     * @throws UnsupportedOperationException if the Codec was not instantiated by the Factory
     */
    public final ConverterManager getConverterManager()
    {
        if (converterManager == null)
        {
            throw new UnsupportedOperationException(
                "This codec is not registered in the CodecManager and therefor has no ConverterManager for its own converters");
        }
        return converterManager;
    }

    /**
     * Loads in the given {@link Reflected} using the <code>Input</code>
     *
     * @param reflected the Reflected to load
     * @param input     the Input to load from
     */
    public abstract void loadReflected(Reflected reflected, InputT input);

    /**
     * Saves the {@link Reflected} using given <code>Output</code>
     *
     * @param reflected the Reflected to save
     * @param output    the Output to save into
     */
    public abstract void saveReflected(Reflected reflected, OutputT output);

    /**
     * Saves the values contained in the {@link MapNode} using given <code>Output</code>
     *
     * @param node      the MapNode containing all data to save
     * @param out       the Output to save to
     * @param reflected the Reflected
     */
    protected abstract void save(MapNode node, OutputT out, Reflected reflected) throws ConversionException;

    /**
     * Converts the <code>Input</code> into a {@link MapNode}
     *
     * @param in        the Input to load from
     * @param reflected the Reflected
     */
    protected abstract MapNode load(InputT in, Reflected reflected) throws ConversionException;


    /**
     * Converts given Reflected into a MapNode
     *
     * @param reflected the Reflected to convert
     *
     * @return the MapNode
     */
    public final MapNode convertReflected(Reflected reflected)
    {
        try
        {
            reflected.getConverterManager().withFallback(converterManager);
            return (MapNode)reflected.getConverterManager().convertReflected(reflected);
        }
        catch (ConversionException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts a MapNode to fill a Reflected with values
     *
     * @param reflected the Reflected to fill
     * @param node      the MapNode
     */
    public final void fillReflected(Reflected reflected, MapNode node)
    {
        try
        {
            reflected.getConverterManager().withFallback(this.converterManager);
            reflected.getConverterManager().fillReflected(node, reflected);
        }
        catch (ConversionException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the Reflector this Codec was initialized with
     *
     * @return the Reflector
     */
    public Reflector getReflector()
    {
        return reflector;
    }
}
