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
package de.cubeisland.engine.reflect;

import java.io.InputStream;
import java.util.logging.Logger;

import de.cubeisland.engine.reflect.codec.CodecManager;
import de.cubeisland.engine.converter.ConverterManager;
import de.cubeisland.engine.reflect.exception.ReflectedInstantiationException;

/**
 * This Class creates new Reflected Objects and provides them with a CodecManger containing the Converters
 */
public class Reflector
{
    public static final Logger LOGGER = Logger.getLogger("ReflecT");
    private CodecManager codecManager = new CodecManager();

    /**
     * Loads the reflected from given source and optionally saves it afterwards
     *
     * @param clazz  the reflected class
     * @param source the file to load from and save to
     * @param save   whether to save the reflected or not
     *
     * @return the loaded reflected
     */
    public <T extends Reflected<?, S>, S> T load(Class<T> clazz, S source, boolean save)
    {
        T reflected = create(clazz);
        // IMPORTANT TO SET BEFORE LOADING!
        reflected.setTarget(source);
        reflected.reload(save);
        return reflected;
    }

    /**
     * Loads the reflected from given source and saves it afterwards
     *
     * @param clazz  the reflected class
     * @param source the source to load from and save to
     *
     * @return the loaded reflected
     */
    public <T extends Reflected<?, S>, S> T load(Class<T> clazz, S source)
    {
        return load(clazz, source, true);
    }

    /**
     * Loads the reflected from given InputStream
     *
     * @param clazz the reflected class
     * @param is    the InputStream to load from
     *
     * @return the loaded reflected
     */
    public <T extends ReflectedFile> T load(Class<T> clazz, InputStream is)
    {
        T reflected = create(clazz);
        reflected.loadFrom(is);
        return reflected;
    }

    /**
     * Creates an instance of given Reflected Class.
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

    /**
     * Returns the CodecManager
     *
     * @return the CodecManager
     */
    public CodecManager getCodecManager()
    {
        return this.codecManager;
    }

    /**
     * Returns the default ConverterManager
     *
     * @return the default ConverterManager
     */
    public ConverterManager getDefaultConverterManager()
    {
        return this.codecManager.getDefaultConverterManager();
    }
}
