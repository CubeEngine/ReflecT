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
package de.cubeisland.engine.reflect;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;

import de.cubeisland.engine.reflect.codec.CodecManager;
import de.cubeisland.engine.reflect.codec.ConverterManager;
import de.cubeisland.engine.reflect.exception.ReflectedInstantiationException;

public class Reflector
{
    private CodecManager codecManager = new CodecManager();
    Logger logger; // package private

    public Reflector()
    {
        this.logger = Logger.getLogger("ReflecT");
    }

    public Reflector(Logger logger)
    {
        this.logger = logger;
    }

    /**
     * Loads the reflected from given file and optionally saves it afterwards
     *
     * @param clazz the reflected class
     * @param file  the file to load from and save to
     * @param save  whether to save the reflected or not
     *
     * @return the loaded reflected
     */
    public <T extends Reflected> T load(Class<T> clazz, File file, boolean save)
    {
        T reflected = create(clazz); // loading
        reflected.setFile(file); // IMPORTANT TO SET BEFORE LOADING!
        reflected.reload(save);
        return reflected;
    }

    /**
     * Loads the reflected from given file and saves it afterwards
     *
     * @param clazz the reflected class
     * @param file  the file to load from and save to
     *
     * @return the loaded reflected
     */
    public <T extends Reflected> T load(Class<T> clazz, File file)
    {
        return load(clazz, file, true);
    }

    /**
     * Loads the reflected from the InputStream
     *
     * @param clazz the reflected class
     * @param is    the InputStream to load from
     *
     * @return the loaded reflected
     */
    public <T extends Reflected> T load(Class<T> clazz, InputStream is)
    {
        T reflected = create(clazz);
        reflected.loadFrom(is);
        return reflected;
    }

    /**
     * Creates an instance of given reflected-class.
     * <p>The reflected has to have the default Constructor for this to work!
     *
     * @param clazz the reflected class
     * @param <T>   The type of the returned reflected
     *
     * @return the created reflected
     */
    public <T extends Reflected> T create(Class<T> clazz) throws ReflectedInstantiationException
    {
        try
        {
            T reflected = clazz.newInstance();
            reflected.init(this);
            return reflected;
        }
        catch (IllegalAccessException e)
        {
            throw new ReflectedInstantiationException(clazz, e);
        }
        catch (InstantiationException e)
        {
            throw new ReflectedInstantiationException(clazz, e);
        }
    }

    public CodecManager getCodecManager()
    {
        return this.codecManager;
    }

    public ConverterManager getDefaultConverterManager()
    {
        return this.codecManager.getDefaultConverterManager();
    }

    /**
     * Sets the logger for all reflected created by this factory
     *
     * @param logger the logger
     */
    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }
}
