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

import de.cubeisland.engine.configuration.*;
import de.cubeisland.engine.configuration.annotations.Comment;
import de.cubeisland.engine.configuration.annotations.Name;
import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.convert.ConverterNotFoundException;
import de.cubeisland.engine.configuration.convert.converter.generic.CollectionConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.MapConverter;
import de.cubeisland.engine.configuration.exception.InvalidConfigurationException;
import de.cubeisland.engine.configuration.node.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import static de.cubeisland.engine.configuration.FieldType.*;

/**
 * This abstract Codec can be implemented to read and write configurations that allow child-configs
 */
public abstract class ConfigurationCodec
{
    public static final Convert CODEC_CONVERTERS = Convert.emptyConverter();

    // PUBLIC Methods

    /**
     * Loads in the given <code>Configuration</code> using the <code>InputStream</code>
     * <p>if the configuration has no default set it will be saved normally!
     *
     * @param config the Configuration to load
     * @param is     the InputStream to load from
     *
     * @return a collection of all erroneous Nodes
     */
    public final Collection<ErrorNode> loadConfig(Configuration config, InputStream is) throws InvalidConfigurationException
    {
        return dumpIntoSection(config.getDefault(), config, this.load(is, config), config);
    }

    /**
     * Saves the <code>Configuration</code> using given <code>OutputStream</code>
     *
     * @param config the Configuration to save
     * @param os     the OutputStream to save into
     */
    public final void saveConfig(Configuration config, OutputStream os) throws IOException
    {
        this.save(convertSection(config.getDefault(), config, config), os, config);
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
     * @throws IOException
     */
    protected abstract void save(MapNode node, OutputStream os, Configuration config) throws IOException;

    /**
     * Converts the <code>InputStream</code> into a <code>MapNode</code>
     *
     * @param is the InputStream to load from
     * @param config the Configuration
     */
    protected abstract MapNode load(InputStream is, Configuration config) throws InvalidConfigurationException;

    // PACKAGE-PRIVATE STATIC Methods

    // Configuration loading Methods

    /**
     * Dumps the contents of the MapNode into the fields of the section using the defaultSection as default if a node is not given
     *
     * @param defaultSection the parent configuration-section
     * @param section       the configuration-section
     * @param currentNode   the Node to load from
     * @param config        the MultiConfiguration containing this section
     *
     * @return a collection of all erroneous Nodes
     */
    final static Collection<ErrorNode> dumpIntoSection(Section defaultSection, Section section, MapNode currentNode, Configuration config)
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
                                config.addinheritedField(field);
                            }
                        }
                        else
                        {
                            errorNodes.addAll(dumpIntoField(defaultSection, section, field, fieldNode, config));
                        }
                    }
                    catch (ReflectiveOperationException e)
                    {
                        throw new InvalidConfigurationException("Error while dumping loaded config into fields", e);
                    }
                    catch (ConversionException e)
                    {
                        InvalidConfigurationException ex = InvalidConfigurationException.of("Error while converting Node to dump into field!", getPathFor(field), section.getClass(), field, e);
                        if (config.useStrictExceptionPolicy())
                        {
                            throw ex;
                        }
                        errorNodes.add(new ErrorNode(ex));
                    }
                    catch (Exception e)
                    {
                        throw InvalidConfigurationException.of("Error while dumping loaded config into fields!", getPathFor(field), section.getClass(), field, e);
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
    final static Collection<ErrorNode> dumpDefaultIntoField(Section parentSection, Section section, Field field, Configuration config) throws ConversionException, ReflectiveOperationException
    {
        if (parentSection != section)
        {
            if (getFieldType(field) == FieldType.SECTION_COLLECTION)
            {
                throw new InvalidConfigurationException("Child-Configurations are not allowed for Sections in Collections");
            }
        }
        return dumpIntoField(parentSection, section, field, convertField(field, parentSection, parentSection, config), config); // convert parent in node and dump back in
    }

    /**
     * Dumps the contents of the Node into a field of the section
     *
     * @param defaultSection the parent configuration-section
     * @param section       the configuration-section
     * @param field         the Field to load into
     * @param fieldNode     the Node to load from
     * @param config        the MultiConfiguration containing this section
     *
     * @return a collection of all erroneous Nodes
     */
    @SuppressWarnings("unchecked")
    final static Collection<ErrorNode> dumpIntoField(Section defaultSection, Section section, Field field, Node fieldNode, Configuration config) throws ConversionException, ReflectiveOperationException
    {
        Collection<ErrorNode> errorNodes = new HashSet<ErrorNode>();
        Type type = field.getGenericType();
        FieldType fieldType = getFieldType(field);
        Object fieldValue = null;
        Object defaultValue = field.get(defaultSection);
        switch (fieldType)
        {
            case NORMAL:
                fieldValue = convertFromNode(fieldNode, type); // Convert the value
                if (fieldValue == null && !(section == defaultSection))
                {
                    fieldValue = field.get(defaultSection);
                    config.addinheritedField(field);
                }
                break;
            case SECTION:
                if (fieldNode instanceof MapNode)
                {
                    fieldValue = SectionFactory.newSectionInstance((Class<? extends Section>) field.getType(), section);
                    if (defaultValue == null)
                    {
                        if (section == defaultSection)
                        {
                            defaultValue = fieldValue;
                        }
                        else
                        {
                            defaultValue = SectionFactory.newSectionInstance((Class<? extends Section>) field.getType(), defaultSection);
                        }
                    }
                    errorNodes.addAll(dumpIntoSection((Section) defaultValue, (Section) fieldValue, (MapNode) fieldNode, config));
                }
                else
                {
                    throw new InvalidConfigurationException("Node for Section is not a MapNode!\n" + fieldNode);
                }
                break;
            case SECTION_COLLECTION:
                if (section != defaultSection)
                {
                    throw new InvalidConfigurationException("Child-Configurations are not allowed for Sections in Collections");
                }
                if (fieldNode instanceof ListNode)
                {
                    fieldValue = CollectionConverter.getCollectionFor((ParameterizedType) type);
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
                            errorNodes.addAll(dumpIntoSection(subSection, subSection, (MapNode) listedNode, config));
                            ((Collection<Section>)fieldValue).add(subSection);
                        }
                        else
                        {
                            throw new InvalidConfigurationException("Node for listed Section is not a MapNode!\n" + listedNode);
                        }
                    }
                }
                else
                {
                    throw new InvalidConfigurationException("Node for listed Sections is not a ListNode!\n" + fieldNode);
                }
                break;
            case SECTION_MAP:
                if (fieldNode instanceof MapNode)
                {
                    fieldValue = MapConverter.getMapFor((ParameterizedType)type);
                    if (((MapNode)fieldNode).isEmpty())
                    if (((MapNode)fieldNode).isEmpty())
                    {
                        break;
                    }
                    Map<Object, Section> mappedParentSections = (Map<Object, Section>)field.get(defaultSection);
                    Class<? extends Section> subSectionClass = (Class<? extends Section>)((ParameterizedType)type).getActualTypeArguments()[1];
                    for (Map.Entry<String, Node> entry : ((MapNode)fieldNode).getMappedNodes().entrySet())
                    {
                        Object key = convertFromNode(StringNode.of(entry.getKey()), ((ParameterizedType) type).getActualTypeArguments()[0]);
                        Section value = SectionFactory.newSectionInstance(subSectionClass, section);
                        if (entry.getValue() instanceof NullNode)
                        {
                            errorNodes.addAll(dumpIntoSection(defaultSection, section, MapNode.emptyMap(), config));
                        }
                        else if (entry.getValue() instanceof MapNode)
                        {
                            errorNodes.addAll(dumpIntoSection(mappedParentSections.get(key), value, (MapNode) entry.getValue(), config));
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

    protected static Object convertFromNode(Node node, Type type) throws ConversionException
    {
        try
        {
            return CODEC_CONVERTERS.convertFromNode(node, type);
        }
        catch (ConverterNotFoundException ignored)
        {}
        return Configuration.CONVERTERS.convertFromNode(node, type);
    }

    // Configuration saving Methods

    /**
     * Fills the map with values from the Fields to save
     *
     * @param defaultSection the parent config
     * @param section       the config
     */
    static MapNode convertSection(Section defaultSection, Section section, Configuration config)
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
                catch (ReflectiveOperationException e)
                {
                    throw new InvalidConfigurationException("Error while converting field into node", e);
                }
                catch (ConversionException e)
                {
                    throw InvalidConfigurationException.of("Error while converting Field into Node!", getPathFor(field), section.getClass(), field, e);
                }
                catch (Exception e)
                {
                    throw InvalidConfigurationException.of("Error while converting Section!", getPathFor(field), section.getClass(), field, e);
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
     * @param field the field to convert
     * @param defaultSection the defaultSection
     * @param section the section containing the fields value
     * @param config the configuration
     * @return the converted Node
     */
    @SuppressWarnings("unchecked")
    private static Node convertField(Field field, Section defaultSection, Section section, Configuration config) throws ReflectiveOperationException, ConversionException
    {
        Object fieldValue = field.get(section);
        Object defaultValue = section == defaultSection ? fieldValue : field.get(defaultSection);
        FieldType fieldType = getFieldType(field);
        Node node = null;
        switch (fieldType)
        {
            case NORMAL:
                node = convertToNode(fieldValue);
                break;
            case SECTION:
                if (fieldValue == null)
                {
                    if (defaultValue == null)
                    {
                        defaultValue = SectionFactory.newSectionInstance((Class<? extends Section>) field.getType(), defaultSection); // default section is not set -> generate new
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
                node = convertSection((Section) defaultValue, (Section) fieldValue, config);
                break;
            case SECTION_COLLECTION:
                if (defaultSection != section)
                {
                    throw InvalidConfigurationException.of("Child-Configurations are not allowed for Sections in Collections", getPathFor(field), section.getClass(), field, null);
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
                    Node keyNode = convertToNode(defaultEntry.getKey());
                    if (keyNode instanceof StringNode)
                    {
                        MapNode configNode = convertSection(defaultEntry.getValue(), fieldMap.get(defaultEntry.getKey()), config);
                        ((MapNode)node).setNode((StringNode)keyNode, configNode);
                    }
                    else
                    {
                        throw InvalidConfigurationException.of("Invalid Key-Node for mapped Section at", getPathFor(field), section.getClass(), field, null);
                    }
                }
        }
        addComment(node, field);
        return node;

    }

    protected static Node convertToNode(Object o) throws ConversionException
    {
        try
        {
            return CODEC_CONVERTERS.convertToNode(o);
        }
        catch (ConverterNotFoundException ignored)
        {}
        return Configuration.CONVERTERS.convertToNode(o);
    }

    // HELPER Methods

    /**
     * Adds a comment to the given Node
     *
     * @param node  the Node to add the comment to
     * @param field the field possibly having a {@link de.cubeisland.engine.configuration.annotations.Comment} annotation
     */
    final static void addComment(Node node, Field field)
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
    final static FieldType getFieldType(Field field)
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
                if (subType1 instanceof Class && SectionFactory.isSectionClass((Class) subType1))
                {
                    return SECTION_COLLECTION;
                }
            }

            if (Map.class.isAssignableFrom((Class)pType.getRawType()))
            {
                Type subType2 = pType.getActualTypeArguments()[1];
                if (subType2 instanceof Class && SectionFactory.isSectionClass((Class) subType2))
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
    final static boolean isConfigField(Field field)
    {
        int modifiers = field.getModifiers();
        return !(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers));
    }

    final static ConfigPath getPathFor(Field field)
    {
        if (field.isAnnotationPresent(Name.class))
        {
            return ConfigPath.forName(field.getAnnotation(Name.class).value());
        }
        return ConfigPath.forName(StringUtils.fieldNameToPath(field.getName()));
    }
}
