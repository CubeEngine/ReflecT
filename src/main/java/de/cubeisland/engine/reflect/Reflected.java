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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.cubeisland.engine.reflect.codec.Codec;
import de.cubeisland.engine.reflect.exception.InvalidReflectedObjectException;
import de.cubeisland.engine.reflect.exception.MissingCodecException;
import de.cubeisland.engine.reflect.node.ErrorNode;

import static de.cubeisland.engine.reflect.codec.Codec.getFieldType;
import static de.cubeisland.engine.reflect.codec.Codec.isReflectedField;

/**
 * This abstract class represents a reflected object to be serialized using a Codec C.
 */
public abstract class Reflected<C extends Codec> implements Section
{
    private transient Reflector factory;
    private transient Reflected defaultReflected = this;
    private transient final Class<C> defaultCodec = getCodecClass(this.getClass());
    protected transient File file;

    /**
     * Saves the fields that got inherited from the parent-reflected
     */
    private transient HashSet<Field> inheritedFields;

    public final void init(Reflector factory)
    {
        this.factory = factory;
        this.onInit();
    }

    public final Reflected getDefault()
    {
        return this.defaultReflected;
    }

    public final void setDefault(Reflected reflected)
    {
        if (reflected == null)
        {
            this.defaultReflected = this;
            this.inheritedFields = null;
        }
        else
        {
            if (!this.getClass().equals(reflected.getClass()))
            {
                throw new IllegalArgumentException("Parent and child-reflected have to be the same type of reflected!");
            }
            this.defaultReflected = reflected;
            this.inheritedFields = new HashSet<Field>();
        }
    }

    /**
     * Returns true if ConversionExceptions should be rethrown immediately
     * <p>this does not affect ConversionException thrown when converting fields into nodes
     *
     * <p>override to change
     *
     * @return whether to rethrow ConversionExceptions or log them instead
     */
    public boolean useStrictExceptionPolicy()
    {
        return true;
    }

    /**
     * Marks a field as being inherited from the parent reflected and thus not being saved
     * <p>if this reflectd is not a child-reflected nothing happens
     *
     * @param field the inherited field
     */
    public final void addInheritedField(Field field)
    {
        if (inheritedFields == null)
        {
            return;
        }
        this.inheritedFields.add(field);
    }

    /**
     * Marks a field as not being inherited from the parent reflected and thus saved into file
     * <p>if this reflected is not a child-reflected nothing happens
     *
     * @param field the not inherited field
     */
    public final void removeInheritedField(Field field)
    {
        if (inheritedFields == null)
        {
            return;
        }
        this.inheritedFields.remove(field);
    }

    /**
     * Returns whether the given field-value was inherited from a parent-reflected
     * <p>Returns always false when this is not a child-reflected
     *
     * @param field the field to check
     *
     * @return true if the field got inherited
     */
    public final boolean isInheritedField(Field field)
    {
        return inheritedFields != null && inheritedFields.contains(field);
    }

    /**
     * Loads and saves a child-reflected from given path with this reflected as parent
     *
     * @param sourceFile the path to the file
     * @param <T>        the ReflectedType
     *
     * @return the loaded child-reflected
     */
    @SuppressWarnings("unchecked")
    public <T extends Reflected> T loadChild(File sourceFile)
    {
        Reflected<C> childReflected = factory.create(this.getClass());
        childReflected.setFile(sourceFile);
        childReflected.setDefault(this);
        try
        {
            childReflected.reload(true);
            return (T)childReflected;
        }
        catch (InvalidReflectedObjectException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Unknown Exception while loading Child-Reflected!", ex);
        }
    }

    /**
     * Tries to get the CodecClazz of a Reflected implementation.
     *
     * @param clazz the clazz of the reflected
     *
     * @return the Codec
     */
    @SuppressWarnings("unchecked")
    private Class<C> getCodecClass(Class clazz)
    {
        Type genericSuperclass = clazz.getGenericSuperclass(); // Get generic superclass
        try
        {
            if (genericSuperclass.equals(Reflected.class))
            {
                // superclass is this class -> No Codec set as GenericType Missing Codec!
                return null;
            }
            if (genericSuperclass instanceof ParameterizedType) // check if genericSuperclass is ParametrizedType
            {
                Type gType = ((ParameterizedType)genericSuperclass).getActualTypeArguments()[0]; // get First gType
                // check if it is codec
                if (gType instanceof Class && Codec.class.isAssignableFrom((Class<?>)gType))
                {
                    return (Class<C>)gType;
                }
            }
            if (genericSuperclass instanceof Class)
            {
                return getCodecClass((Class)genericSuperclass); // lookup next superclass
            }
            throw new IllegalStateException("Unable to get Codec! " + genericSuperclass + " is not a class!");
        }
        catch (IllegalStateException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Something went wrong", ex);
        }
    }

    /**
     * Saves the reflected to the set file.
     */
    public final void save()
    {
        this.save(this.file);
    }

    /**
     * Saves this reflected into the given file
     *
     * @param target the file to save into
     */
    public final void save(File target)
    {
        if (target == null)
        {
            throw new IllegalArgumentException("A reflected cannot be saved without a valid file!");
        }
        try
        {
            this.save(new FileOutputStream(target));
            this.onSaved(file);
        }
        catch (FileNotFoundException ex)
        {
            throw new InvalidReflectedObjectException("File to save into cannot be accessed!", ex);
        }
    }

    /**
     * Saves this reflected using given OutputStream
     *
     * @param os the OutputStream to write into
     */
    public final void save(OutputStream os)
    {
        this.onSave();
        this.getCodec().saveReflected(this, os);
    }

    /**
     * Reloads the reflected from file
     * <p>This will only work if the file of the reflected got set previously
     */
    public final void reload()
    {
        this.reload(false);
    }

    /**
     * Reloads the reflected from file
     * <p>This will only work if the file of the reflected got set previously
     *
     * @param save true if the reflected should be saved after loading
     *
     * @return true when a new file got created while saving
     *
     * @throws de.cubeisland.engine.reflect.exception.InvalidReflectedObjectException if an error occurs while loading
     */
    public final boolean reload(boolean save) throws InvalidReflectedObjectException
    {
        boolean result = false;
        if (!this.loadFrom(this.file) && save)
        {
            this.factory.logger.info("Saved reflected in new file: " + file.getAbsolutePath());
            result = true;
        }
        if (save)
        {
            this.updateInheritance();
            this.save(); // save the default values
        }
        return result;
    }

    /**
     * Loads the Reflected using the given File
     * <p>This will NOT set the file of this Reflected
     *
     * @param file the file to load from
     *
     * @return true if the Reflected was loaded from file
     */
    public final boolean loadFrom(File file)
    {
        if (this.file == null)
        {
            throw new IllegalArgumentException("The file must not be null in order to load the reflected!");
        }
        try
        {
            this.loadFrom(new FileInputStream(this.file));
            this.onLoaded(file);
        }
        catch (FileNotFoundException ex)
        {
            this.factory.logger.log(Level.INFO, "Could not load reflected from file! Using default...", ex);
            return false;
        }
        return true;
    }

    /**
     * Loads the reflected using the given InputStream
     *
     * @param is the InputStream to load from
     */
    public final void loadFrom(InputStream is)
    {
        if (is == null)
        {
            throw new IllegalArgumentException("The input stream must not be null!");
        }
        this.onLoad();
        this.showLoadErrors(this.getCodec().loadReflected(this, is));
    }

    final void showLoadErrors(Collection<ErrorNode> errors)
    {
        if (!errors.isEmpty())
        {
            this.factory.logger.warning(errors.size() + " ErrorNodes were encountered while loading the reflected!");
            for (ErrorNode error : errors)
            {
                this.factory.logger.log(Level.WARNING, error.getErrorMessage());
            }
        }
    }

    /**
     * Returns the Codec
     *
     * @return the Codec defined in the GenericType of the reflected
     *
     * @throws MissingCodecException when no codec was set via genericType
     */
    public final C getCodec() throws MissingCodecException
    {
        if (defaultCodec == null)
        {
            throw new MissingCodecException("Reflected has no Codec set! A reflected object needs to have a codec defined in its GenericType");
        }
        return this.factory.getCodecManager().getCodec(this.defaultCodec);
    }

    /**
     * Sets the path to load from
     *
     * @param file the path the reflected will load from
     */
    public final void setFile(File file)
    {
        if (file == null)
        {
            throw new IllegalArgumentException("The file must not be null!");
        }
        this.file = file;
    }

    /**
     * Returns the path this reflected will be saved to and loaded from by default
     *
     * @return the path of this reflected
     */
    public final File getFile()
    {
        return this.file;
    }

    /**
     * This method gets called right after the reflected got loaded.
     */
    public void onLoaded(File loadedFrom)
    {}

    /**
     * This method gets called right after the reflected get saved.
     */
    public void onSaved(File savedTo)
    {}

    /**
     * Gets called after {@link #init(Reflector)} was called
     */
    public void onInit()
    {}

    /**
     * Gets called right before saving
     */
    public void onSave()
    {}

    /**
     * Gets called right before loading
     */
    public void onLoad()
    {}

    /**
     * Returns the lines to be added in front of the Reflected.
     * <p>not every Codec may be able to use this
     *
     * @return the head
     */
    public String[] head()
    {
        return null;
    }

    /**
     * Returns the lines to be added at the end of the reflected.
     * <p>not every Codec may be able to use this
     *
     * @return the head
     */
    public String[] tail()
    {
        return null;
    }

    public Logger getLogger()
    {
        return this.factory.logger;
    }

    public final void updateInheritance()
    {
        if (this.defaultReflected == null || this.defaultReflected == this)
        {
            return; // Default is this reflected anyways
        }
        this.inheritedFields = new HashSet<Field>();
        this.updateInheritance(this, defaultReflected);
    }

    @SuppressWarnings("unchecked")
    private void updateInheritance(Section section, Section defaultSection)
    {
        try
        {
            for (Field field : section.getClass().getFields())
            {
                if (isReflectedField(field))
                {
                    Object value = field.get(section);
                    Object defaultValue = field.get(defaultSection);
                    if ((value == null && defaultValue == null) || (value != null && defaultValue != null && value.equals(defaultValue)))
                    {
                        this.addInheritedField(field);
                        continue;
                    }
                    else if (value == null || defaultValue == null)
                    {
                        continue;
                    }
                    switch (getFieldType(field))
                    {
                    case NORMAL: // Already handled
                        break;
                    case SECTION:
                        this.updateInheritance((Section)value, (Section)defaultValue);
                        break;
                    case SECTION_COLLECTION:
                        throw new IllegalStateException("Collections in child reflected are not allowed!");
                    case SECTION_MAP:
                        for (Entry<?, Section> entry : ((Map<?, Section>)value).entrySet())
                        {
                            Section defaulted = ((Map<?, Section>)defaultValue).get(entry.getKey());
                            if (defaulted != null)
                            {
                                this.updateInheritance(entry.getValue(), defaulted);
                            }
                        }
                        break;
                    }
                }
            }
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
