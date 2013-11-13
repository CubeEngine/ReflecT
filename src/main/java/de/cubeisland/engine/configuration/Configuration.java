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
import de.cubeisland.engine.configuration.exception.InvalidConfigurationException;
import de.cubeisland.engine.configuration.exception.MissingCodecException;
import de.cubeisland.engine.configuration.node.ErrorNode;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This abstract class represents a configuration.
 */
public abstract class Configuration<Codec extends ConfigurationCodec> implements Section
{
    protected transient static Logger LOGGER = Logger.getLogger(Configuration.class.getName());

    public static void setLogger(Logger logger)
    {
        LOGGER = logger;
    }

    private ConfigurationFactory factory;

    public final void init(ConfigurationFactory factory)
    {
        this.factory = factory;
    }

    private transient Configuration defaultConfig;

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
     * Returns true if exceptions should be rethrown immediately
     * <p>override to change
     *
     * @return whether to rethrow exceptions or log them instead
     */
    public boolean useStrictExceptionPolicy()
    {
        return true;
    }

    private transient final Class<Codec> defaultCodec = getCodecClass(this.getClass());

    /**
     * Saves the fields that got inherited from the parent-configuration
     */
    private transient HashSet<Field> inheritedFields;

    /**
     * Marks a field as being inherited from the parent configuration and thus not being saved
     *
     * @param field the inherited field
     */
    public final void addinheritedField(Field field)
    {
        if (inheritedFields == null)
        {
            throw new IllegalStateException("This is not a child-configuration");
        }
        this.inheritedFields.add(field);
    }

    /**
     * Marks a field as not being inherited from the parent configuration and thus saved into file
     *
     * @param field the not inherited field
     */
    public final void removeInheritedField(Field field)
    {
        if (inheritedFields == null)
        {
            throw new IllegalStateException("This is not a child-configuration");
        }
        this.inheritedFields.remove(field);
    }

    /**
     * Returns whether the given field-value was inherited from a parent-configuration
     *
     * @param field the field to check
     *
     * @return true if the field got inherited
     */
    public final boolean isInheritedField(Field field)
    {
        if (inheritedFields == null)
        {
            throw new IllegalStateException("This is not a child-configuration");
        }
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
        Configuration<Codec> childConfig = factory.create(this.getClass()); // Can throw ConfigurationInstantiationException
        childConfig.setFile(sourceFile);
        childConfig.setDefault(this);
        try
        {
            childConfig.reload(true);
            return (T)childConfig;
        }
        catch (InvalidConfigurationException ex)
        {
            //TODO policy
            return (T) childConfig;
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Unknown Exception while loading ChildConfig!", ex);
        }
    }

    /**
     * Tries to get the CodecClazz of a configuration implementation.
     *
     * @param clazz    the clazz of the configuration
     *
     * @return the Codec
     *
     * @throws InvalidConfigurationException if no Codec was defined through the GenericType
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
                throw new MissingCodecException("Configuration has no Codec set! A configuration needs to have a codec defined in its GenericType");
            }
            if (genericSuperclass instanceof ParameterizedType) // check if genericSuperclass is ParametrizedType
            {
                Type gType = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0]; // get First gType
                if (gType instanceof Class && ConfigurationCodec.class.isAssignableFrom((Class<?>) gType)) // check if it is codec
                {
                    return (Class<Codec>) gType;
                }
            }
            if (genericSuperclass instanceof Class)
            {
                return getCodecClass((Class) genericSuperclass); // lookup next superclass
            }
            throw new IllegalStateException("Unable to get Codec! " + genericSuperclass + " is not a class!");
        }
        catch (InvalidConfigurationException ex)
        {
            throw ex;
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

    protected transient File file;

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
            if (useStrictExceptionPolicy())
            {
                throw new InvalidConfigurationException("File to save into cannot be accessed!", ex);
            }
            LOGGER.log(Level.SEVERE, "File to save into cannot be accessed!", ex);
        }
        catch (IOException ex)
        {
            if (useStrictExceptionPolicy())
            {
                throw new InvalidConfigurationException("Error while saving Configuration!", ex);
            }
            LOGGER.log(Level.SEVERE, "Error while saving Configuration!", ex);
        }
    }

    /**
     * Saves this configuration using given OutputStream
     *
     * @param os the OutputStream to write into
     */
    public final void save(OutputStream os) throws IOException
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
        try
        {
            this.loadFrom(this.file);
        }
        catch (FileNotFoundException ex)
        {
            if (save)
            {
                LOGGER.info("File to load from not found! Creating new File when saving...");
                result = true;
            }
            else
            {
                if (useStrictExceptionPolicy())
                {
                    throw new InvalidConfigurationException("Could not load configuration", ex);
                }
                LOGGER.log(Level.WARNING, "Could not load configuration", ex);
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
        if (this.file == null)
        {
            throw new IllegalArgumentException("The file must not be null in order to load the configuration!");
        }
        this.loadFrom(new FileInputStream(this.file));

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
        try
        {
            this.showLoadErrors(this.getCodec().loadConfig(this, is));//load config in maps -> updates -> sets fields
        }
        catch (RuntimeException e)
        {
            if (!this.useStrictExceptionPolicy() && e instanceof InvalidConfigurationException)
            {
                LOGGER.log(Level.SEVERE, "Could not load configuration from file!", e);
                return;
            }
            throw e;
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (IOException e)
            {
                LOGGER.log(Level.WARNING, "Failed to close InputStream", e);
            }
        }
    }

    final void showLoadErrors(Collection<ErrorNode> errors)
    {
        if (!errors.isEmpty())
        {
            LOGGER.warning(errors.size() + " ErrorNodes were encountered while loading the configuration!");
            for (ErrorNode error : errors)
            {
                LOGGER.log(Level.WARNING, error.getErrorMessage(), error.getExeption());
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
        return this.factory.getCodec(this.defaultCodec);
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
}
