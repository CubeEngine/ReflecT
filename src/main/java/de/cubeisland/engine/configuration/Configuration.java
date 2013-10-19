package de.cubeisland.engine.configuration;

import de.cubeisland.engine.configuration.codec.ConfigurationCodec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;

/**
 * This abstract class represents a configuration.
 */
public abstract class Configuration<Codec extends ConfigurationCodec>
{
    public final Codec codec;
    protected Path file;

    public Configuration()
    {
        this.codec = getCodec(this.getClass());
    }

    /**
     * Tries to get the Codec of a configuration implementation.
     *
     * @param clazz the clazz of the configuration
     * @param <C> the CodecType
     * @param <Config> the ConfigType
     * @return the Codec
     *
     * @throws InvalidConfigurationException if no Codec was defined through the GenericType
     */
    @SuppressWarnings("unchecked cast")
    private static <C extends ConfigurationCodec, Config extends Configuration> C getCodec(Class<Config> clazz)
    {
        Type genericSuperclass = clazz.getGenericSuperclass(); // Get genegeric superclass
        Class<C> codecClass = null;
        try
        {
            if (genericSuperclass.equals(Configuration.class))
            {
                // superclass is this class -> No Codec set as GenericType
                throw new InvalidConfigurationException("Configuration has no Codec set! A configuration needs to have a coded defined in its GenericType");
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
        catch (ReflectiveOperationException ex)
        {
            throw new InvalidConfigurationException("Could not instantiate the Codec! " + codecClass, ex);
        }
        return getCodec((Class<? extends Configuration>)genericSuperclass); // Lookup next superclass
    }

    /**
     * Saves this configuration in a file for given Path
     *
     * @param target the Path to the file to save into
     */
    public final void save(Path target)
    {
        if (target == null)
        {
            throw new IllegalArgumentException("A configuration cannot be saved without a valid file!");
        }
        this.codec.save(this, target);
        this.onSaved(target);
    }

    /**
     * Reloads the configuration from file
     * <p>This will only work if the file of the configuration got set previously (usually through loading from file)
     */
    public void reload()
    {
        this.reload(false);
    }

    /**
     * Reloads the configuration from file
     * <p>This will only work if the file of the configuration got set previously (usually through loading from file)
     *
     * @param save true if the configuration should be saved after loading
     *
     * @return true when a new file got created while saving
     *
     * @throws InvalidConfigurationException if an error occurs while loading
     */
    public boolean reload(boolean save) throws InvalidConfigurationException
    {
        if (this.file == null)
        {
            throw new IllegalArgumentException("The file must not be null in order to load the configuration!");
        }
        boolean result = false;
        try (InputStream is = new FileInputStream(this.file.toFile()))
        {
            this.loadFrom(is);
        }
        catch (FileNotFoundException e)
        {
            if (save)
            {
                result = true;
            }
            else
            {
                throw new InvalidConfigurationException("Could not load configuration from file!", e);
            }
        }
        catch (Exception e)
        {
            throw new InvalidConfigurationException("Could not load configuration from file!", e);
        }
        if (save)
        {
            this.save();
        }
        return result;
    }

    /**
     * Loads the configuration using the given InputStream
     *
     * @param is the InputStream to load from
     */
    public void loadFrom(InputStream is)
    {
        assert is != null : "You hae to provide a InputStream to load from";
        this.codec.load(this, is); //load config in maps -> updates -> sets fields
        this.onLoaded(file);
    }

    /**
     * Saves the configuration to the set file.
     */
    public final void save()
    {
        this.save(this.file);
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
     * @param path the path the configuration will load from
     */
    public final void setPath(Path path)
    {
        assert path != null: "The file must not be null!";
        this.file = path;
    }

    /**
     * Returns the path this config will be saved to and loaded from by default
     *
     * @return the path of this config
     */
    public final Path getPath()
    {
        return this.file;
    }

    /**
     * This method gets called right after the configuration got loaded.
     */
    public void onLoaded(Path loadedFrom)
    {}

    /**
     * This method gets called right after the configuration get saved.
     */
    public void onSaved(Path savedTo)
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
     * @param <T> The Type of the returned configuration
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
     * Loads the configuration from given path and optionally saves it afterwards
     *
     * @param clazz the configurations class
     * @param path the path to load from and save to
     * @param save whether to save the configuration or not
     * @return the loaded Configuration
     */
    public static <T extends Configuration> T load(Class<T> clazz, Path path, boolean save)
    {
        return load(clazz, path.toFile(), save);
    }

    /**
     * Loads the configuration from given path and saves it afterwards
     *
     * @param clazz the configurations class
     * @param path the path to load from and save to
     * @return the loaded Configuration
     */
    public static <T extends Configuration> T load(Class<T> clazz, Path path)
    {
        return load(clazz, path, true);
    }

    /**
     * Loads the configuration from given file and optionally saves it afterwards
     *
     * @param clazz the configurations class
     * @param file the file to load from and save to
     * @param save whether to save the configuration or not
     * @return the loaded Configuration
     */
    public static <T extends Configuration> T load(Class<T> clazz, File file, boolean save)
    {
        T config = create(clazz); // loading
        config.file = file.toPath(); // IMPORTANT TO SET BEFORE LOADING!
        config.reload(save);
        return config;
    }

    /**
     * Loads the configuration from given file and saves it afterwards
     *
     * @param clazz the configurations class
     * @param file the file to load from and save to
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
     * @param is the InputStream to load from
     * @return the loaded configuration
     */
    public static <T extends Configuration> T load(Class<T> clazz, InputStream is)
    {
        T config = create(clazz);
        config.loadFrom(is);
        return config;
    }
}
