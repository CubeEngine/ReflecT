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

import de.cubeisland.engine.configuration.Configuration;
import de.cubeisland.engine.configuration.FieldType;
import de.cubeisland.engine.configuration.InvalidConfigurationException;
import de.cubeisland.engine.configuration.annotations.Comment;
import de.cubeisland.engine.configuration.annotations.MapComment;
import de.cubeisland.engine.configuration.annotations.MapComments;
import de.cubeisland.engine.configuration.annotations.Option;
import de.cubeisland.engine.configuration.convert.converter.generic.MapConverter;
import de.cubeisland.engine.configuration.node.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import static de.cubeisland.engine.configuration.Configuration.convertToNode;
import static de.cubeisland.engine.configuration.Configuration.convertFromNode;
import static de.cubeisland.engine.configuration.FieldType.*;

/**
 * This abstract Codec can be implemented to read and write configurations.
 */
public abstract class ConfigurationCodec
{
    protected static final String PATH_SEPARATOR = ":";

    /**
     * Loads in the given configuration using the InputStream
     *
     * @param config the config to load
     * @param is the InputStream to load from
     */
    public Collection<ErrorNode> load(Configuration config, InputStream is)
    {
        return this.dumpIntoFields(config, this.loadFromInputStream(is));
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
            this.saveIntoFile(config, this.fillFromFields(config), file);
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
            Comment comment = field.getAnnotation(Comment.class);
            node.setComment(comment.value());
        }
    }

    /**
     * Adds declared map-comments of the configuration
     *
     * @param baseNode the baseNode containing all nodes
     * @param configClass the class of the configuration
     */
    protected void addMapComments(MapNode baseNode, Class<? extends Configuration> configClass)
    {
        if (configClass.isAnnotationPresent(MapComments.class))
        {
            MapComment[] mapComments = configClass.getAnnotation(MapComments.class).value();
            for (MapComment comment : mapComments)
            {
                Node nodeAt = baseNode.getNodeAt(comment.path(), "."); // TODO option to not create the Node
                if (nodeAt != null) // if null ignore comment
                {
                    if (nodeAt instanceof NullNode || nodeAt instanceof ErrorNode)
                    {
                        nodeAt.getParentNode().removeNode(nodeAt);
                    }
                    else
                    {
                        nodeAt.setComment(comment.text());
                    }
                }
            }
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
        FieldType fieldType = NORMAL_FIELD;
        if (Configuration.class.isAssignableFrom(field.getType()))
        {
            return CONFIG_FIELD;
        }
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType)
        {
            ParameterizedType pType = (ParameterizedType)type;
            if (Collection.class.isAssignableFrom((Class)pType.getRawType()))
            {
                Type subType1 = pType.getActualTypeArguments()[0];
                if (subType1 instanceof Class && Configuration.class.isAssignableFrom((Class)subType1))
                {
                    return COLLECTION_CONFIG_FIELD;
                }
            }

            if (Map.class.isAssignableFrom((Class)pType.getRawType()))
            {
                Type subType2 = pType.getActualTypeArguments()[1];
                if (subType2 instanceof Class && Configuration.class.isAssignableFrom((Class)subType2))
                {
                    return MAP_CONFIG_FIELD;
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
        return field.isAnnotationPresent(Option.class);
    }

    /**
     * Dumps the contents of the MapNode into the fields odf the configuration
     *
     * @param config the configuration
     * @param currentNode the current MapNode
     */
    @SuppressWarnings("unchecked cast")
    protected Collection<ErrorNode> dumpIntoFields(Configuration config, MapNode currentNode)
    {
        Collection<ErrorNode> errorNodes = new HashSet<>();
        for (Field field : config.getClass().getFields()) // ONLY public fields are allowed
        {
            try
            {
                if (isConfigField(field))
                {
                    String path = field.getAnnotation(Option.class).value().replace(".", PATH_SEPARATOR);
                    Node fieldNode = currentNode.getNodeAt(path, PATH_SEPARATOR);
                    if (fieldNode instanceof ErrorNode)
                    {
                        errorNodes.add((ErrorNode) fieldNode);
                        continue;
                    }
                    Type type = field.getGenericType();
                    FieldType fieldType = getFieldType(field);
                    switch (fieldType)
                    {
                    case NORMAL_FIELD:
                        if (!(fieldNode instanceof NullNode))
                        {
                            Object object = convertFromNode(fieldNode, type); // Convert the value
                            if (object != null)
                            {
                                field.set(config, object);//Set loaded Value into Field
                            }
                        }
                        continue;
                    case CONFIG_FIELD:
                        MapNode singleConfigNode;
                        Configuration singleSubConfig = (Configuration)field.get(config); // Get Config from field
                        if (singleSubConfig == null)
                        {
                            singleSubConfig = (Configuration)field.getType().newInstance(); // create new if null
                            field.set(config, singleSubConfig); // Set new instance
                        }
                        if (fieldNode instanceof MapNode)
                        {
                            singleConfigNode = (MapNode)fieldNode;
                        }
                        else if (fieldNode instanceof NullNode) // Empty Node
                        {
                            singleConfigNode = MapNode.emptyMap(); // Create Empty Map
                            currentNode.setNodeAt(path, PATH_SEPARATOR, singleConfigNode); // and attach
                        }
                        else
                        {
                            throw new InvalidConfigurationException("Invalid Node for Configuration at " + path +
                                                                        "\nConfig:" + config.getClass() +
                                                                        "\nSubConfig:" + singleSubConfig.getClass());
                        }
                        errorNodes.addAll(this.dumpIntoFields(singleSubConfig, singleConfigNode));
                        continue;
                    case COLLECTION_CONFIG_FIELD:
                        ListNode loadFrom_List;
                        if (fieldNode instanceof ListNode)
                        {
                            loadFrom_List = (ListNode)fieldNode;
                        }
                        else if (fieldNode instanceof NullNode) // Empty Node
                        {
                            loadFrom_List = ListNode.emptyList(); // Create Empty List
                            currentNode.setNodeAt(path, PATH_SEPARATOR, loadFrom_List); // and attach
                        }
                        else
                        {
                            throw new InvalidConfigurationException("Invalid Node for List-Configurations at " + path +
                                                                        "\nConfig:" + config.getClass());
                        }
                        Collection<Configuration> subConfigs = (Collection<Configuration>)field.get(config);;
                        // Now iterate through the subConfigs
                        Iterator<Node> loadFrom_Iterator = loadFrom_List.getListedNodes().iterator();
                        for (Configuration subConfig : subConfigs)
                        {
                            Node listElemNode;
                            if (loadFrom_Iterator.hasNext())
                            {
                                listElemNode = loadFrom_Iterator.next();
                                if (listElemNode instanceof NullNode)
                                {
                                    listElemNode = MapNode.emptyMap();
                                    loadFrom_List.addNode(listElemNode);
                                }
                            }
                            else
                            {
                                listElemNode = MapNode.emptyMap();
                                loadFrom_List.addNode(listElemNode);
                            }
                            if (listElemNode instanceof MapNode)
                            {
                                errorNodes.addAll(this.dumpIntoFields(subConfig, (MapNode) listElemNode));
                            }
                            else
                            {
                                throw new InvalidConfigurationException("Invalid Node for List-Configurations at " + path +
                                                                            "\nConfig:" + config.getClass() +
                                                                            "\nSubConfig:" + subConfig.getClass());
                            }
                        }
                        continue;
                    case MAP_CONFIG_FIELD:
                        MapNode loadFrom_Map;
                        if (fieldNode instanceof MapNode)
                        {
                            loadFrom_Map = (MapNode)fieldNode;
                        }
                        else if (fieldNode instanceof NullNode) // Empty Node
                        {
                            loadFrom_Map = MapNode.emptyMap(); // Create Empty List
                            currentNode.setNodeAt(path, PATH_SEPARATOR, loadFrom_Map); // and attach
                        }
                        else
                        {
                            throw new InvalidConfigurationException("Invalid Node for Map-Configurations at " + path +
                                                                        "\nConfig:" + config.getClass());
                        }
                        Class<? extends Configuration> clazz = (Class<? extends Configuration>)((ParameterizedType)type).getActualTypeArguments()[1];
                        Map<Object, Configuration> configs = MapConverter.getMapFor((ParameterizedType) type);
                        for (Entry<String, Node> entry : loadFrom_Map.getMappedNodes().entrySet())
                        {
                            Node keyNode = convertToNode(entry.getKey());
                            Node valueNode = loadFrom_Map.getNodeAt(entry.getKey(), PATH_SEPARATOR);
                            Object key = convertFromNode(keyNode, ((ParameterizedType) type).getActualTypeArguments()[0]);
                            Configuration value;
                            if (valueNode instanceof NullNode)
                            {
                                value = clazz.newInstance();

                            }
                            else if (valueNode instanceof MapNode)
                            {
                                errorNodes.addAll(this.dumpIntoFields(value = clazz.newInstance(), (MapNode) valueNode));
                            }
                            else
                            {
                                throw new InvalidConfigurationException("Invalid Value-Node for Map of de.cubeisland.engine.configuration.Configuration at " + path +
                                                                            "\nConfig:" + config.getClass() +
                                                                            "\nSubConfig:" + clazz);
                            }
                            configs.put(key,value);
                            field.set(config,configs);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                throw new InvalidConfigurationException(
                    "Error while dumping loaded config into fields!" +
                        "\ncurrent Configuration: " + config.getClass().toString() +
                        "\ncurrent Field:" + field.getName() +
                        "\ncurrent Node: " + currentNode.toString() , e);
            }
        }
        return errorNodes;
    }



    /**
     * Fills a MapNode with values from the fields to later save
     *
     * @param config the configuration
     */
    @SuppressWarnings("unchecked cast")
    public <C extends Configuration> MapNode fillFromFields(C config)
    {
        MapNode baseNode = MapNode.emptyMap();
        Class<C> configClass = (Class<C>) config.getClass();

        boolean advanced = true;
        try
        // to get a boolean advanced field (if not found ignore)
        {
            Field field = configClass.getField("advanced");
            advanced = field.getBoolean(config);
        }
        catch (Exception ignored)
        {}
        for (Field field : configClass.getFields())
        {
            if (isConfigField(field))
            {
                if (!advanced && field.getAnnotation(Option.class).advanced())
                {
                    continue;
                }
                this.fillFromField(field, config, baseNode);
            }
        }
        this.addMapComments(baseNode, config.getClass());
        return baseNode;
    }

    /**
     * Fills the values of the given field into the MapNode
     *
     * @param field the field to get the values from
     * @param config the configuration containing the values
     * @param baseNode the MapNode to insert the values into
     */
    @SuppressWarnings("unchecked cast")
    protected void fillFromField(Field field, Configuration config, MapNode baseNode)
    {
        String path = field.getAnnotation(Option.class).value().replace(".", PATH_SEPARATOR);
        try
        {
            Object fieldValue = field.get(config);
            FieldType fieldType = getFieldType(field);
            Node node = null;
            switch (fieldType)
            {
                case NORMAL_FIELD:
                    node = convertToNode(fieldValue);
                    break;
                case CONFIG_FIELD:
                    node = this.fillFromFields((Configuration)fieldValue);
                    break;
                case COLLECTION_CONFIG_FIELD:
                    node = ListNode.emptyList();
                    for (Configuration subConfig : (Collection<Configuration>)fieldValue)
                    {
                        MapNode collectionConfigNode = this.fillFromFields(subConfig);
                        ((ListNode)node).addNode(collectionConfigNode);
                    }
                    break;
                case MAP_CONFIG_FIELD:
                    node = MapNode.emptyMap();
                    Map<Object, Configuration> fieldMap = (Map<Object, Configuration>)fieldValue;
                    for (Map.Entry<Object, Configuration> entry : fieldMap.entrySet())
                    {
                        Node keyNode = convertToNode(entry.getKey());
                        if (keyNode instanceof StringNode)
                        {
                            MapNode mapConfigNode = this.fillFromFields(entry.getValue());
                            ((MapNode)node).setNode((StringNode)keyNode, mapConfigNode);
                        }
                        else
                        {
                            throw new InvalidConfigurationException("Invalid Key-Node for Map of Configuration at " + path
                                            + "\nConfiguration:" + config.getClass());
                        }
                    }
            }
            this.addComment(node, field);
            baseNode.setNodeAt(path, PATH_SEPARATOR, node);
        }
        catch (Exception e)
        {
            throw InvalidConfigurationException.of(
                    "Error while dumping loaded config into fields!",
                    config.getPath(), path, config.getClass(), field, e);
        }
    }

}
