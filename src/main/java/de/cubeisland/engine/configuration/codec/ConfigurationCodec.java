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
package de.cubeisland.engine.configuration.codec;

import de.cubeisland.engine.configuration.*;
import de.cubeisland.engine.configuration.annotations.Comment;
import de.cubeisland.engine.configuration.annotations.Name;
import de.cubeisland.engine.configuration.convert.ConversionException;
import de.cubeisland.engine.configuration.convert.converter.generic.CollectionConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.MapConverter;
import de.cubeisland.engine.configuration.node.*;

import java.beans.Transient;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.nio.file.Path;
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
    /**
     * Loads in the given configuration using the InputStream
     *
     * @param config the config to load
     * @param is the InputStream to load from
     */
    public Collection<ErrorNode> load(Configuration config, InputStream is)
    {
        return this.dumpIntoSection(config, this.loadFromInputStream(is));
    }

    /**
     * Saves the configuration into given file
     *
     * @param config the configuration to save
     * @param file the file to save into
     */
    public void save(Configuration config, Path file)
    {
        try
        {
            if (file == null)
            {
                throw new IllegalStateException("Tried to save config without File.");
            }
            this.saveIntoFile(config, this.convertSection(config), file);
        }
        catch (Exception ex)
        {
            throw new InvalidConfigurationException("Error while saving Configuration!", ex);
        }
    }

    /**
     * Saves the configuration with the values contained in the Node into a file for given path
     *
     * @param config the configuration to save
     * @param node the Node containing all data to save
     * @param file the file to save into
     * @throws IOException
     */
    protected abstract void saveIntoFile(Configuration config, MapNode node, Path file) throws IOException;

    /**
     * Converts the inputStream into a readable Object
     *
     * @param is the InputStream
     */
    public abstract MapNode loadFromInputStream(InputStream is);

    /**
     * Returns the FileExtension as String
     *
     * @return the fileExtension
     */
    public abstract String getExtension();

    /**
     * Adds a comment to the given Node
     *
     * @param node the Node to add the comment to
     * @param field the field possibly having a {@link Comment} annotation
     */
    protected void addComment(Node node, Field field)
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
     * @return the Type of the field
     */
    protected static FieldType getFieldType(Field field)
    {
        FieldType fieldType = NORMAL;
        if (Section.class.isAssignableFrom(field.getType()))
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
                if (subType1 instanceof Class && Section.class.isAssignableFrom((Class)subType1))
                {
                    return SECTION_COLLECTION;
                }
            }

            if (Map.class.isAssignableFrom((Class)pType.getRawType()))
            {
                Type subType2 = pType.getActualTypeArguments()[1];
                if (subType2 instanceof Class && Section.class.isAssignableFrom((Class)subType2))
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
     * @return whether the field is a field of the configuration that needs to be serialized
     */
    protected static boolean isConfigField(Field field)
    {
        int mask = field.getModifiers();
        if ((((mask & Modifier.STATIC) == Modifier.STATIC))) // skip static fields
        {
            return false;
        }
        // else disallow when Transient
        return !(field.isAnnotationPresent(Transient.class) && field.getAnnotation(Transient.class).value());
    }

    protected ConfigPath getPathFor(Field field)
    {
        if (field.isAnnotationPresent(Name.class))
        {
            return ConfigPath.forName(field.getAnnotation(Name.class).value());
        }
        return ConfigPath.forName(StringUtils.fieldNameToPath(field.getName()));
    }

    /**
     * Dumps the contents of the MapNode into the fields of the section
     *
     * @param section the configuration-section
     * @param currentNode the Node to load from
     * @return a collection of all erroneous Nodes
     */
    protected Collection<ErrorNode> dumpIntoSection(Section section, MapNode currentNode)
    {
        Collection<ErrorNode> errorNodes = new HashSet<>();
        for (Field field : section.getClass().getFields()) // ONLY public fields are allowed
        {
            if (isConfigField(field))
            {
                Node fieldNode = currentNode.getNodeAt(this.getPathFor(field));
                if (fieldNode instanceof ErrorNode)
                {
                    errorNodes.add((ErrorNode) fieldNode);
                }
                else if (!(fieldNode instanceof NullNode)) // Empty Node => default value
                {
                    try
                    {
                        errorNodes.addAll(this.dumpIntoField(section, field, fieldNode));
                    }
                    catch (Exception e)
                    {
                        throw InvalidConfigurationException.of("Error while dumping loaded section into fields!" , this.getPathFor(field), section.getClass(), field, e);
                    }
                }
            }
        }
        return errorNodes;
    }

    protected static Section newSectionInstance(Section parentSection, Class<? extends Section> subSectionClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        if (subSectionClass.getEnclosingClass() == null)
        {
            return subSectionClass.newInstance();
        }
        else if (Modifier.isStatic(subSectionClass.getModifiers()))
        {
            return subSectionClass.newInstance();
        }
        else
        {
            return subSectionClass.getDeclaredConstructor(parentSection.getClass()).newInstance(parentSection);
        }
    }

    /**
     * Dumps the contents of the Node into a field of the section
     *
     * @param section the configuration section
     * @param field the Field to load into
     * @param fieldNode the Node to load from
     * @return a collection of all erroneous Nodes
     */
    @SuppressWarnings("unchecked")
    protected Collection<ErrorNode> dumpIntoField(Section section, Field field, Node fieldNode) throws ConversionException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        Collection<ErrorNode> errorNodes = new HashSet<>();
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
                    fieldValue = newSectionInstance(section, (Class<? extends Section>) field.getType());
                    errorNodes.addAll(this.dumpIntoSection((Section) fieldValue, (MapNode)fieldNode));
                }
                else
                {
                    throw new InvalidConfigurationException("Node for Section is not a MapNode!\n" + fieldNode);
                }
                break;
            case SECTION_COLLECTION:
                if (fieldNode instanceof ListNode)
                {
                    fieldValue = CollectionConverter.getCollectionFor((ParameterizedType) type);
                    if (((ListNode)fieldNode).isEmpty())
                    {
                        break;
                    }
                    Class<? extends Section> subSectionClass = (Class<? extends Section>)((ParameterizedType) type).getActualTypeArguments()[0];
                    for (Node listedNode : ((ListNode)fieldNode).getListedNodes())
                    {
                        if (listedNode instanceof NullNode)
                        {
                            listedNode = MapNode.emptyMap();
                        }
                        if (listedNode instanceof MapNode)
                        {
                            Section subSection = newSectionInstance(section, subSectionClass);
                            errorNodes.addAll(this.dumpIntoSection(subSection, (MapNode) listedNode));
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
                    fieldValue = MapConverter.getMapFor((ParameterizedType) type);
                    if (((MapNode) fieldNode).isEmpty())
                    {
                        break;
                    }
                    Class<? extends Section> subSectionClass = (Class<? extends Section>)((ParameterizedType) type).getActualTypeArguments()[1];
                    for (Entry<String, Node> entry : ((MapNode)fieldNode).getMappedNodes().entrySet())
                    {
                        Object key = convertFromNode(StringNode.of(entry.getKey()), ((ParameterizedType) type).getActualTypeArguments()[0]);
                        Section value = newSectionInstance(section, subSectionClass);
                        if (entry.getValue() instanceof NullNode)
                        {
                            errorNodes.addAll(this.dumpIntoSection(value, MapNode.emptyMap()));
                        }
                        else if (entry.getValue() instanceof MapNode)
                        {
                            errorNodes.addAll(this.dumpIntoSection(value, (MapNode) entry.getValue()));
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

    /**
     * Converts an entire Section into a MapNode
     *
     * @param section the section to convert
     */
    public MapNode convertSection(Section section)
    {
        MapNode baseNode = MapNode.emptyMap();
        Class<? extends Section> configClass = section.getClass();
        for (Field field : configClass.getFields())
        {
            if (isConfigField(field))
            {
                try
                {
                    baseNode.setNodeAt(this.getPathFor(field), this.convertField(field, section));
                }
                catch (Exception e)
                {
                    throw InvalidConfigurationException.of(
                            "Error while converting Section into a MapNode!",
                            this.getPathFor(field), section.getClass(), field, e);
                }
            }
        }
        return baseNode;
    }

    /**
     * Converts a single field of a section into a node
     *
     * @param field the field to get the values from
     * @param section the section containing the fields value
     */
    @SuppressWarnings("unchecked")
    public Node convertField(Field field, Section section) throws IllegalAccessException, ConversionException
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
                node = this.convertSection((Section) fieldValue);
                break;
            case SECTION_COLLECTION:
                node = ListNode.emptyList();
                for (Section subSection : (Collection<Section>)fieldValue)
                {
                    MapNode listElemNode = this.convertSection(subSection);
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
                        MapNode valueNode = this.convertSection(entry.getValue());
                        ((MapNode)node).setNode((StringNode)keyNode, valueNode);
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

}
