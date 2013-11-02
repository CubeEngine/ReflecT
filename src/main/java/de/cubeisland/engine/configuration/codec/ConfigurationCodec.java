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
import de.cubeisland.engine.configuration.convert.converter.generic.CollectionConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.MapConverter;
import de.cubeisland.engine.configuration.node.*;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import static de.cubeisland.engine.configuration.Configuration.convertFromNode;
import static de.cubeisland.engine.configuration.Configuration.convertToNode;
import static de.cubeisland.engine.configuration.FieldType.*;

/**
 * This abstract Codec can be implemented to read and write configurations.
 */
public abstract class ConfigurationCodec
{
    // PUBLIC Methods

    /**
     * Loads in the given <code>Configuration</code> using the <code>InputStream</code>
     *
     * @param config the config to load
     * @param is     the InputStream to load from
     */
    public final Collection<ErrorNode> load(Configuration config, InputStream is)
    {
        return dumpIntoSection(config, this.load(is, config));
    }

    /**
     * Saves the <code>Configuration</code> into given <code>File</code>
     *
     * @param config the configuration to save
     * @param file   the file to save into
     */
    public final void save(Configuration config, File file)
    {
        try
        {
            if (file == null)
            {
                throw new IllegalStateException("Tried to save config without File.");
            }
            this.save(convertSection(config), new FileOutputStream(file), config);
        }
        catch (Exception ex)
        {
            throw new InvalidConfigurationException("Error while saving Configuration!", ex);
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
     * Saves the values contained in the <code>MapNode</code> into a file
     *
     *
     * @param node   the Node containing all data to save
     * @param os   the file to save into
     * @param config the configuration
     * @throws IOException
     */
    protected abstract void save(MapNode node, OutputStream os, Configuration config) throws IOException;

    /**
     * Converts the <code>InputStream</code> into a <code>MapNode</code>
     *
     * @param is the InputStream to load from
     * @param config
     */
    protected abstract MapNode load(InputStream is, Configuration config);

    // HELPER Methods

    /**
     * Adds a comment to the given Node
     *
     * @param node  the Node to add the comment to
     * @param field the field possibly having a {@link Comment} annotation
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

    // Configuration loading Methods

    /**
     * Dumps the contents of the MapNode into the fields of the section
     *
     * @param section     the configuration-section
     * @param currentNode the Node to load from
     *
     * @return a collection of all erroneous Nodes
     */
    @SuppressWarnings("unchecked")
    final static Collection<ErrorNode> dumpIntoSection(Section section, MapNode currentNode)
    {
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
                else if (fieldNode instanceof NullNode) // Empty Node => default value
                {
                    try
                    {
                        if (field.get(section) == null && SectionFactory.isSectionClass(field.getType())) // if section is not yet set
                        {
                            field.set(section, SectionFactory.newSectionInstance((Class<? extends Section>) field.getType(), section)); // create a new instance
                        }
                    }
                    catch (ReflectiveOperationException e)
                    {
                        throw InvalidConfigurationException.of("Error while creating unset section!", getPathFor(field), section.getClass(), field, e);
                    }
                }
                else
                {
                    try
                    {
                        errorNodes.addAll(dumpIntoField(section, field, fieldNode));
                    }
                    catch (Exception e)
                    {
                        throw InvalidConfigurationException.of("Error while dumping loaded section into fields!", getPathFor(field), section.getClass(), field, e);
                    }
                }
            }
        }
        return errorNodes;
    }

    /**
     * Dumps the contents of the Node into a field of the section
     *
     * @param section   the configuration section
     * @param field     the Field to load into
     * @param fieldNode the Node to load from
     *
     * @return a collection of all erroneous Nodes
     */
    @SuppressWarnings("unchecked")
    final static Collection<ErrorNode> dumpIntoField(Section section, Field field, Node fieldNode) throws ConversionException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        Collection<ErrorNode> errorNodes = new HashSet<ErrorNode>();
        Type type = field.getGenericType();
        FieldType fieldType = getFieldType(field);
        Object fieldValue = null;
        switch (fieldType)
        {
            case NORMAL:
                fieldValue = convertFromNode(fieldNode, type); // Convert the value
                break;
            case SECTION:
                if (fieldNode instanceof MapNode)
                {
                    fieldValue = SectionFactory.newSectionInstance((Class<? extends Section>) field.getType(), section);
                    errorNodes.addAll(dumpIntoSection((Section) fieldValue, (MapNode) fieldNode));
                }
                else
                {
                    throw new InvalidConfigurationException("Node for Section is not a MapNode!\n" + fieldNode);
                }
                break;
            case SECTION_COLLECTION:
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
                            errorNodes.addAll(dumpIntoSection(subSection, (MapNode) listedNode));
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
                    {
                        break;
                    }
                    Class<? extends Section> subSectionClass = (Class<? extends Section>)((ParameterizedType)type).getActualTypeArguments()[1];
                    for (Entry<String, Node> entry : ((MapNode)fieldNode).getMappedNodes().entrySet())
                    {
                        Object key = convertFromNode(StringNode.of(entry.getKey()), ((ParameterizedType)type).getActualTypeArguments()[0]);
                        Section value = SectionFactory.newSectionInstance(subSectionClass, section);
                        if (entry.getValue() instanceof NullNode)
                        {
                            errorNodes.addAll(dumpIntoSection(value, MapNode.emptyMap()));
                        }
                        else if (entry.getValue() instanceof MapNode)
                        {
                            errorNodes.addAll(dumpIntoSection(value, (MapNode) entry.getValue()));
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
        if (fieldValue == null)
        {
            throw new IllegalStateException();
        }
        field.set(section, fieldValue); //Set loaded Value into Field
        return errorNodes;
    }

    // Configuration saving Methods

    /**
     * Converts an entire Section into a MapNode
     *
     * @param section the section to convert
     */
    final static MapNode convertSection(Section section)
    {
        MapNode baseNode = MapNode.emptyMap();
        Class<? extends Section> configClass = section.getClass();
        for (Field field : configClass.getFields())
        {
            if (isConfigField(field))
            {
                try
                {
                    baseNode.setNodeAt(getPathFor(field), convertField(field, section));
                }
                catch (Exception e)
                {
                    throw InvalidConfigurationException.of("Error while converting Section into a MapNode!", getPathFor(field), section.getClass(), field, e);
                }
            }
        }
        return baseNode;
    }

    /**
     * Converts a single field of a section into a node
     *
     * @param field   the field to get the values from
     * @param section the section containing the fields value
     */
    @SuppressWarnings("unchecked")
    final static Node convertField(Field field, Section section) throws IllegalAccessException, ConversionException, NoSuchMethodException, InstantiationException, InvocationTargetException
    {
        Object fieldValue = field.get(section);
        FieldType fieldType = getFieldType(field);
        Node node = NullNode.emptyNode();
        switch (fieldType)
        {
            case NORMAL:
                node = convertToNode(fieldValue);
                break;
            case SECTION:
                if (fieldValue == null)
                {
                    fieldValue = SectionFactory.newSectionInstance((Class<? extends Section>) field.getType(), section); // You do not need to instantiate sections
                    field.set(section, fieldValue);
                }
                node = convertSection((Section)fieldValue);
                break;
            case SECTION_COLLECTION:
                node = ListNode.emptyList();
                for (Section subSection : (Collection<Section>)fieldValue)
                {
                    MapNode listElemNode = convertSection(subSection);
                    ((ListNode)node).addNode(listElemNode);
                }
                break;
            case SECTION_MAP:
                node = MapNode.emptyMap();
                Map<Object, Section> fieldMap = (Map<Object, Section>)fieldValue;
                for (Map.Entry<Object, Section> entry : fieldMap.entrySet())
                {
                    Node keyNode = convertToNode(entry.getKey());
                    if (keyNode instanceof StringNode)
                    {
                        MapNode valueNode = convertSection(entry.getValue());
                        ((MapNode)node).setNode((StringNode)keyNode, valueNode);
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
}
