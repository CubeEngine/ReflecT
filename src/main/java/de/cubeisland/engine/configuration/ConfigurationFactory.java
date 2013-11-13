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
package de.cubeisland.engine.configuration;

import de.cubeisland.engine.configuration.codec.ConfigurationCodec;
import de.cubeisland.engine.configuration.exception.ConfigurationInstantiationException;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationFactory
{
    public final Convert DEFAULT_CONVERTERS = Convert.defaultConverter(this);

    private final Map<Class<? extends ConfigurationCodec>, ConfigurationCodec> CODECS = new HashMap<Class<? extends ConfigurationCodec>, ConfigurationCodec>();

    /**
     * Loads the configuration from given file and optionally saves it afterwards
     *
     * @param clazz the configurations class
     * @param file  the file to load from and save to
     * @param save  whether to save the configuration or not
     *
     * @return the loaded Configuration
     */
    public <T extends Configuration> T load(Class<T> clazz, File file, boolean save)
    {
        T config = create(clazz); // loading
        config.setFile(file); // IMPORTANT TO SET BEFORE LOADING!
        config.reload(save);
        return config;
    }

    /**
     * Loads the configuration from given file and saves it afterwards
     *
     * @param clazz the configurations class
     * @param file  the file to load from and save to
     *
     * @return the loaded Configuration
     */
    public <T extends Configuration> T load(Class<T> clazz, File file)
    {
        return load(clazz, file, true);
    }

    /**
     * Loads the configuration from the InputStream
     *
     * @param clazz the configurations class
     * @param is    the InputStream to load from
     *
     * @return the loaded configuration
     */
    public <T extends Configuration> T load(Class<T> clazz, InputStream is)
    {
        T config = create(clazz);
        config.loadFrom(is);
        return config;
    }

    /**
     * Creates an instance of this given configuration-class.
     * <p>The configuration has to have the default Constructor for this to work!
     *
     * @param clazz the configurations class
     * @param <T>   The Type of the returned configuration
     *
     * @return the created configuration
     */
    public <T extends Configuration> T create(Class<T> clazz) throws ConfigurationInstantiationException
    {
        try
        {
            T config = clazz.newInstance();
            config.init(this);
            return config;
        }
        catch (ReflectiveOperationException e)
        {
            throw new ConfigurationInstantiationException(clazz, e);
        }
    }

    @SuppressWarnings("unchecked")
    public <Codec extends ConfigurationCodec> Codec getCodec(Class<Codec> codecClass)
    {
        Codec codec = (Codec) this.CODECS.get(codecClass);
        if (codec == null)
        {
            try
            {
                codec = codecClass.newInstance();
                codec.init(this);
            }
            catch (ReflectiveOperationException e)
            {
                throw new ConfigurationInstantiationException(codecClass, e);
            }
            this.CODECS.put(codecClass, codec);
        }
        return codec;
    }
}
