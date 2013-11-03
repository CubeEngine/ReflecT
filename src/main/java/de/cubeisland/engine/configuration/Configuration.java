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
import de.cubeisland.engine.configuration.node.ErrorNode;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * This abstract class represents a configuration.
 */
public abstract class Configuration<Codec extends ConfigurationCodec> implements Section
{
    protected static Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    public static void setLogger(Logger logger)
    {
        LOGGER = logger;
    }

    private Configuration defaultConfig;

    protected Configuration()
    {
        this.defaultConfig = this;
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
        }
        else
        {
            if (!this.getClass().equals(config.getClass()))
            {
                throw new IllegalArgumentException("Parent and child-configuration have to be the same type of configuration!");
            }
            this.defaultConfig = config;
        }
    }

    public static final Convert CONVERTERS = new Convert();

    private final Codec codec = getCodec(this.getClass());

    /**
     * Saves the fields that got inherited from the parent-configuration
     */
    private HashSet<Field> inheritedFields;

    /**
     * Marks a field as being inherited from the parent configuration and thus not being saved
     *
     * @param field the inherited field
     */
    public void addinheritedField(Field field)
    {
        this.inheritedFields.add(field);
    }

    /**
     * Marks a field as not being inherited from the parent configuration and thus saved into file
     *
     * @param field the not inherited field
     */
    public void removeInheritedField(Field field)
    {
        this.inheritedFields.remove(field);
    }

    /**
     * Returns whether the given field-value was inherited from a parent-configuration
     *
     * @param field the field to check
     *
     * @return true if the field got inherited
     */
    public boolean isInheritedField(Field field)
    {
        return inheritedFields.contains(field);
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
        Configuration<Codec> childConfig;
        try
        {
            childConfig = Configuration.create(this.getClass());
            childConfig.inheritedFields = new HashSet<Field>();
            childConfig.setFile(sourceFile);
            childConfig.setDefault(this);
            childConfig.reload(true);
            return (T)childConfig;
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Could not load ChildConfig!", ex);
        }
    }

    /**
     * Tries to get the Codec of a configuration implementation.
     *
     * @param clazz    the clazz of the configuration
     * @param <C>      the CodecType
     *
     * @return the Codec
     *
     * @throws InvalidConfigurationException if no Codec was defined through the GenericType
     */
    @SuppressWarnings("unchecked")
    private static <C extends ConfigurationCodec> C getCodec(Class clazz)
    {
        Type genericSuperclass = clazz.getGenericSuperclass(); // Get generic superclass
        Class<C> codecClass = null;
        try
        {
            if (genericSuperclass.equals(Configuration.class))
            {
                // superclass is this class -> No Codec set as GenericType
                throw new InvalidConfigurationException("Configuration has no Codec set! A configuration needs to have a codec defined in its GenericType");
            }
            else if (genericSuperclass instanceof ParameterizedType) // check if genericsuperclass is ParamereizedType
            {
                codecClass = (Class<C>)((ParameterizedType)genericSuperclass).getActualTypeArguments()[0]; // Get Type
                return codecClass.newInstance(); // Create Instance
            }
            // else lookup next superclass
        }
        catch (ClassCastException e) // Somehow the configuration has a GenericType that is not a Codec
        {
            if (!(genericSuperclass instanceof Class))
            {
                throw new IllegalStateException("Something went wrong!", e);
            }
        }
        catch (Exception ex)
        {
            throw new InvalidConfigurationException("Could not instantiate the Codec! " + codecClass, ex);
        }
        return getCodec((Class<? extends Configuration>)genericSuperclass); // Lookup next superclass
    }

    protected File file;

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
        catch (IOException ex)
        {
            throw new InvalidConfigurationException("Error while saving Configuration!", ex);
        }
    }

    /**
     * Saves this configuration using given OutputStream
     *
     * @param os the OutputStream to write into
     */
    public final void save(OutputStream os) throws IOException
    {
        this.codec.saveConfig(this, os);
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
        if (this.file == null)
        {
            throw new IllegalArgumentException("The file must not be null in order to load the configuration!");
        }
        boolean result = false;
        try
        {
            this.loadFrom(this.file);
        }
        catch (FileNotFoundException e)
        {
            if (save)
            {
                result = true;
            }
            else
            {

            }
        }
        if (save)
        {
            this.save();
        }
        return result;
    }

    /**
     * Loads the configuration using the given File
     * <p>This will NOT set the file of this configuration
     *
     * @param file the file to load from
     */
    public final void loadFrom(File file) throws FileNotFoundException
    {
        InputStream is = new FileInputStream(this.file);
        try
        {
            this.loadFrom(is);
        }
        catch (RuntimeException e)
        {
            throw new InvalidConfigurationException("Could not load configuration from file!", e);
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (IOException ignored)
            {}
        }
        this.onLoaded(file);
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
        this.showLoadErrors(this.codec.loadConfig(this, is));//load config in maps -> updates -> sets fields
    }

    final void showLoadErrors(Collection<ErrorNode> errors)
    {
        if (!errors.isEmpty())
        {
            LOGGER.warning(errors.size() + " ErrorNodes were encountered while loading the configuration!");
            for (ErrorNode error : errors)
            {
                LOGGER.warning(error.getErrorMessage());
            }
        }
    }

    /**
     * Returns the Codec
     *
     * @return the ConfigurationCodec defined in the GenericType of the Configuration
     */
    public final Codec getCodec()
    {
        return this.codec;
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
    {}

    /**
     * This method gets called right after the configuration get saved.
     */
    public void onSaved(File savedTo)
    {}

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

    /**
     * Creates an instance of this given configuration-class.
     * <p>The configuration has to have the default Constructor for this to work!
     *
     * @param clazz the configurations class
     * @param <T>   The Type of the returned configuration
     *
     * @return the created configuration
     */
    public static <T extends Configuration> T create(Class<T> clazz)
    {
        try
        {
            return clazz.newInstance();
        }
        catch (Exception e)
        {
            throw new InvalidConfigurationException("Failed to create an instance of " + clazz.getName(), e);
        }
    }

    /**
     * Loads the configuration from given file and optionally saves it afterwards
     *
     * @param clazz the configurations class
     * @param file  the file to load from and save to
     * @param save  whether to save the configuration or not
     *
     * @return the loaded Configuration
     */
    public static <T extends Configuration> T load(Class<T> clazz, File file, boolean save)
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
    public static <T extends Configuration> T load(Class<T> clazz, File file)
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
    public static <T extends Configuration> T load(Class<T> clazz, InputStream is)
    {
        T config = create(clazz);
        config.loadFrom(is);
        return config;
    }
}
