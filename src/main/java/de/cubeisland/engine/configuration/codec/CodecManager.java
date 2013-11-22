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
package de.cubeisland.engine.configuration.codec;

import java.util.HashMap;
import java.util.Map;

import de.cubeisland.engine.configuration.exception.ConfigInstantiationException;

public class CodecManager
{
    private final ConverterManager defaultManager = ConverterManager.defaultManager();
    private final Map<Class<? extends ConfigurationCodec>, ConfigurationCodec> codecs = new HashMap<Class<? extends ConfigurationCodec>, ConfigurationCodec>();

    /**
     * Gets the instance of given <code>codecClass</code>
     *
     * @param codecClass the class of the codec
     * @param <Codec>    the type of the returned codec
     *
     * @return the codec instance
     */
    @SuppressWarnings("unchecked")
    public <Codec extends ConfigurationCodec> Codec getCodec(Class<Codec> codecClass)
    {
        Codec codec = (Codec)this.codecs.get(codecClass);
        if (codec == null) // Codec not registered yet! Try to auto-register...
        {
            try
            {
                codec = codecClass.newInstance();
                this.registerCodec(codec);
            }
            catch (InstantiationException e)
            {
                throw new ConfigInstantiationException("Could not instantiate unregistered Codec! " + codecClass.getName(), e);
            }
            catch (IllegalAccessException e)
            {
                throw new ConfigInstantiationException("Could not instantiate unregistered Codec! " + codecClass.getName(), e);
            }
        }
        return codec;
    }

    /**
     * Registeres a new Codec
     *
     * @param codec the codec to register
     */
    public <Codec extends ConfigurationCodec> void registerCodec(Codec codec)
    {
        this.codecs.put(codec.getClass(), codec);
        codec.setConverterManager(ConverterManager.emptyManager(this.defaultManager));
    }

    /**
     * Returns the default ConverterManager for all codecs managed by this CodecManager
     *
     * @return the default ConverterManager
     */
    public ConverterManager getDefaultConverterManager()
    {
        return this.defaultManager;
    }
}
