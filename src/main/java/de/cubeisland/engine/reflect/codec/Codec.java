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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import de.cubeisland.engine.reflect.Reflected;
import de.cubeisland.engine.reflect.Section;
import de.cubeisland.engine.reflect.util.SectionFactory;
import de.cubeisland.engine.reflect.annotations.Comment;
import de.cubeisland.engine.reflect.annotations.Name;
import de.cubeisland.engine.reflect.codec.converter.Converter;
import de.cubeisland.engine.reflect.codec.converter.generic.CollectionConverter;
import de.cubeisland.engine.reflect.codec.converter.generic.MapConverter;
import de.cubeisland.engine.reflect.exception.ConversionException;
import de.cubeisland.engine.reflect.exception.FieldAccessException;
import de.cubeisland.engine.reflect.exception.InvalidReflectedObjectException;
import de.cubeisland.engine.reflect.exception.UnsupportedReflectedException;
import de.cubeisland.engine.reflect.node.ErrorNode;
import de.cubeisland.engine.reflect.node.KeyNode;
import de.cubeisland.engine.reflect.node.ListNode;
import de.cubeisland.engine.reflect.node.MapNode;
import de.cubeisland.engine.reflect.node.Node;
import de.cubeisland.engine.reflect.node.NullNode;
import de.cubeisland.engine.reflect.node.ReflectedPath;
import de.cubeisland.engine.reflect.node.StringNode;
import de.cubeisland.engine.reflect.util.StringUtils;

import static de.cubeisland.engine.reflect.codec.FieldType.*;

/**
 * This abstract Codec can be implemented to read and write reflected objects that allow child-reflected
 */
public abstract class Codec<Input, Output>
{
    private static final String[] NO_COMMENT = new String[0];

    private final Map<Class<? extends Section>, Field[]> cachedFields = new HashMap<Class<? extends Section>, Field[]>();
    private final Map<Field, ReflectedPath> paths = new HashMap<Field, ReflectedPath>();
    private final Map<Field, String[]> comments = new HashMap<Field, String[]>();

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
     * Returns the {@link ConverterManager} for this codec, allowing to register custom {@link Converter} for this codec only
     *
     * @return the ConverterManager
     *
     * @throws UnsupportedOperationException if the Codec was not instantiated by the Factory
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
     * Loads in the given {@link Reflected} using the <code>Input</code>
     *
     * @param reflected the Reflected to load
     * @param input     the Input to load from
     *
     * @return a collection of all erroneous Nodes
     */
    public abstract Collection<ErrorNode> loadReflected(Reflected reflected, Input input);

    /**
     * Saves the {@link Reflected} using given <code>Output</code>
     *
     * @param reflected the Reflected to save
     * @param output    the Output to save into
     */
    public abstract void saveReflected(Reflected reflected, Output output);

    // PROTECTED ABSTRACT Methods

    /**
     * Saves the values contained in the {@link MapNode} using given <code>Output</code>
     *
     * @param node      the MapNode containing all data to save
     * @param out       the Output to save to
     * @param reflected the Reflected
     */
    protected abstract void save(MapNode node, Output out, Reflected reflected) throws ConversionException;

    /**
     * Converts the <code>Input</code> into a {@link MapNode}
     *
     * @param in        the Input to load from
     * @param reflected the Reflected
     */
    protected abstract MapNode load(Input in, Reflected reflected) throws ConversionException;

    // Reflected loading Methods

    /**
     * Dumps the contents of the {@link MapNode} into the Fields of the Section
     * <p>falling back to using the default Section if a node is not given
     *
     * @param defaultSection the default Section
     * @param section        the Section
     * @param currentNode    the Node to load from
     * @param reflected      the Reflected containing the Section
     *
     * @return a collection of all erroneous Nodes
     */
    protected final Collection<ErrorNode> dumpIntoSection(Section defaultSection, Section section, MapNode currentNode, Reflected reflected)
    {
        Section dSection = defaultSection == null ? section : defaultSection;
        if (!dSection.getClass().equals(section.getClass()))
        {
            throw new IllegalArgumentException("defaultSection and section have to be the same type of section!");
        }
        Collection<ErrorNode> errorNodes = new HashSet<ErrorNode>();
        // ONLY public fields are allowed
        for (Field field : this.getSectionFields(section.getClass()))
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
                    catch (InvalidReflectedObjectException e)
                    {
                        // rethrow
                        throw e;
                    }
                    catch (IllegalAccessException e)
                    {
                        throw FieldAccessException.of(getPathFor(field), section.getClass(), field, e);
                    }
                    catch (ConversionException e)
                    {
                        // non-fatal ConversionException
                        InvalidReflectedObjectException ex = InvalidReflectedObjectException.of("Error while converting Node to dump into field!", getPathFor(field), section.getClass(), field, e);
                        if (reflected.useStrictExceptionPolicy())
                        {
                            throw ex;
                        }
                        reflected.getLogger().log(Level.WARNING, ex.getMessage(), ex);
                    }
                    catch (RuntimeException e)
                    {
                        throw InvalidReflectedObjectException.of("Unknown Error while dumping loaded reflected into fields", getPathFor(field), section.getClass(), field, e);
                    }
                }
            }
        }
        return errorNodes;
    }

    /**
     * Copy the contents of the default Section into a field of the Section
     *
     * @param parentSection the default Section
     * @param section       the Section
     * @param field         the Field to write the value into
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
     * Dumps the contents of the {@link Node} into a Field of the Section
     *
     * @param defaultSection the default Section
     * @param section        the Section
     * @param field          the Field to write the value into
     * @param fieldNode      the Node to get the value from
     * @param reflected      the Reflected containing the Section
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
            fieldValue = getNormalFieldValue(defaultSection, section, field, fieldNode, reflected, type);
        }
        else if (fieldType == FieldType.SECTION)
        {
            if (fieldNode instanceof MapNode)
            {
                fieldValue = getSectionFieldValue(defaultSection, section, field, (MapNode)fieldNode, reflected, errorNodes, defaultValue);
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
                fieldValue = getSectionCollectionFieldValue(section, (ListNode)fieldNode, reflected, errorNodes, (ParameterizedType)type);
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
                fieldValue = getSectionMapFieldValue(defaultSection, section, field, (MapNode)fieldNode, reflected, errorNodes, (ParameterizedType)type);
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

    /**
     * Gets the converted value of the {@link MapNode} to write into the Field
     * <p>The Field being a Map with Sections
     *
     * @param defaultSection the default Section
     * @param section        the Section
     * @param field          the Field to write the value into
     * @param fieldNode      the Node to get the value from
     * @param reflected      the Reflected
     * @param errorNodes     the current erroneous Nodes
     * @param type           the ParametrizedType of given Field
     *
     * @return the value to write into the Field
     */
    @SuppressWarnings("unchecked")
    private Object getSectionMapFieldValue(Section defaultSection, Section section, Field field, MapNode fieldNode, Reflected reflected, Collection<ErrorNode> errorNodes, ParameterizedType type) throws IllegalAccessException, ConversionException
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

    /**
     * Gets the converted value of the {@link ListNode} to write into the Field
     * <p>The Field being a Collection with Sections
     *
     * @param section    the Section
     * @param fieldNode  the Node to get the value from
     * @param reflected  the Reflected
     * @param errorNodes the current erroneous Nodes
     * @param type       the ParametrizedType of given Field
     *
     * @return the value to write into the Field
     */
    @SuppressWarnings("unchecked")
    private Object getSectionCollectionFieldValue(Section section, ListNode fieldNode, Reflected reflected, Collection<ErrorNode> errorNodes, ParameterizedType type) throws ConversionException
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

    /**
     * Gets the converted value of the {@link MapNode} to write into the Field
     * <p>The Field being a Section
     *
     * @param defaultSection the default Section
     * @param section        the Section
     * @param field          the Field to write the value into
     * @param fieldNode      the Node to get the value from
     * @param reflected      the Reflected
     * @param errorNodes     the current erroneous Nodes
     * @param defaultValue   the default Value
     *
     * @return the value to write into the Field
     */
    @SuppressWarnings("unchecked")
    private Object getSectionFieldValue(Section defaultSection, Section section, Field field, MapNode fieldNode, Reflected reflected, Collection<ErrorNode> errorNodes, Object defaultValue)
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

    /**
     * Gets the converted value of the {@link Node} to write into the Field
     *
     * @param defaultSection the default Section
     * @param section        the Section
     * @param field          the Field to write the value into
     * @param fieldNode      the Node to get the value from
     * @param reflected      the Reflected
     * @param type           the ParametrizedType of given Field
     *
     * @return the value to write into the Field
     */
    private Object getNormalFieldValue(Section defaultSection, Section section, Field field, Node fieldNode, Reflected reflected, Type type) throws ConversionException, IllegalAccessException
    {
        // Convert the value
        Object fieldValue = converterManager.convertFromNode(fieldNode, type);
        if (fieldValue == null && !(section == defaultSection))
        {
            fieldValue = field.get(defaultSection);
            reflected.addInheritedField(field);
        }
        return fieldValue;
    }

    // Reflected saving Methods

    /**
     * Fills a MapNode with the values of given Section
     *
     * @param defaultSection the default Section
     * @param section        the Section
     * @param reflected      the Reflected
     *
     * @return the Section converted into a MapNode
     */
    protected final MapNode convertSection(Section defaultSection, Section section, Reflected reflected)
    {
        MapNode baseNode = MapNode.emptyMap();
        if (!defaultSection.getClass().equals(section.getClass()))
        {
            throw new IllegalArgumentException("defaultSection and section have to be the same type of section!");
        }
        Class<? extends Section> sectionClass = section.getClass();

        for (Field field : this.getSectionFields(sectionClass))
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
                catch (InvalidReflectedObjectException e)
                {
                    // rethrow
                    throw e;
                }
                catch (IllegalAccessException e)
                {
                    throw FieldAccessException.of(getPathFor(field), sectionClass, field, e);
                }
                catch (ConversionException e)
                {
                    // fatal ConversionException
                    throw InvalidReflectedObjectException.of("Could not convert Field into Node!", getPathFor(field), section.getClass(), field, e);
                }
                catch (RuntimeException e)
                {
                    throw InvalidReflectedObjectException.of("Unknown Error while converting Section!", getPathFor(field), section.getClass(), field, e);
                }
            }
        }
        if (section != defaultSection)
        {
            // remove generated empty ParentNodes ONLY from child-reflected
            baseNode.cleanUpEmptyNodes();
        }
        return baseNode;
    }

    private Field[] getSectionFields(Class<? extends Section> clazz)
    {
        Field[] fields = this.cachedFields.get(clazz);
        if (fields == null)
        {
            fields = clazz.getFields();
            this.cachedFields.put(clazz, fields);
        }
        return fields;
    }

    /**
     * Fills a Node with the values of Field
     *
     * @param field          the Field to get the value from
     * @param defaultSection the default Section
     * @param section        the Section
     * @param reflected      the Reflected
     *
     * @return the Field converted into a Node
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
            node = convertSectionCollection(reflected, (Collection<Section>)fieldValue);
            break;
        case SECTION_MAP:
            node = convertSectionMap(reflected, (Map<Object, Section>)fieldValue, (Map<Object, Section>)defaultValue);
            break;
        default:
            throw new IllegalArgumentException("Invalid FieldType!");
        }
        addComment(node, field);
        return node;
    }

    /**
     * Fills a Node with the value of given Map with Sections
     *
     * @param reflected         the Reflected
     * @param fieldValue        the Map
     * @param defaultFieldValue the default Map
     *
     * @return the Map converted into a Node
     */
    private Node convertSectionMap(Reflected reflected, Map<Object, Section> fieldValue, Map<Object, Section> defaultFieldValue) throws ConversionException
    {
        MapNode node = MapNode.emptyMap();
        for (Entry<Object, Section> defaultEntry : defaultFieldValue.entrySet())
        {
            Node keyNode = converterManager.convertToNode(defaultEntry.getKey());
            if (keyNode instanceof KeyNode)
            {
                node.setNode((KeyNode)keyNode, convertSection(defaultEntry.getValue(), fieldValue.get(defaultEntry.getKey()), reflected));
            }
            else
            {
                throw new UnsupportedReflectedException("Node is not a KeyNode! " + keyNode);
            }
        }
        return node;
    }

    /**
     * Fills a Node with the value of given Collection of Sections
     *
     * @param reflected  the Reflected
     * @param fieldValue the Collection
     *
     * @return the Collection converted into a Node
     */
    private Node convertSectionCollection(Reflected reflected, Collection<Section> fieldValue)
    {
        ListNode node = ListNode.emptyList();
        for (Section subSection : fieldValue)
        {
            node.addNode(convertSection(subSection, subSection, reflected));
        }
        return node;
    }

    /**
     * Fills a Node with the value of given Object
     *
     * @param field          the Field to get the value from
     * @param defaultSection the default Section
     * @param section        the Section
     * @param reflected      the Reflected
     * @param fieldValue     the Object
     * @param defaultValue   the default Object
     *
     * @return the Object converted into a Node
     */
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
        String[] comment = comments.get(field);
        if (comment == null)
        {
            if (field.isAnnotationPresent(Comment.class))
            {
                comment = field.getAnnotation(Comment.class).value();
            }
            else
            {
                comment = NO_COMMENT;
            }
            this.comments.put(field, comment);
        }
        if (comment.length != 0)
        {
            node.setComments(comment);
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

    /**
     * Checks if the ParameterizedType has given Class as nth TypeArgument
     *
     * @param clazz the Class to check
     * @param n     the index
     * @param pType the ParameterizedType
     *
     * @return true if the given class is assignable from the nth TypeArgument
     */
    private static boolean hasSectionTypeArgument(Class<?> clazz, int n, ParameterizedType pType)
    {
        if (clazz.isAssignableFrom((Class)pType.getRawType()))
        {
            Type subType = pType.getActualTypeArguments()[n];
            return subType instanceof Class && SectionFactory.isSectionClass((Class)subType);
        }
        return false;
    }

    /**
     * Detects if given field needs to be serialized
     * <p>static and transient Field do not get converted
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

    /**
     * Constructs a path for given Field
     *
     * @param field the field to get the path for
     *
     * @return the ReflectedPath
     */
    protected ReflectedPath getPathFor(Field field)
    {
        ReflectedPath path = this.paths.get(field);
        if (path == null)
        {
            if (field.isAnnotationPresent(Name.class))
            {
                path = ReflectedPath.forName(field.getAnnotation(Name.class).value());
            }
            else
            {
                path = ReflectedPath.forName(StringUtils.fieldNameToPath(field.getName()));
            }
            this.paths.put(field, path);
        }
        return path;
    }
}
