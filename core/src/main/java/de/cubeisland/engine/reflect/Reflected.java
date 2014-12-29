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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import de.cubeisland.engine.reflect.codec.Codec;
import de.cubeisland.engine.reflect.exception.InvalidReflectedObjectException;
import de.cubeisland.engine.reflect.exception.MissingCodecException;


/**
 * This abstract class represents a reflected object to be serialized using a Codec C.
 */
public abstract class Reflected<CodecT extends Codec, SerialType> implements Section
{
    private final transient Class<CodecT> defaultCodec = getCodecClass(getClass());
    private transient Reflector reflector;
    private transient SerialType serialType;
    private transient Reflected defaults = this;

    private transient ReflectedConverterManager manager = new ReflectedConverterManager(this);

    /**
     * Saves the fields that got inherited from the parent-reflected
     */
    private transient Set<Field> inheritedFields;

    /**
     * Initializes the Reflected with a Reflector
     * <p>This needs to be called before using any save or load method
     *
     * @param reflector the Reflector
     */
    public final void init(Reflector reflector)
    {
        this.reflector = reflector;
        this.onInit();
    }

    /**
     * Returns the reflector used for this reflected
     *
     * @return the reflector
     */
    public Reflector getReflector()
    {
        return reflector;
    }

    /**
     * Returns the default Reflected
     * <p>If not a child Reflected the default is <code>this</code>
     *
     * @return the default Reflected
     */
    public final Reflected getDefault()
    {
        return this.defaults;
    }

    /**
     * Sets the default Reflected
     *
     * @param reflected the Reflected
     */
    public final void setDefault(Reflected reflected)
    {
        if (reflected == null)
        {
            this.defaults = this;
            this.inheritedFields = null;
            return;
        }
        if (!getClass().equals(reflected.getClass()))
        {
            throw new IllegalArgumentException("Parent and child-reflected have to be the same type of reflected!");
        }
        this.defaults = reflected;
        this.inheritedFields = new HashSet<Field>();
    }

    /**
     * Returns true if ConversionExceptions should be rethrown immediately
     * <p>this does not affect ConversionException thrown when converting fields into nodes
     * <p>override to change
     *
     * @return whether to rethrow ConversionExceptions or log them instead
     */
    public boolean useStrictExceptionPolicy()
    {
        return true;
    }

    /**
     * Marks a field as being inherited from the default Reflected and thus not being saved
     * <p>if this Reflected is not a child Reflected nothing happens
     *
     * @param field the inherited field
     */
    protected final void addInheritedField(Field field)
    {
        if (inheritedFields == null)
        {
            return;
        }
        this.inheritedFields.add(field);
    }

    /**
     * Marks a field as not being inherited from the default Reflected and thus saved into file
     * <p>if this Reflected is not a child Reflected nothing happens
     *
     * @param field the not inherited field
     */
    protected final void removeInheritedField(Field field)
    {
        if (inheritedFields == null)
        {
            return;
        }
        this.inheritedFields.remove(field);
    }

    /**
     * Returns whether the given field was inherited from an other Reflected
     * <p>Returns always false when this is not a child Reflected
     *
     * @param field the field to check
     *
     * @return true if the field got inherited
     */
    protected final boolean isInheritedField(Field field)
    {
        return inheritedFields != null && inheritedFields.contains(field);
    }

    /**
     * Loads and saves a child Reflected from given SerialType with this Reflected as default
     *
     * @param source the source SerialType
     * @param <T>    the ReflectedType
     *
     * @return the loaded child Reflected
     */
    @SuppressWarnings("unchecked")
    public <T extends Reflected> T loadChild(SerialType source)
    {
        Reflected<CodecT, SerialType> childReflected = reflector.create(this.getClass());
        childReflected.setTarget(source);
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
    private Class<CodecT> getCodecClass(Class clazz)
    {
        Type genericSuperclass = clazz;
        try
        {
            while (genericSuperclass instanceof Class)
            {
                genericSuperclass = ((Class)genericSuperclass).getGenericSuperclass();
                if (Reflected.class.equals(genericSuperclass))
                {
                    // superclass is this class -> No Codec set as GenericType Missing Codec!
                    return null;
                }
                // check if genericSuperclass is ParametrizedType
                if (genericSuperclass instanceof ParameterizedType)
                {
                    // get First gType
                    Type gType = ((ParameterizedType)genericSuperclass).getActualTypeArguments()[0];
                    // check if it is codec
                    if (gType instanceof Class && Codec.class.isAssignableFrom((Class<?>)gType))
                    {
                        return (Class<CodecT>)gType;
                    }
                    genericSuperclass = ((ParameterizedType)genericSuperclass).getRawType();
                }
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
     * Saves the Reflected default SerialType
     */
    public final void save()
    {
        this.save(this.serialType);
    }

    /**
     * Saves this Reflected into the target
     *
     * @param target the target to save to
     */
    public abstract void save(SerialType target);

    /**
     * Reloads the Reflected from the default SerialType
     * <p>This will only work if the SerialType got set previously
     */
    public final void reload()
    {
        this.reload(false);
    }

    /**
     * Reloads the Reflected from the default SerialType
     * <p>This will only work if the SerialType got set previously
     *
     * @param save true if the Reflected should be saved after loading
     *
     * @return false when the reflected did not get loaded
     */
    public final boolean reload(boolean save) throws InvalidReflectedObjectException
    {
        boolean result = false;
        if (!this.loadFrom(this.serialType) && save)
        {
            result = true;
        }
        if (save)
        {
            this.updateInheritance();
            // save the default values
            this.save();
        }
        return result;
    }

    /**
     * Loads the Reflected using the given SerialType
     * <p>This will NOT set the SerialType of this Reflected
     *
     * @param source the SerialType to load from
     *
     * @return true if the Reflected was loaded from the given source
     */
    public abstract boolean loadFrom(SerialType source);

    /**
     * Returns the Codec
     *
     * @return the Codec defined in the GenericType of the reflected
     *
     * @throws MissingCodecException when no codec was set via genericType
     */
    public final CodecT getCodec() throws MissingCodecException
    {
        if (defaultCodec == null)
        {
            throw new MissingCodecException(
                "Reflected has no Codec set! A reflected object needs to have a codec defined in its GenericType");
        }
        return this.reflector.getCodecManager().getCodec(this.defaultCodec);
    }

    /**
     * Returns the SerialType this reflected will be saved to and loaded from by default
     *
     * @return the path of this reflected
     */
    public final SerialType getTarget()
    {
        return this.serialType;
    }

    /**
     * Sets the SerialType to load from
     *
     * @param type the SerialType the reflected will load from and save to by default
     */
    public final void setTarget(SerialType type)
    {
        if (type == null)
        {
            throw new IllegalArgumentException("The SerialType must not be null!");
        }
        this.serialType = type;
    }

    /**
     * Gets called right before loading
     */
    public void onLoad()
    {
        // implement onLoad
    }

    /**
     * This method gets called right after the reflected got loaded.
     */
    public void onLoaded(SerialType loadedFrom)
    {
        // implement onLoaded
    }

    /**
     * This method gets called right after the reflected get saved.
     */
    public void onSaved(SerialType savedTo)
    {
        // implement onSaved
    }

    /**
     * Gets called after {@link #init(Reflector)} was called
     */
    public void onInit()
    {
        // implement onInit
    }

    /**
     * Gets called right before saving
     */
    public void onSave()
    {
        // implement onSave
    }

    /**
     * Updates the inheritance of Fields
     * <p>This does nothing if the Reflected has no other default set
     */
    public final void updateInheritance()
    {
        if (this.defaults == null || this.defaults == this)
        {
            return;
        }
        this.inheritedFields = new HashSet<Field>();
        SectionConverter sectionConverter = this.getConverterManager().getConverterByClass(SectionConverter.class);
        try
        {
            this.updateInheritance(this, defaults, sectionConverter);
        }
        catch (IllegalAccessException e)
        {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Updates the inheritance of the Sections
     *
     * @param section          the Section
     * @param defaults   the default Section
     * @param converter the SectionConverter
     */
    private void updateInheritance(Section section, Section defaults, SectionConverter converter) throws IllegalAccessException
    {
        for (Field field : converter.getReflectedFields(section.getClass()))
        {
            Type type = field.getGenericType();

            Object value = field.get(section);
            Object defaultValue = field.get(defaults);
            if (value == null && defaultValue == null)
            {
                this.addInheritedField(field);
                return;
            }
            if (value != null && defaultValue != null)
            {
                if (value.equals(defaultValue))
                {
                    this.addInheritedField(field);
                }
                else if (value instanceof Section && defaultValue instanceof Section)
                {
                    this.updateInheritance((Section)value, (Section)defaultValue, converter);
                }
                else if (type instanceof ParameterizedType)
                {
                    updateSectionMapInheritance(converter, (ParameterizedType)type, value, defaultValue);
                }
            }
        }
    }

    private void updateSectionMapInheritance(SectionConverter sectionConverter, ParameterizedType pType, Object value,
                                             Object defaultValue) throws IllegalAccessException
    {
        if (Map.class.isAssignableFrom((Class<?>)pType.getRawType()) && Section.class.isAssignableFrom(
            (Class<?>)pType.getActualTypeArguments()[1]))
        {
            @SuppressWarnings("unchecked") Map<?, Section> valueMap = (Map<?, Section>)value;
            @SuppressWarnings("unchecked") Map<?, Section> defaultValueMap = (Map<?, Section>)defaultValue;
            for (Entry<?, Section> entry : valueMap.entrySet())
            {
                this.updateInheritance(entry.getValue(), defaultValueMap.get(entry.getKey()), sectionConverter);
            }
        }
    }

    /**
     * Returns the ConverterManager
     *
     * @return the ConverterManager
     */
    public final ReflectedConverterManager getConverterManager()
    {
        return manager;
    }

    /**
     * Returns true if this Reflected has an other default Reflected than itself
     */
    public final boolean isChild()
    {
        return this.getDefault() != this;
    }
}
