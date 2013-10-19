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
import de.cubeisland.engine.configuration.node.ErrorNode;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Collection;
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
            Collection<ErrorNode> errors;
            try (InputStream is = new FileInputStream(sourcePath.toFile()))
            {
                errors = childConfig.getCodec().loadChildConfig(childConfig, is);
            }
            catch (IOException ignored) // not found load from parent / save child
            {
                errors = childConfig.getCodec().loadChildConfig(childConfig, null);
            }
            if (!errors.isEmpty())
            {
                LOGGER.warning(errors.size() + " ErrorNodes were encountered while loading the configuration!");
                for (ErrorNode error : errors)
                {
                    LOGGER.warning(error.getErrorMessage());
                }
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
