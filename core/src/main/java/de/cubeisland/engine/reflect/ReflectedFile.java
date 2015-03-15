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

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import de.cubeisland.engine.reflect.codec.FileCodec;
import de.cubeisland.engine.reflect.exception.InvalidReflectedObjectException;

import static de.cubeisland.engine.reflect.Reflector.LOGGER;
import static java.util.logging.Level.WARNING;

/**
 * A Reflected saving into a {@link File} using a {@link FileCodec}
 */
public abstract class ReflectedFile<I extends Closeable, O extends Closeable, C extends FileCodec<I, O>> extends Reflected<C, File>
{
    private static final String[] EMPTY = new String[0];

    public final void save(File target)
    {
        if (target == null)
        {
            throw new IllegalArgumentException("A reflected cannot be saved without a valid file!");
        }
        O os = null;
        try
        {
            os = getCodec().newOutput(target);
            this.save(os);
            this.onSaved(target);
        }
        catch (IOException e)
        {
            throw new InvalidReflectedObjectException("File to save into cannot be accessed!", e);
        }
        finally
        {
            if (os != null)
            {
                try
                {
                    os.close();
                }
                catch (IOException e)
                {
                    Reflector.LOGGER.log(WARNING, "Failed to close the output stream", e);
                }
            }
        }
    }

    /**
     * Saves this reflected using given OutputStream
     *
     * @param os the OutputStream to write into
     */
    public final void save(O os)
    {
        this.onSave();
        this.getCodec().saveReflected(this, os);
    }

    public final boolean loadFrom(File source)
    {
        if (source == null)
        {
            throw new IllegalArgumentException("The file must not be null in order to load the reflected!");
        }
        if (source.exists())
        {
            I in = null;
            try
            {

                in = getCodec().newInput(source);
                this.loadFrom(in);
            }
            catch (IOException e)
            {
                throw new IllegalArgumentException("File to load from cannot be accessed!", e);
            }
            finally
            {
                if (in != null)
                {
                    try
                    {
                        in.close();
                    }
                    catch (IOException e)
                    {
                        Reflector.LOGGER.log(WARNING, "Failed to close the input stream!", e);
                    }
                }
            }
            this.onLoaded(source);
            return true;
        }
        LOGGER.log(Level.INFO, "Could not load reflected from file! Using default...");
        return false;
    }

    /**
     * Loads the reflected using the given InputStream
     *
     * @param is the InputStream to load from
     */
    public final void loadFrom(I is)
    {
        if (is == null)
        {
            throw new IllegalArgumentException("The input stream must not be null!");
        }
        this.onLoad();
        this.getCodec().loadReflected(this, is);
    }

    /**
     * Returns the File
     *
     * @return the file
     */
    public File getFile()
    {
        return this.getTarget();
    }

    /**
     * Sets the file
     *
     * @param file the file to set
     */
    public void setFile(File file)
    {
        this.setTarget(file);
    }

    /**
     * Returns the lines to be added in front of the Reflected.
     * <p>not every Codec may be able to use this
     *
     * @return the head
     */
    public String[] head()
    {
        return EMPTY;
    }

    /**
     * Returns the lines to be added at the end of the reflected.
     * <p>not every Codec may be able to use this
     *
     * @return the head
     */
    public String[] tail()
    {
        return EMPTY;
    }
}
