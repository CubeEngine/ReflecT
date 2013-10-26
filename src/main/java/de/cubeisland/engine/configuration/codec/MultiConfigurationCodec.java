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
package de.cubeisland.engine.configuration.codec;

import de.cubeisland.engine.configuration.FieldType;
import de.cubeisland.engine.configuration.InvalidConfigurationException;
import de.cubeisland.engine.configuration.MultiConfiguration;
import de.cubeisland.engine.configuration.Section;
import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.convert.converter.generic.MapConverter;
import de.cubeisland.engine.configuration.node.ErrorNode;
import de.cubeisland.engine.configuration.node.MapNode;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.NullNode;
import de.cubeisland.engine.configuration.node.StringNode;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import static de.cubeisland.engine.configuration.Configuration.convertFromNode;
import static de.cubeisland.engine.configuration.Configuration.convertToNode;

/**
 * This abstract Codec can be implemented to read and write configurations that allow child-configs
 */
public abstract class MultiConfigurationCodec extends ConfigurationCodec
{
    /**
     * Saves a configuration with another configuration as parent
     *
     * @param parentConfig the parent configuration
     * @param config       the configuration to save
     * @param file         the file to save into
     */
    public void saveChildConfig(MultiConfiguration parentConfig, MultiConfiguration config, File file)
    {
        try
        {
            if (file == null)
            {
                throw new IllegalStateException("Tried to save config without File.");
            }
            this.saveIntoFile(config, this.convertSection(parentConfig, config, config), file);
        }
        catch (Exception ex)
        {
            throw new InvalidConfigurationException("Error while saving Configuration!", ex);
        }
    }

    /**
     *
     *
     * @param config the config to load
     * @param is the InputStream to load from
     */

    /**
     * Loads in the given configuration using the InputStream
     *
     * @param config the MultiConfiguration to load
     * @param is     the InputStream to load from
     *
     * @return a collection of all erroneous Nodes
     */
    public Collection<ErrorNode> loadChildConfig(MultiConfiguration config, InputStream is)
    {
        return this.dumpIntoSection(config.getParent(), config, this.loadFromInputStream(is), config);
    }

    /**
     * Dumps the contents of the MapNode into the fields of the section using the parentSection as backup if a node is not given
     *
     * @param parentSection the parent configuration-section
     * @param section       the configuration-section
     * @param currentNode   the Node to load from
     * @param config        the MultiConfiguration containing this section
     *
     * @return a collection of all erroneous Nodes
     */

    protected Collection<ErrorNode> dumpIntoSection(Section parentSection, Section section, MapNode currentNode, MultiConfiguration config)
    {
        if (parentSection == null) // Not a child Config! Use default behaviour
        {
            return this.dumpIntoSection(section, currentNode);
        }
        if (!parentSection.getClass().equals(section.getClass()))
        {
            throw new IllegalArgumentException("Parent and child-section have to be the same type of section!");
        }
        Collection<ErrorNode> errorNodes = new HashSet<ErrorNode>();
        for (Field field : section.getClass().getFields()) // ONLY public fields are allowed
        {
            if (isConfigField(field))
            {
                Node fieldNode = currentNode.getNodeAt(this.getPathFor(field));
                if (fieldNode instanceof ErrorNode)
                {
                    errorNodes.add((ErrorNode)fieldNode);
                }
                else
                {
                    try
                    {
                        if (fieldNode instanceof NullNode)
                        {
                            errorNodes.addAll(this.dumpParentIntoField(parentSection, section, field));
                            config.addinheritedField(field);
                        }
                        else
                        {
                            errorNodes.addAll(this.dumpIntoField(parentSection, section, field, fieldNode, config));
                        }
                    }
                    catch (Exception e)
                    {
                        throw InvalidConfigurationException.of("Error while dumping loaded config into fields!", this.getPathFor(field), section.getClass(), field, e);
                    }
                }
            }
        }
        return errorNodes;
    }

    /**
     * Copy the contents of the parent section into a field of the section
     *
     * @param parentSection the parent configuration-section
     * @param section       the configuration-section
     * @param field         the Field to load into
     *
     * @return a collection of all erroneous Nodes
     */
    protected Collection<ErrorNode> dumpParentIntoField(Section parentSection, Section section, Field field) throws ConversionException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException
    {
        if (getFieldType(field) == FieldType.SECTION_COLLECTION)
        {
            throw new InvalidConfigurationException("Child-Configurations are not allowed for Sections in Collections");
        }
        return this.dumpIntoField(section, field, this.convertField(field, parentSection)); // convert parent in node and dump back in
    }

    /**
     * Dumps the contents of the Node into a field of the section
     *
     * @param parentSection the parent configuration-section
     * @param section       the configuration-section
     * @param field         the Field to load into
     * @param fieldNode     the Node to load from
     * @param config        the MultiConfiguration containing this section
     *
     * @return a collection of all erroneous Nodes
     */
    @SuppressWarnings("unchecked")
    protected Collection<ErrorNode> dumpIntoField(Section parentSection, Section section, Field field, Node fieldNode, MultiConfiguration config) throws ConversionException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException
    {
        Collection<ErrorNode> errorNodes = new HashSet<ErrorNode>();
        Type type = field.getGenericType();
        FieldType fieldType = getFieldType(field);
        Object fieldValue = null;
        switch (fieldType)
        {
            case NORMAL:
                fieldValue = convertFromNode(fieldNode, type); // Convert the value
                if (fieldValue == null)
                {
                    fieldValue = field.get(parentSection);
                    config.addinheritedField(field);
                }
                break;
            case SECTION:
                fieldValue = newSectionInstance(section, (Class<? extends Section>)field.getType());
                if (fieldNode instanceof MapNode)
                {
                    errorNodes.addAll(this.dumpIntoSection((Section)field.get(parentSection), (Section)fieldValue, (MapNode)fieldNode, config));
                }
                else
                {
                    throw new InvalidConfigurationException("Node for Section is not a MapNode!\n" + fieldNode);
                }
                break;
            case SECTION_COLLECTION:
                throw new InvalidConfigurationException("Child-Configurations are not allowed for Sections in Collections");
            case SECTION_MAP:
                if (fieldNode instanceof MapNode)
                {
                    fieldValue = MapConverter.getMapFor((ParameterizedType)type);
                    if (((MapNode)fieldNode).isEmpty()) // No values set => load from parent-section
                    {
                        break;
                    } // else load values for child-section using parent-section as backup
                    Map<Object, Section> mappedParentSections = (Map<Object, Section>)field.get(parentSection);
                    Class<? extends Section> subSectionClass = (Class<? extends Section>)((ParameterizedType)type).getActualTypeArguments()[1];
                    for (Map.Entry<String, Node> entry : ((MapNode)fieldNode).getMappedNodes().entrySet())
                    {
                        Object key = convertFromNode(StringNode.of(entry.getKey()), ((ParameterizedType)type).getActualTypeArguments()[0]);
                        Section value = newSectionInstance(section, subSectionClass);
                        if (entry.getValue() instanceof NullNode)
                        {
                            errorNodes.addAll(this.dumpIntoSection(parentSection, section, MapNode.emptyMap(), config));
                        }
                        else if (entry.getValue() instanceof MapNode)
                        {
                            errorNodes.addAll(this.dumpIntoSection(mappedParentSections.get(key), value, (MapNode)entry.getValue(), config));
                        }
                        else
                        {
                            throw new InvalidConfigurationException("Value-Node for mapped Section is not a MapNode!\n" + entry.getValue());
                        }
                        ((Map<Object, Section>)fieldValue).put(key, value);
                    }
                }
                else
                {
                    throw new InvalidConfigurationException("Node for mapped Sections is not a MapNode!\n" + fieldNode);
                }
        }
        field.set(section, fieldValue);
        return errorNodes;
    }

    /**
     * Fills the map with values from the Fields to save
     *
     * @param parentSection the parent config
     * @param section       the config
     */
    @SuppressWarnings("unchecked")
    public MapNode convertSection(Section parentSection, Section section, MultiConfiguration config)
    {
        MapNode baseNode = MapNode.emptyMap();
        if (parentSection == null)
        {
            return this.convertSection(section);
        }
        if (!parentSection.getClass().equals(section.getClass()))
        {
            throw new IllegalStateException("Parent and child-section have to be the same type of section!");
        }
        Class<? extends Section> configClass = section.getClass();
        for (Field field : configClass.getFields())
        {
            if (config.isInheritedField(field))
            {
                continue;
            }
            if (isConfigField(field))
            {
                baseNode.setNodeAt(this.getPathFor(field), this.convertField(field, parentSection, section, config));
            }
        }
        baseNode.cleanUpEmptyNodes();
        return baseNode;
    }

    @SuppressWarnings("unchecked")
    protected Node convertField(Field field, Section parentSection, Section section, MultiConfiguration config)
    {
        try
        {
            Object fieldValue = field.get(section);
            FieldType fieldType = getFieldType(field);
            Node node = null;
            switch (fieldType)
            {
                case NORMAL:
                    node = convertToNode(fieldValue);
                    break;
                case SECTION:
                    node = this.convertSection((Section)field.get(parentSection), (Section)fieldValue, config);
                    break;
                case SECTION_COLLECTION:
                    throw InvalidConfigurationException.of("Child-Configurations are not allowed for Sections in Collections", this.getPathFor(field), section.getClass(), field, null);
                case SECTION_MAP:
                    node = MapNode.emptyMap();
                    Map<Object, Section> parentFieldMap = (Map<Object, Section>)field.get(parentSection);
                    Map<Object, Section> childFieldMap = MapConverter.getMapFor((ParameterizedType)field.getGenericType());
                    for (Map.Entry<Object, Section> parentEntry : parentFieldMap.entrySet())
                    {
                        Node keyNode = convertToNode(parentEntry.getKey());
                        if (keyNode instanceof StringNode)
                        {
                            MapNode configNode = this.convertSection(parentEntry.getValue(), childFieldMap.get(parentEntry.getKey()), config);
                            ((MapNode)node).setNode((StringNode)keyNode, configNode);
                        }
                        else
                        {
                            throw InvalidConfigurationException.of("Invalid Key-Node for mapped Section at", this.getPathFor(field), section.getClass(), field, null);
                        }
                    }
            }
            this.addComment(node, field);
            return node;
        }
        catch (Exception e)
        {
            throw InvalidConfigurationException.of("Error while dumping loaded config into fields!", this.getPathFor(field), section.getClass(), field, e);
        }
    }
}
