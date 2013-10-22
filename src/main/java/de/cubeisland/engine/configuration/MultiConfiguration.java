/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Anselm Brehme
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

import de.cubeisland.engine.configuration.codec.MultiConfigurationCodec;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
     * Saves the fields that got inherited from the parent-configuration
     */
    private HashSet<Field> inheritedFields;

    /**
     * Saves this configuration as a child-configuration of the set parent-configuration
     */
    private final void saveChild()
    {
        this.saveChild(this.getPath());
    }

    private final void saveChild(Path target)
    {
        if (this.getCodec() == null)
        {
            throw new IllegalStateException("A configuration cannot be saved without a valid Codec!");
        }
        this.getCodec().saveChildConfig(this.parent, this, target);
        this.onSaved(target);
    }

    /**
     * Loads and saves a child-configuration from given path
     *
     * @param sourcePath the path to the file
     * @param <T> the ConfigurationType
     * @return the loaded child-configuration
     */
    @SuppressWarnings("unchecked")
    public <T extends MultiConfiguration> T loadChild(Path sourcePath)
    {
        MultiConfiguration<ConfigCodec> childConfig;
        try
        {
            childConfig = Configuration.create(this.getClass());
            childConfig.inheritedFields = new HashSet<>();
            childConfig.setPath(sourcePath);
            childConfig.setParentConfiguration(this);
            childConfig.reload(true);
            return (T)childConfig;
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Could not load ChildConfig!", ex);
        }
    }

    /**
     * Sets the parent-configuration for this configuration
     * <p>if the parent-configuration is null
     *
     * @param parentConfiguration the parent-configuration (can be null)
     */
    public void setParentConfiguration(MultiConfiguration parentConfiguration)
    {
        if (parentConfiguration != null)
        {
            if (!this.getClass().equals(parentConfiguration.getClass()))
            {
                throw new IllegalArgumentException("Parent and child-configuration have to be the same type of configuration!");
            }
        }
        this.parent = parentConfiguration;
    }

    @Override
    public boolean reload(boolean save) throws InvalidConfigurationException
    {
        if (this.isChildConfiguration()) // This is a child-config!
        {
            boolean result = false;
            try (InputStream is = new FileInputStream(this.getPath().toFile()))
            {
                this.showLoadErrors(this.getCodec().loadChildConfig(this, is));
            }
            catch (FileNotFoundException ex) // file not found load from parent & save child
            {
                result = true;
                this.showLoadErrors(this.getCodec().loadChildConfig(this, null));
            }
            catch (Exception ex)
            {
                throw new InvalidConfigurationException("Could not load configuration from file!", ex);
            }
            this.onLoaded(this.getPath());
            if (save)
            {
                this.saveChild();
            }
            return result;
        }
        else
        {
            return super.reload(save);
        }
    }

    @Override
    public void save(Path target)
    {
        if (this.isChildConfiguration())
        {
            this.saveChild(target);
        }
        else
        {
            super.save(target);
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

    /**
     * Returns true if this configuration has a parent-configuration set
     *
     * @return true if this configuration has a parent-configuration set
     */
    public boolean isChildConfiguration()
    {
        return this.parent == null;
    }
}
