package de.cubeisland.engine.configuration;

import de.cubeisland.engine.configuration.codec.MultiConfigurationCodec;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashSet;

/**
 * This Configuration can have child-configs.
 * A child config will ignore default-set data and instead use the data saved in the parent config
 * Any data set in the configuration file will stay in the file. Even if the data is equal to the parents data.
 */
public class MultiConfiguration<ConfigCodec extends MultiConfigurationCodec> extends Configuration<ConfigCodec>
{
    protected MultiConfiguration parent = null;

    /**
     * Saves this configuration as a child-configuration of the set parent-configuration
     */
    public final void saveChild()
    {
        if (this.codec == null)
        {
            throw new IllegalStateException("A configuration cannot be saved without a valid de.cubeisland.engine.configuration.codec!");
        }
        if (this.file == null)
        {
            throw new IllegalStateException("A configuration cannot be saved without a valid file!");
        }
        this.codec.saveChildConfig(this.parent, this, this.file);
        this.onSaved(this.file);
    }

    /**
     * Loads and saves a child-configuration from given path
     *
     * @param sourcePath the path to the file
     * @param <T> the ConfigurationType
     * @return the loaded child-configuration
     */
    @SuppressWarnings("unchecked")
    public <T extends Configuration> T loadChild(Path sourcePath) //and save
    {
        MultiConfiguration<ConfigCodec> childConfig;
        try
        {
            childConfig = this.getClass().newInstance();
            childConfig.inheritedFields = new HashSet<>();
            childConfig.setPath(sourcePath);
            childConfig.parent = this;
            try (InputStream is = new FileInputStream(sourcePath.toFile()))
            {
                childConfig.getCodec().loadChildConfig(childConfig, is);
            }
            catch (IOException ignored) // not found load from parent / save child
            {
                childConfig.getCodec().loadChildConfig(childConfig, null);
            }
            childConfig.onLoaded(file);
            childConfig.saveChild();
            return (T)childConfig;
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Could not load ChildConfig!", ex);
        }
    }

    /**
     * Gets the parent-configuration if given
     *
     * @return the parent-configuration or null
     */
    public MultiConfiguration getParent()
    {
        return parent;
    }

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
     * Returns whether the given field-value was inherited from a parent-configuration
     *
     * @param field the field to check
     * @return true if the field got inherited
     */
    public boolean isInheritedField(Field field)
    {
        return inheritedFields.contains(field);
    }
}
