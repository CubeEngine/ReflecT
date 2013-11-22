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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;

import de.cubeisland.engine.configuration.Configuration;
import de.cubeisland.engine.configuration.FieldType;
import de.cubeisland.engine.configuration.Section;
import de.cubeisland.engine.configuration.SectionFactory;
import de.cubeisland.engine.configuration.StringUtils;
import de.cubeisland.engine.configuration.annotations.Comment;
import de.cubeisland.engine.configuration.annotations.Name;
import de.cubeisland.engine.configuration.convert.converter.generic.CollectionConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.MapConverter;
import de.cubeisland.engine.configuration.exception.CodecIOException;
import de.cubeisland.engine.configuration.exception.ConversionException;
import de.cubeisland.engine.configuration.exception.FieldAccessException;
import de.cubeisland.engine.configuration.exception.InvalidConfigurationException;
import de.cubeisland.engine.configuration.exception.UnsupportedConfigurationException;
import de.cubeisland.engine.configuration.node.ConfigPath;
import de.cubeisland.engine.configuration.node.ErrorNode;
import de.cubeisland.engine.configuration.node.ListNode;
import de.cubeisland.engine.configuration.node.MapNode;
import de.cubeisland.engine.configuration.node.Node;
import de.cubeisland.engine.configuration.node.NullNode;
import de.cubeisland.engine.configuration.node.StringNode;

import static de.cubeisland.engine.configuration.FieldType.*;

/**
 * This abstract Codec can be implemented to read and write configurations that allow child-configs
 */
public abstract class ConfigurationCodec
{
    private ConverterManager converterManager = null;

    /**
     * Called via registering with the CodecManager
     */
    final void setConverterManager(ConverterManager converterManager)
    {
        this.converterManager = converterManager;
    }

    // PUBLIC FINAL API Methods

    /**
     * Returns the <code>ConverterManager</code> for this codec, allowing to register custom <code>Converter</code> for this codec only
     *
     * @return the ConverterManager
     *
     * @throws IllegalStateException if the Codec was not instantiated by the Factory
     */
    public final ConverterManager getConverterManager()
    {
        if (converterManager == null)
        {
            throw new UnsupportedOperationException("This codec is not registered in the CodecManager and therefor has no ConverterManager for its own converters");
        }
        return converterManager;
    }

    /**
     * Loads in the given <code>Configuration</code> using the <code>InputStream</code>
     * <p>if the configuration has no default set it will be saved normally!
     *
     * @param config the Configuration to load
     * @param is     the InputStream to load from
     *
     * @return a collection of all erroneous Nodes
     */
    public final Collection<ErrorNode> loadConfig(Configuration config, InputStream is)
    {
        try
        {
            return dumpIntoSection(config.getDefault(), config, this.load(is, config), config);
        }
        catch (ConversionException ex)
        {
            if (config.useStrictExceptionPolicy())
            {
                throw new CodecIOException("Could not load configuration", ex);
            }
            config.getLogger().warning("Could not load configuration" + ex);
            return Collections.emptyList();
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (IOException e)
            {
                config.getLogger().log(Level.WARNING, "Failed to close InputStream", e);
            }
        }
    }

    /**
     * Saves the <code>Configuration</code> using given <code>OutputStream</code>
     *
     * @param config the Configuration to save
     * @param os     the OutputStream to save into
     */
    public final void saveConfig(Configuration config, OutputStream os)
    {
        try
        {
            this.save(convertSection(config.getDefault(), config, config), os, config);
        }
        catch (ConversionException ex)
        {
            if (config.useStrictExceptionPolicy())
            {
                throw new CodecIOException("Could not save configuration", ex);
            }
            config.getLogger().warning("Could not save configuration" + ex);
        }
        finally
        {
            try
            {
                os.close();
            }
            catch (IOException e)
            {
                config.getLogger().log(Level.WARNING, "Failed to close OutputStream", e);
            }
        }
    }

    /**
     * Returns the FileExtension as String
     *
     * @return the fileExtension
     */
    public abstract String getExtension();

    // PROTECTED ABSTRACT Methods

    /**
     * Saves the values contained in the <code>MapNode</code> using given <code>OutputStream</code>
     *
     * @param node   the Node containing all data to save
     * @param os     the File to save into
     * @param config the Configuration
     */
    protected abstract void save(MapNode node, OutputStream os, Configuration config) throws ConversionException;

    /**
     * Converts the <code>InputStream</code> into a <code>MapNode</code>
     *
     * @param is     the InputStream to load from
     * @param config the Configuration
     */
    protected abstract MapNode load(InputStream is, Configuration config) throws ConversionException;

    // Configuration loading Methods

    /**
     * Dumps the contents of the MapNode into the fields of the section using the defaultSection as default if a node is not given
     *
     * @param defaultSection the parent configuration-section
     * @param section        the configuration-section
     * @param currentNode    the Node to load from
     * @param config         the MultiConfiguration containing this section
     *
     * @return a collection of all erroneous Nodes
     */
    private Collection<ErrorNode> dumpIntoSection(Section defaultSection, Section section, MapNode currentNode, Configuration config)
    {
        if (defaultSection == null) // Special case for Section in Maps
        {
            defaultSection = section;
        }
        if (!defaultSection.getClass().equals(section.getClass()))
        {
            throw new IllegalArgumentException("defaultSection and section have to be the same type of section!");
        }
        Collection<ErrorNode> errorNodes = new HashSet<ErrorNode>();
        for (Field field : section.getClass().getFields()) // ONLY public fields are allowed
        {
            if (isConfigField(field))
            {
                Node fieldNode = currentNode.getNodeAt(getPathFor(field));
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
                            errorNodes.addAll(dumpDefaultIntoField(defaultSection, section, field, config));
                            if (section != defaultSection)
                            {
                                config.addInheritedField(field);
                            }
                        }
                        else
                        {
                            errorNodes.addAll(dumpIntoField(defaultSection, section, field, fieldNode, config));
                        }
                    }
                    catch (InvalidConfigurationException e) // rethrow
                    {
                        throw e;
                    }
                    catch (IllegalAccessException e)
                    {
                        throw FieldAccessException.of(getPathFor(field), section.getClass(), field, e);
                    }
                    catch (ConversionException e) // non-fatal ConversionException
                    {
                        InvalidConfigurationException ex = InvalidConfigurationException.of("Error while converting Node to dump into field!", getPathFor(field), section.getClass(), field, e);
                        if (config.useStrictExceptionPolicy())
                        {
                            throw ex;
                        }
                        config.getLogger().log(Level.WARNING, ex.getMessage(), ex);
                    }
                    catch (RuntimeException e)
                    {
                        throw InvalidConfigurationException.of("Unknown Error while dumping loaded config into fields", getPathFor(field), section.getClass(), field, e);
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
    @SuppressWarnings("unchecked")
    private Collection<ErrorNode> dumpDefaultIntoField(Section parentSection, Section section, Field field, Configuration config) throws ConversionException, IllegalAccessException
    {
        if (parentSection != section)
        {
            if (getFieldType(field) == FieldType.SECTION_COLLECTION)
            {
                throw new UnsupportedConfigurationException("Child-Configurations are not allowed for Sections in Collections");
            }
        }
        return dumpIntoField(parentSection, section, field, convertField(field, parentSection, parentSection, config), config); // convert parent in node and dump back in
    }

    /**
     * Dumps the contents of the Node into a field of the section
     *
     * @param defaultSection the parent configuration-section
     * @param section        the configuration-section
     * @param field          the Field to load into
     * @param fieldNode      the Node to load from
     * @param config         the MultiConfiguration containing this section
     *
     * @return a collection of all erroneous Nodes
     */
    @SuppressWarnings("unchecked")
    private Collection<ErrorNode> dumpIntoField(Section defaultSection, Section section, Field field, Node fieldNode, Configuration config) throws ConversionException, IllegalAccessException
    {
        Collection<ErrorNode> errorNodes = new HashSet<ErrorNode>();
        Type type = field.getGenericType();
        FieldType fieldType = getFieldType(field);
        Object fieldValue = null;
        Object defaultValue = field.get(defaultSection);
        switch (fieldType)
        {
        case NORMAL:
            fieldValue = converterManager.convertFromNode(fieldNode, type); // Convert the value
            if (fieldValue == null && !(section == defaultSection))
            {
                fieldValue = field.get(defaultSection);
                config.addInheritedField(field);
            }
            break;
        case SECTION:
            if (fieldNode instanceof MapNode)
            {
                fieldValue = SectionFactory.newSectionInstance((Class<? extends Section>)field.getType(), section);
                if (defaultValue == null)
                {
                    if (section == defaultSection)
                    {
                        defaultValue = fieldValue;
                    }
                    else
                    {
                        defaultValue = SectionFactory.newSectionInstance((Class<? extends Section>)field
                            .getType(), defaultSection);
                    }
                }
                errorNodes.addAll(dumpIntoSection((Section)defaultValue, (Section)fieldValue, (MapNode)fieldNode, config));
            }
            else
            {
                throw ConversionException.of(this, fieldNode, "Node for Section is not a MapNode!");
            }
            break;
        case SECTION_COLLECTION:
            if (section != defaultSection)
            {
                throw new UnsupportedConfigurationException("Child-Configurations are not allowed for Sections in Collections");
            }
            if (fieldNode instanceof ListNode)
            {
                fieldValue = CollectionConverter.getCollectionFor((ParameterizedType)type);
                if (((ListNode)fieldNode).isEmpty())
                {
                    break;
                }
                Class<? extends Section> subSectionClass = (Class<? extends Section>)((ParameterizedType)type).getActualTypeArguments()[0];
                for (Node listedNode : ((ListNode)fieldNode).getListedNodes())
                {
                    if (listedNode instanceof NullNode)
                    {
                        listedNode = MapNode.emptyMap();
                    }
                    if (listedNode instanceof MapNode)
                    {
                        Section subSection = SectionFactory.newSectionInstance(subSectionClass, section);
                        errorNodes.addAll(dumpIntoSection(subSection, subSection, (MapNode)listedNode, config));
                        ((Collection<Section>)fieldValue).add(subSection);
                    }
                    else
                    {
                        throw ConversionException.of(this, listedNode, "Node for listed Section is not a MapNode!");
                    }
                }
            }
            else
            {
                throw ConversionException.of(this, fieldNode, "\"Node for listed Sections is not a ListNode!");
            }
            break;
        case SECTION_MAP:
            if (fieldNode instanceof MapNode)
            {
                fieldValue = MapConverter.getMapFor((ParameterizedType)type);
                if (((MapNode)fieldNode).isEmpty())
                {
                    if (((MapNode)fieldNode).isEmpty())
                    {
                        break;
                    }
                }
                Map<Object, Section> mappedParentSections = (Map<Object, Section>)field.get(defaultSection);
                Class<? extends Section> subSectionClass = (Class<? extends Section>)((ParameterizedType)type).getActualTypeArguments()[1];
                for (Map.Entry<String, Node> entry : ((MapNode)fieldNode).getMappedNodes().entrySet())
                {
                    Object key = converterManager.convertFromNode(StringNode
                                                                      .of(entry.getKey()), ((ParameterizedType)type)
                                                                      .getActualTypeArguments()[0]);
                    Section value = SectionFactory.newSectionInstance(subSectionClass, section);
                    if (entry.getValue() instanceof NullNode)
                    {
                        errorNodes.addAll(dumpIntoSection(defaultSection, section, MapNode.emptyMap(), config));
                    }
                    else if (entry.getValue() instanceof MapNode)
                    {
                        errorNodes.addAll(dumpIntoSection(mappedParentSections.get(key), value, (MapNode)entry.getValue(), config));
                    }
                    else
                    {
                        throw ConversionException.of(this, entry.getValue(), "Value-Node for mapped Section is not a MapNode!");
                    }
                    ((Map<Object, Section>)fieldValue).put(key, value);
                }
            }
            else
            {
                throw ConversionException.of(this, fieldNode, "Node for mapped Sections is not a MapNode!");
            }
        }
        field.set(section, fieldValue);
        return errorNodes;
    }

    // Configuration saving Methods

    /**
     * Fills the map with values from the Fields to save
     *
     * @param defaultSection the parent config
     * @param section        the config
     */
    final MapNode convertSection(Section defaultSection, Section section, Configuration config) // this is only package private as it is used for testing
    {
        MapNode baseNode = MapNode.emptyMap();
        if (!defaultSection.getClass().equals(section.getClass()))
        {
            throw new IllegalArgumentException("defaultSection and section have to be the same type of section!");
        }
        Class<? extends Section> sectionClass = section.getClass();
        for (Field field : sectionClass.getFields())
        {
            if (section != defaultSection && config.isInheritedField(field))
            {
                continue;
            }
            if (isConfigField(field))
            {
                try
                {
                    baseNode.setNodeAt(getPathFor(field), convertField(field, defaultSection, section, config));
                }
                catch (InvalidConfigurationException e) // rethrow
                {
                    throw e;
                }
                catch (IllegalAccessException e)
                {
                    throw FieldAccessException.of(getPathFor(field), sectionClass, field, e);
                }
                catch (ConversionException e) // fatal ConversionException
                {
                    throw InvalidConfigurationException.of("Could not convert Field into Node!", getPathFor(field), section.getClass(), field, e);
                }
                catch (RuntimeException e)
                {
                    throw InvalidConfigurationException.of("Unknown Error while converting Section!", getPathFor(field), section.getClass(), field, e);
                }
            }
        }
        if (section != defaultSection) // remove generated empty ParentNodes ONLY from child-configs
        {
            baseNode.cleanUpEmptyNodes();
        }
        return baseNode;
    }

    /**
     * Converts a single field of a section into a node
     *
     * @param field          the field to convert
     * @param defaultSection the defaultSection
     * @param section        the section containing the fields value
     * @param config         the configuration
     *
     * @return the converted Node
     */
    @SuppressWarnings("unchecked")
    private Node convertField(Field field, Section defaultSection, Section section, Configuration config) throws ConversionException, IllegalAccessException
    {
        Object fieldValue = field.get(section);
        Object defaultValue = section == defaultSection ? fieldValue : field.get(defaultSection);
        FieldType fieldType = getFieldType(field);
        Node node = null;
        switch (fieldType)
        {
        case NORMAL:
            node = converterManager.convertToNode(fieldValue);
            break;
        case SECTION:
            if (fieldValue == null)
            {
                if (defaultValue == null) // default section is not set -> generate new
                {
                    defaultValue = SectionFactory.newSectionInstance((Class<? extends Section>)field.getType(), defaultSection);
                    field.set(defaultSection, defaultValue);
                    if (section == defaultSection)
                    {
                        fieldValue = defaultValue;
                    }
                    else
                    {
                        dumpDefaultIntoField(defaultSection, section, field, config);
                        fieldValue = field.get(section);
                    }
                }
                else
                {
                    dumpDefaultIntoField(defaultSection, section, field, config);
                    fieldValue = field.get(section);
                }
            }
            node = convertSection((Section)defaultValue, (Section)fieldValue, config);
            break;
        case SECTION_COLLECTION:
            if (defaultSection != section)
            {
                throw new UnsupportedConfigurationException("Child-Configurations are not allowed for Sections in Collections");
            }
            node = ListNode.emptyList();
            for (Section subSection : (Collection<Section>)fieldValue)
            {
                MapNode listElemNode = convertSection(subSection, subSection, config);
                ((ListNode)node).addNode(listElemNode);
            }
            break;
        case SECTION_MAP:
            node = MapNode.emptyMap();
            Map<Object, Section> defaultFieldMap = (Map<Object, Section>)defaultValue;
            Map<Object, Section> fieldMap = (Map<Object, Section>)fieldValue;
            for (Map.Entry<Object, Section> defaultEntry : defaultFieldMap.entrySet())
            {
                Node keyNode = converterManager.convertToNode(defaultEntry.getKey());
                if (keyNode instanceof StringNode)
                {
                    MapNode configNode = convertSection(defaultEntry.getValue(), fieldMap.get(defaultEntry.getKey()), config);
                    ((MapNode)node).setNode((StringNode)keyNode, configNode);
                }
                else // TODO allow Numbers
                {
                    throw new UnsupportedConfigurationException("Key-Node is not supported for mapped Sections: " + keyNode);
                }
            }
        }
        addComment(node, field);
        return node;
    }

    // HELPER Methods

    /**
     * Adds a comment to the given Node
     *
     * @param node  the Node to add the comment to
     * @param field the field possibly having a {@link Comment} annotation
     */
    private void addComment(Node node, Field field)
    {
        if (field.isAnnotationPresent(Comment.class))
        {
            node.setComments(field.getAnnotation(Comment.class).value());
        }
    }

    /**
     * Detects the Type of a field
     *
     * @param field the field
     *
     * @return the Type of the field
     */
    private FieldType getFieldType(Field field)
    {
        FieldType fieldType = NORMAL;
        if (SectionFactory.isSectionClass(field.getType()))
        {
            return SECTION;
        }
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pType = (ParameterizedType)type;
            if (Collection.class.isAssignableFrom((Class)pType.getRawType()))
            {
                Type subType1 = pType.getActualTypeArguments()[0];
                if (subType1 instanceof Class && SectionFactory.isSectionClass((Class)subType1))
                {
                    return SECTION_COLLECTION;
                }
            }

            if (Map.class.isAssignableFrom((Class)pType.getRawType()))
            {
                Type subType2 = pType.getActualTypeArguments()[1];
                if (subType2 instanceof Class && SectionFactory.isSectionClass((Class)subType2))
                {
                    return SECTION_MAP;
                }
            }
        }
        return fieldType;
    }

    /**
     * Detects if given field needs to be serialized
     *
     * @param field the field to check
     *
     * @return whether the field is a field of the configuration that needs to be serialized
     */
    private boolean isConfigField(Field field)
    {
        int modifiers = field.getModifiers();
        return !(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers));
    }

    private ConfigPath getPathFor(Field field)
    {
        if (field.isAnnotationPresent(Name.class))
        {
            return ConfigPath.forName(field.getAnnotation(Name.class).value());
        }
        return ConfigPath.forName(StringUtils.fieldNameToPath(field.getName()));
    }
}
