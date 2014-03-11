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
package de.cubeisland.engine.reflect.codec;

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
import java.util.Map.Entry;
import java.util.logging.Level;

import de.cubeisland.engine.reflect.Reflected;
import de.cubeisland.engine.reflect.FieldType;
import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.SectionFactory;
import de.cubeisland.engine.reflect.util.StringUtils;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.annotations.Name;
import de.cubeisland.engine.reflect.codec.converter.generic.CollectionConverter;
import de.cubeisland.engine.reflect.codec.converter.generic.MapConverter;
import de.cubeisland.engine.reflect.exception.CodecIOException;
import de.cubeisland.engine.reflect.exception.ConversionException;
import de.cubeisland.engine.reflect.exception.FieldAccessException;
import de.cubeisland.engine.reflect.exception.InvalidReflectedObjectException;
import de.cubeisland.engine.reflect.exception.UnsupportedReflectedException;
import de.cubeisland.engine.reflect.node.KeyNode;
import de.cubeisland.engine.reflect.node.ReflectedPath;
import de.cubeisland.engine.reflect.node.ErrorNode;
import de.cubeisland.engine.reflect.node.ListNode;
import de.cubeisland.engine.reflect.node.MapNode;
import de.cubeisland.engine.reflect.node.Node;
import de.cubeisland.engine.reflect.node.NullNode;
import de.cubeisland.engine.reflect.node.StringNode;

import static de.cubeisland.engine.reflect.FieldType.*;

/**
 * This abstract Codec can be implemented to read and write reflected objects that allow child-reflected
 */
public abstract class Codec
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
     * Loads in the given <code>Reflected</code> using the <code>InputStream</code>
     * <p>if the reflected object has no default set it will be saved normally!
     *
     * @param reflected the Reflected to load
     * @param is     the InputStream to load from
     *
     * @return a collection of all erroneous Nodes
     */
    public final Collection<ErrorNode> loadReflected(Reflected reflected, InputStream is)
    {
        try
        {
            return dumpIntoSection(reflected.getDefault(), reflected, this.load(is, reflected), reflected);
        }
        catch (ConversionException ex)
        {
            if (reflected.useStrictExceptionPolicy())
            {
                throw new CodecIOException("Could not load reflected", ex);
            }
            reflected.getLogger().warning("Could not load reflected" + ex);
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
                reflected.getLogger().log(Level.WARNING, "Failed to close InputStream", e);
            }
        }
    }

    /**
     * Saves the <code>Reflected</code> using given <code>OutputStream</code>
     *
     * @param reflected the Reflected to save
     * @param os     the OutputStream to save into
     */
    public final void saveReflected(Reflected reflected, OutputStream os)
    {
        try
        {
            this.save(convertSection(reflected.getDefault(), reflected, reflected), os, reflected);
        }
        catch (ConversionException ex)
        {
            if (reflected.useStrictExceptionPolicy())
            {
                throw new CodecIOException("Could not save reflected", ex);
            }
            reflected.getLogger().warning("Could not save reflected" + ex);
        }
        finally
        {
            try
            {
                os.close();
            }
            catch (IOException e)
            {
                reflected.getLogger().log(Level.WARNING, "Failed to close OutputStream", e);
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
     * @param reflected the reflected
     */
    protected abstract void save(MapNode node, OutputStream os, Reflected reflected) throws ConversionException;

    /**
     * Converts the <code>InputStream</code> into a <code>MapNode</code>
     *
     * @param is     the InputStream to load from
     * @param reflected the reflected
     */
    protected abstract MapNode load(InputStream is, Reflected reflected) throws ConversionException;

    // Reflected loading Methods

    /**
     * Dumps the contents of the MapNode into the fields of the section using the defaultSection as default if a node is not given
     *
     * @param defaultSection the parent reflected-section
     * @param section        the reflected-section
     * @param currentNode    the Node to load from
     * @param reflected         the MultiReflected containing this section
     *
     * @return a collection of all erroneous Nodes
     */
    private Collection<ErrorNode> dumpIntoSection(Section defaultSection, Section section, MapNode currentNode, Reflected reflected)
    {
        Section dSection = defaultSection == null ? section : defaultSection;
        if (!dSection.getClass().equals(section.getClass()))
        {
            throw new IllegalArgumentException("defaultSection and section have to be the same type of section!");
        }
        Collection<ErrorNode> errorNodes = new HashSet<ErrorNode>();
        for (Field field : section.getClass().getFields()) // ONLY public fields are allowed
        {
            if (isReflectedField(field))
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
                            errorNodes.addAll(dumpDefaultIntoField(dSection, section, field, reflected));
                            if (section != dSection)
                            {
                                reflected.addInheritedField(field);
                            }
                        }
                        else
                        {
                            errorNodes.addAll(dumpIntoField(dSection, section, field, fieldNode, reflected));
                        }
                    }
                    catch (InvalidReflectedObjectException e) // rethrow
                    {
                        throw e;
                    }
                    catch (IllegalAccessException e)
                    {
                        throw FieldAccessException.of(getPathFor(field), section.getClass(), field, e);
                    }
                    catch (ConversionException e) // non-fatal ConversionException
                    {
                        InvalidReflectedObjectException ex = InvalidReflectedObjectException
                            .of("Error while converting Node to dump into field!", getPathFor(field), section
                                .getClass(), field, e);
                        if (reflected.useStrictExceptionPolicy())
                        {
                            throw ex;
                        }
                        reflected.getLogger().log(Level.WARNING, ex.getMessage(), ex);
                    }
                    catch (RuntimeException e)
                    {
                        throw InvalidReflectedObjectException
                            .of("Unknown Error while dumping loaded reflected into fields", getPathFor(field), section
                                .getClass(), field, e);
                    }
                }
            }
        }
        return errorNodes;
    }

    /**
     * Copy the contents of the parent section into a field of the section
     *
     * @param parentSection the parent reflected-section
     * @param section       the reflected-section
     * @param field         the Field to load into
     *
     * @return a collection of all erroneous Nodes
     */
    @SuppressWarnings("unchecked")
    private Collection<ErrorNode> dumpDefaultIntoField(Section parentSection, Section section, Field field, Reflected reflected) throws ConversionException, IllegalAccessException
    {
        if (parentSection != section && getFieldType(field) == FieldType.SECTION_COLLECTION)
        {
            throw new UnsupportedReflectedException("Child-reflected are not allowed for Sections in Collections");
        }
        return dumpIntoField(parentSection, section, field, convertField(field, parentSection, parentSection, reflected), reflected); // convert parent in node and dump back in
    }

    /**
     * Dumps the contents of the Node into a field of the section
     *
     * @param defaultSection the parent reflected-section
     * @param section        the reflected-section
     * @param field          the Field to load into
     * @param fieldNode      the Node to load from
     * @param reflected         the MultiReflected containing this section
     *
     * @return a collection of all erroneous Nodes
     */
    private Collection<ErrorNode> dumpIntoField(Section defaultSection, Section section, Field field, Node fieldNode, Reflected reflected) throws ConversionException, IllegalAccessException
    {
        Collection<ErrorNode> errorNodes = new HashSet<ErrorNode>();
        Type type = field.getGenericType();
        FieldType fieldType = getFieldType(field);
        Object fieldValue;
        Object defaultValue = field.get(defaultSection);
        if (fieldType == FieldType.NORMAL)
        {
            fieldValue = dumpIntoNormalField(defaultSection, section, field, fieldNode, reflected, type);
        }
        else if (fieldType == FieldType.SECTION)
        {
            if (fieldNode instanceof MapNode)
            {
                fieldValue = dumpIntoSectionField(defaultSection, section, field, (MapNode)fieldNode, reflected, errorNodes, defaultValue);
            }
            else
            {
                throw ConversionException.of(this, fieldNode, "Node for Section is not a MapNode!");
            }
        }
        else if (fieldType == FieldType.SECTION_COLLECTION)
        {
            if (section != defaultSection)
            {
                throw new UnsupportedReflectedException("Child-reflected are not allowed for Sections in Collections");
            }
            if (fieldNode instanceof ListNode)
            {
                fieldValue = dumpIntoSectionCollectionField(section, (ListNode)fieldNode, reflected, errorNodes, (ParameterizedType)type);
            }
            else
            {
                throw ConversionException.of(this, fieldNode, "\"Node for listed Sections is not a ListNode!");
            }
        }
        else if (fieldType == FieldType.SECTION_MAP)
        {
            if (fieldNode instanceof MapNode)
            {
                fieldValue = dumpIntoSectionMapField(defaultSection, section, field, (MapNode)fieldNode, reflected, errorNodes, (ParameterizedType)type);
            }
            else
            {
                throw ConversionException.of(this, fieldNode, "Node for mapped Sections is not a MapNode!");
            }
        }
        else
        {
            throw new IllegalArgumentException("Invalid FieldType!");
        }
        field.set(section, fieldValue);
        return errorNodes;
    }

    @SuppressWarnings("unchecked")
    private Object dumpIntoSectionMapField(Section defaultSection, Section section, Field field, MapNode fieldNode, Reflected reflected, Collection<ErrorNode> errorNodes, ParameterizedType type) throws IllegalAccessException, ConversionException
    {
        Object fieldValue = MapConverter.getMapFor(type);
        if (fieldNode.isEmpty())
        {
            return fieldValue;
        }
        Map<Object, Section> mappedParentSections = (Map<Object, Section>)field.get(defaultSection);
        Class<? extends Section> subSectionClass = (Class<? extends Section>)type.getActualTypeArguments()[1];
        for (Entry<String, Node> entry : fieldNode.getMappedNodes().entrySet())
        {
            Object key = converterManager.convertFromNode(StringNode.of(entry.getKey()), type.getActualTypeArguments()[0]);
            Section value = SectionFactory.newSectionInstance(subSectionClass, section);
            if (entry.getValue() instanceof NullNode)
            {
                errorNodes.addAll(dumpIntoSection(defaultSection, section, MapNode.emptyMap(), reflected));
            }
            else if (entry.getValue() instanceof MapNode)
            {
                errorNodes.addAll(dumpIntoSection(mappedParentSections.get(key), value, (MapNode)entry.getValue(), reflected));
            }
            else
            {
                throw ConversionException.of(this, entry.getValue(), "Value-Node for mapped Section is not a MapNode!");
            }
            ((Map<Object, Section>)fieldValue).put(key, value);
        }
        return fieldValue;
    }

    @SuppressWarnings("unchecked")
    private Object dumpIntoSectionCollectionField(Section section, ListNode fieldNode, Reflected reflected, Collection<ErrorNode> errorNodes, ParameterizedType type) throws ConversionException
    {
        Object fieldValue = CollectionConverter.getCollectionFor(type);
        if (fieldNode.isEmpty())
        {
            return fieldValue;
        }
        Class<? extends Section> subSectionClass = (Class<? extends Section>)type.getActualTypeArguments()[0];
        for (Node listedNode : fieldNode.getValue())
        {
            if (listedNode instanceof NullNode)
            {
                listedNode = MapNode.emptyMap();
            }
            if (listedNode instanceof MapNode)
            {
                Section subSection = SectionFactory.newSectionInstance(subSectionClass, section);
                errorNodes.addAll(dumpIntoSection(subSection, subSection, (MapNode)listedNode, reflected));
                ((Collection<Section>)fieldValue).add(subSection);
            }
            else
            {
                throw ConversionException.of(this, listedNode, "Node for listed Section is not a MapNode!");
            }
        }
        return fieldValue;
    }

    @SuppressWarnings("unchecked")
    private Object dumpIntoSectionField(Section defaultSection, Section section, Field field, MapNode fieldNode, Reflected reflected, Collection<ErrorNode> errorNodes, Object defaultValue)
    {
        Object fieldValue = SectionFactory.newSectionInstance((Class<? extends Section>)field.getType(), section);
        if (defaultValue == null)
        {
            if (section == defaultSection)
            {
                defaultValue = fieldValue;
            }
            else
            {
                defaultValue = SectionFactory.newSectionInstance((Class<? extends Section>)field.getType(), defaultSection);
            }
        }
        errorNodes.addAll(dumpIntoSection((Section)defaultValue, (Section)fieldValue, fieldNode, reflected));
        return fieldValue;
    }

    private Object dumpIntoNormalField(Section defaultSection, Section section, Field field, Node fieldNode, Reflected reflected, Type type) throws ConversionException, IllegalAccessException
    {
        Object fieldValue = converterManager.convertFromNode(fieldNode, type); // Convert the value
        if (fieldValue == null && !(section == defaultSection))
        {
            fieldValue = field.get(defaultSection);
            reflected.addInheritedField(field);
        }
        return fieldValue;
    }

    // Reflected saving Methods

    /**
     * Fills the map with values from the Fields to save
     *
     * @param defaultSection the parent section
     * @param section        the section
     */
    final MapNode convertSection(Section defaultSection, Section section, Reflected reflected) // this is only package private as it is used for testing
    {
        MapNode baseNode = MapNode.emptyMap();
        if (!defaultSection.getClass().equals(section.getClass()))
        {
            throw new IllegalArgumentException("defaultSection and section have to be the same type of section!");
        }
        Class<? extends Section> sectionClass = section.getClass();
        for (Field field : sectionClass.getFields())
        {
            if (section != defaultSection && reflected.isInheritedField(field))
            {
                continue;
            }
            if (isReflectedField(field))
            {
                try
                {
                    Node prevNode = baseNode.getNodeAt(getPathFor(field));
                    if (prevNode instanceof MapNode)
                    {
                        Node node = convertField(field, defaultSection, section, reflected);
                        if (node instanceof MapNode)
                        {
                            for (Entry<String, Node> entry : ((MapNode)node).getMappedNodes().entrySet())
                            {
                                ((MapNode)prevNode).setExactNode(entry.getKey(), entry.getValue());
                            }
                        }
                    }
                    else
                    {
                        baseNode.setNodeAt(getPathFor(field), convertField(field, defaultSection, section, reflected));
                    }
                }
                catch (InvalidReflectedObjectException e) // rethrow
                {
                    throw e;
                }
                catch (IllegalAccessException e)
                {
                    throw FieldAccessException.of(getPathFor(field), sectionClass, field, e);
                }
                catch (ConversionException e) // fatal ConversionException
                {
                    throw InvalidReflectedObjectException
                        .of("Could not convert Field into Node!", getPathFor(field), section.getClass(), field, e);
                }
                catch (RuntimeException e)
                {
                    throw InvalidReflectedObjectException
                        .of("Unknown Error while converting Section!", getPathFor(field), section.getClass(), field, e);
                }
            }
        }
        if (section != defaultSection) // remove generated empty ParentNodes ONLY from child-reflected
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
     * @param reflected      the reflected object
     *
     * @return the converted Node
     */
    @SuppressWarnings("unchecked")
    private Node convertField(Field field, Section defaultSection, Section section, Reflected reflected) throws ConversionException, IllegalAccessException
    {
        Object fieldValue = field.get(section);
        Object defaultValue = section == defaultSection ? fieldValue : field.get(defaultSection);
        FieldType fieldType = getFieldType(field);
        Node node;
        switch (fieldType)
        {
        case NORMAL:
            node = converterManager.convertToNode(fieldValue);
            break;
        case SECTION:
            node = convertSectionField(field, defaultSection, section, reflected, fieldValue, defaultValue);
            break;
        case SECTION_COLLECTION:
            if (defaultSection != section)
            {
                throw new UnsupportedReflectedException("Child-reflected are not allowed for Sections in Collections");
            }
            node = convertSectionCollectionField(reflected, (Collection<Section>)fieldValue);
            break;
        case SECTION_MAP:
            node = convertSectionMapField(reflected, (Map<Object, Section>)fieldValue, (Map<Object, Section>)defaultValue);
            break;
        default:
            throw new IllegalArgumentException("Invalid FieldType!");
        }
        addComment(node, field);
        return node;
    }

    private Node convertSectionMapField(Reflected reflected, Map<Object, Section> fieldMap, Map<Object, Section> defaultFieldMap) throws ConversionException
    {
        MapNode node = MapNode.emptyMap();
        for (Entry<Object, Section> defaultEntry : defaultFieldMap.entrySet())
        {
            Node keyNode = converterManager.convertToNode(defaultEntry.getKey());
            if (keyNode instanceof KeyNode)
            {
                node.setNode((KeyNode)keyNode, convertSection(defaultEntry.getValue(), fieldMap.get(defaultEntry.getKey()), reflected));
            }
            else
            {
                throw new UnsupportedReflectedException("Node is not a KeyNode! " + keyNode);
            }
        }
        return node;
    }

    private Node convertSectionCollectionField(Reflected reflected, Collection<Section> fieldValue)
    {
        ListNode node = ListNode.emptyList();
        for (Section subSection : fieldValue)
        {
            node.addNode(convertSection(subSection, subSection, reflected));
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    private Node convertSectionField(Field field, Section defaultSection, Section section, Reflected reflected, Object fieldValue, Object defaultValue) throws IllegalAccessException, ConversionException
    {
        if (fieldValue == null && defaultValue == null)
        {
            defaultValue = SectionFactory.newSectionInstance((Class<? extends Section>)field.getType(), defaultSection);
            field.set(defaultSection, defaultValue);
            if (section == defaultSection)
            {
                fieldValue = defaultValue;
            }
        }
        if (fieldValue == null)
        {
            dumpDefaultIntoField(defaultSection, section, field, reflected);
            fieldValue = field.get(section);
        }
        return convertSection((Section)defaultValue, (Section)fieldValue, reflected);
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
    public static FieldType getFieldType(Field field)
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
            if (hasSectionTypeArgument(Collection.class, 0, pType))
            {
                fieldType = SECTION_COLLECTION;
            }
            else if (hasSectionTypeArgument(Map.class, 1, pType))
            {
                fieldType = SECTION_MAP;
            }
        }
        return fieldType;
    }

    private static boolean hasSectionTypeArgument(Class<?> clazz, int i, ParameterizedType pType)
    {
        if (clazz.isAssignableFrom((Class)pType.getRawType()))
        {
            Type subType = pType.getActualTypeArguments()[i];
            return (subType instanceof Class && SectionFactory.isSectionClass((Class)subType));
        }
        return false;
    }

    /**
     * Detects if given field needs to be serialized
     *
     * @param field the field to check
     *
     * @return whether the field is a field of the reflected that needs to be serialized
     */
    public static boolean isReflectedField(Field field)
    {
        int modifiers = field.getModifiers();
        return !(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers));
    }

    protected static ReflectedPath getPathFor(Field field)
    {
        if (field.isAnnotationPresent(Name.class))
        {
            return ReflectedPath.forName(field.getAnnotation(Name.class).value());
        }
        return ReflectedPath.forName(StringUtils.fieldNameToPath(field.getName()));
    }
}
