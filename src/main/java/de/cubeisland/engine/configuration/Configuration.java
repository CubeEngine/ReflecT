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

import de.cubeisland.engine.configuration.codec.ConfigurationCodec;
import de.cubeisland.engine.configuration.exception.InvalidConfigurationException;
import de.cubeisland.engine.configuration.exception.MissingCodecException;
import de.cubeisland.engine.configuration.node.ErrorNode;

import static de.cubeisland.engine.configuration.codec.ConfigurationCodec.getFieldType;
import static de.cubeisland.engine.configuration.codec.ConfigurationCodec.isConfigField;

/**
 * This abstract class represents a configuration.
 */
public abstract class Configuration<Codec extends ConfigurationCodec> implements Section
{
    private transient ConfigurationFactory factory;
    private transient Configuration defaultConfig = this;
    private transient final Class<Codec> defaultCodec = getCodecClass(this.getClass());
    protected transient File file;

    /**
     * Saves the fields that got inherited from the parent-configuration
     */
    private transient HashSet<Field> inheritedFields;

    public final void init(ConfigurationFactory factory)
    {
        this.factory = factory;
    }

    public final Configuration getDefault()
    {
        return this.defaultConfig;
    }

    public final void setDefault(Configuration config)
    {
        if (config == null)
        {
            this.defaultConfig = this;
            this.inheritedFields = null;
        }
        else
        {
            if (!this.getClass().equals(config.getClass()))
            {
                throw new IllegalArgumentException("Parent and child-configuration have to be the same type of configuration!");
            }
            this.defaultConfig = config;
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
     * Marks a field as being inherited from the parent configuration and thus not being saved
     * <p>if this configuration is not a child-configuration nothing happens
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
     * Marks a field as not being inherited from the parent configuration and thus saved into file
     * <p>if this configuration is not a child-configuration nothing happens
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
     * Returns whether the given field-value was inherited from a parent-configuration
     * <p>Returns always false when this is not a child-configuration
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
     * Loads and saves a child-configuration from given path with this configuration as parent
     *
     * @param sourceFile the path to the file
     * @param <T>        the ConfigurationType
     *
     * @return the loaded child-configuration
     */
    @SuppressWarnings("unchecked")
    public <T extends Configuration> T loadChild(File sourceFile)
    {
        Configuration<Codec> childConfig = factory.create(this.getClass());
        childConfig.setFile(sourceFile);
        childConfig.setDefault(this);
        try
        {
            childConfig.reload(true);
            return (T)childConfig;
        }
        catch (InvalidConfigurationException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Unknown Exception while loading ChildConfig!", ex);
        }
    }

    /**
     * Tries to get the CodecClazz of a configuration implementation.
     *
     * @param clazz the clazz of the configuration
     *
     * @return the Codec
     */
    @SuppressWarnings("unchecked")
    private Class<Codec> getCodecClass(Class clazz)
    {
        Type genericSuperclass = clazz.getGenericSuperclass(); // Get generic superclass
        try
        {
            if (genericSuperclass.equals(Configuration.class))
            {
                // superclass is this class -> No Codec set as GenericType Missing Codec!
                return null;
            }
            if (genericSuperclass instanceof ParameterizedType) // check if genericSuperclass is ParametrizedType
            {
                Type gType = ((ParameterizedType)genericSuperclass).getActualTypeArguments()[0]; // get First gType
                // check if it is codec
                if (gType instanceof Class && ConfigurationCodec.class.isAssignableFrom((Class<?>)gType))
                {
                    return (Class<Codec>)gType;
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
     * Saves the configuration to the set file.
     */
    public final void save()
    {
        this.save(this.file);
    }

    /**
     * Saves this configuration into the given file
     *
     * @param target the file to save into
     */
    public final void save(File target)
    {
        if (target == null)
        {
            throw new IllegalArgumentException("A configuration cannot be saved without a valid file!");
        }
        try
        {
            this.save(new FileOutputStream(target));
            this.onSaved(file);
        }
        catch (FileNotFoundException ex)
        {
            throw new InvalidConfigurationException("File to save into cannot be accessed!", ex);
        }
    }

    /**
     * Saves this configuration using given OutputStream
     *
     * @param os the OutputStream to write into
     */
    public final void save(OutputStream os)
    {
        this.getCodec().saveConfig(this, os);
    }

    /**
     * Reloads the configuration from file
     * <p>This will only work if the file of the configuration got set previously
     */
    public final void reload()
    {
        this.reload(false);
    }

    /**
     * Reloads the configuration from file
     * <p>This will only work if the file of the configuration got set previously
     *
     * @param save true if the configuration should be saved after loading
     *
     * @return true when a new file got created while saving
     *
     * @throws InvalidConfigurationException if an error occurs while loading
     */
    public final boolean reload(boolean save) throws InvalidConfigurationException
    {
        boolean result = false;
        if (!this.loadFrom(this.file) && save)
        {
            this.factory.logger.info("Saved configuration in new file: " + file.getAbsolutePath());
            result = true;
        }
        if (save)
        {
            this.getDefault().save(this.getFile()); // save the default values
            if (this.getDefault() != this) // If default is another config
            {
                this.reload(false); // reload from saved default
                // TODO set inherited fields? configurable?
            }
        }
        return result;
    }

    /**
     * Loads the configuration using the given File
     * <p>This will NOT set the file of this configuration
     *
     * @param file the file to load from
     *
     * @return true if the configuration was loaded from file
     */
    public final boolean loadFrom(File file)
    {
        if (this.file == null)
        {
            throw new IllegalArgumentException("The file must not be null in order to load the configuration!");
        }
        try
        {
            this.loadFrom(new FileInputStream(this.file));
            this.onLoaded(file);
        }
        catch (FileNotFoundException ex)
        {
            this.factory.logger.log(Level.INFO, "Could not load configuration from file! Using default...");
            return false;
        }
        return true;
    }

    /**
     * Loads the configuration using the given InputStream
     *
     * @param is the InputStream to load from
     */
    public final void loadFrom(InputStream is)
    {
        if (is == null)
        {
            throw new IllegalArgumentException("The input stream must not be null!");
        }
        this.showLoadErrors(this.getCodec().loadConfig(this, is));
    }

    final void showLoadErrors(Collection<ErrorNode> errors)
    {
        if (!errors.isEmpty())
        {
            this.factory.logger.warning(errors.size() + " ErrorNodes were encountered while loading the configuration!");
            for (ErrorNode error : errors)
            {
                this.factory.logger.log(Level.WARNING, error.getErrorMessage());
            }
        }
    }

    /**
     * Returns the Codec
     *
     * @return the ConfigurationCodec defined in the GenericType of the Configuration
     *
     * @throws MissingCodecException when no codec was set via genericType
     */
    public final Codec getCodec() throws MissingCodecException
    {
        if (defaultCodec == null)
        {
            throw new MissingCodecException("Configuration has no Codec set! A configuration needs to have a codec defined in its GenericType");
        }
        return this.factory.getCodecManager().getCodec(this.defaultCodec);
    }

    /**
     * Sets the path to load from
     *
     * @param file the path the configuration will load from
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
     * Returns the path this config will be saved to and loaded from by default
     *
     * @return the path of this config
     */
    public final File getFile()
    {
        return this.file;
    }

    /**
     * This method gets called right after the configuration got loaded.
     */
    public void onLoaded(File loadedFrom)
    {
    }

    /**
     * This method gets called right after the configuration get saved.
     */
    public void onSaved(File savedTo)
    {
    }

    /**
     * Returns the lines to be added in front of the Configuration.
     * <p>not every Codec may use this
     *
     * @return the head
     */
    public String[] head()
    {
        return null;
    }

    /**
     * Returns the lines to be added at the end of the Configuration.
     * <p>not every Codec may use this
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
        if (this.defaultConfig == null || this.defaultConfig == this)
        {
            return; // Default is this config anyways
        }
        this.updateInheritance(this, defaultConfig);
    }

    @SuppressWarnings("unchecked")
    private void updateInheritance(Section section, Section defaultSection)
    {
        try
        {
            for (Field field : section.getClass().getFields())
            {
                if (isConfigField(field))
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
                        throw new IllegalStateException("Collections in child configurations are not allowed!");
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
