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
import de.cubeisland.engine.configuration.MultiConfiguration;
import de.cubeisland.engine.configuration.annotations.Option;
import de.cubeisland.engine.configuration.convert.converter.generic.CollectionConverter;
import de.cubeisland.engine.configuration.convert.converter.generic.MapConverter;
import de.cubeisland.engine.configuration.node.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import static de.cubeisland.engine.configuration.Configuration.*;

/**
 * This abstract Codec can be implemented to read and write configurations that allow child-configs
 */
public abstract class MultiConfigurationCodec extends ConfigurationCodec
{
    /**
     * Saves a configuration with another configuration s parent
     *
     * @param parentConfig the parent configuration
     * @param config the configuration to save
     * @param file the file to save into
     */
    public void saveChildConfig(MultiConfiguration parentConfig, MultiConfiguration config, Path file)
    {
        try
        {
            if (file == null)
            {
                throw new IllegalStateException("Tried to save config without File.");
            }
            this.saveIntoFile(config, this.fillFromFields(parentConfig, config), file);
        }
        catch (Exception ex)
        {
            throw new InvalidConfigurationException("Error while saving de.cubeisland.engine.configuration.Configuration!", ex);
        }
    }

    /**
     * Loads in the given configuration using the InputStream
     *
     * @param config the config to load
     * @param is the InputStream to load from
     */
    public Collection<ErrorNode> loadChildConfig(MultiConfiguration config, InputStream is) throws InstantiationException, IllegalAccessException
    {
        return this.dumpIntoFields(config, this.loadFromInputStream(is), config.getParent());
    }

    /**
     * Dumps the values from given Node into the fields of the Configuration
     *
     * @param config the config
     * @param currentNode the Node to load from
     * @param parentConfig the optional parentConfig
     */
    @SuppressWarnings("unchecked cast")
    protected Collection<ErrorNode> dumpIntoFields(MultiConfiguration config, MapNode currentNode, MultiConfiguration parentConfig)
    {
        if (parentConfig == null)
        {
            return this.dumpIntoFields(config,currentNode);
        }
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
                        if (fieldNode instanceof NullNode)
                        {
                            field.set(config, field.get(parentConfig));
                            config.addinheritedField(field);
                        }
                        else
                        {
                            Object object = convertFromNode(fieldNode, type); // Convert the value
                            if (object != null)
                            {
                                field.set(config, object);//Set loaded Value into Field
                            }
                            else // If not loaded but is child-config get from parent-config
                            {
                                field.set(config, field.get(parentConfig));
                                config.addinheritedField(field);
                            }
                        }
                        continue;
                    case CONFIG_FIELD:
                        MapNode loadFrom_singleConfig;
                        MultiConfiguration singleSubConfig = (MultiConfiguration)field.get(config); // Get Config from field
                        if (singleSubConfig == null)
                        {
                            singleSubConfig = (MultiConfiguration)field.getType().newInstance(); // create new if null
                            field.set(config, singleSubConfig); // Set new instance
                        }
                        if (fieldNode instanceof MapNode)
                        {
                            loadFrom_singleConfig = (MapNode)fieldNode;
                        }
                        else if (fieldNode instanceof NullNode) // Empty Node
                        {
                            loadFrom_singleConfig = MapNode.emptyMap(); // Create Empty Map
                            currentNode.setNodeAt(path, PATH_SEPARATOR, loadFrom_singleConfig); // and attach
                        }
                        else
                        {
                            throw new InvalidConfigurationException("Invalid Node for Configuration at " + path +
                                                                        "\nConfig:" + config.getClass() +
                                                                        "\nSubConfig:" + singleSubConfig.getClass());
                        }
                        errorNodes.addAll(this.dumpIntoFields(singleSubConfig, loadFrom_singleConfig,(MultiConfiguration)field.get(parentConfig)));
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
                                                                        "\nConfiguration:" + config.getClass());
                        }
                        Collection<MultiConfiguration> parentSubConfigs = (Collection<MultiConfiguration>)field.get(parentConfig);
                        Collection<MultiConfiguration> subConfigs = CollectionConverter
                            .getCollectionFor((ParameterizedType)type);
                        for (MultiConfiguration configuration : parentSubConfigs)
                        {
                            subConfigs.add(configuration.getClass().newInstance());
                        }
                        field.set(config, subConfigs);
                        Iterator<MultiConfiguration> parentConfig_Iterator = parentSubConfigs.iterator();
                        // Now iterate through the subConfigs
                        Iterator<Node> loadFrom_Iterator = loadFrom_List.getListedNodes().iterator();
                        for (MultiConfiguration subConfig : subConfigs)
                        {
                            Node loadFrom_listElem;
                            if (loadFrom_Iterator.hasNext())
                            {
                                loadFrom_listElem = loadFrom_Iterator.next();
                                if (loadFrom_listElem instanceof NullNode)
                                {
                                    loadFrom_listElem = MapNode.emptyMap();
                                    loadFrom_List.addNode(loadFrom_listElem);
                                }
                            }
                            else
                            {
                                loadFrom_listElem = MapNode.emptyMap();
                                loadFrom_List.addNode(loadFrom_listElem);
                            }
                            if (loadFrom_listElem instanceof MapNode)
                            {
                                errorNodes.addAll(this.dumpIntoFields(subConfig, (MapNode)loadFrom_listElem, parentConfig_Iterator.next()));
                            }
                            else
                            {
                                throw new InvalidConfigurationException("Invalid Node for List-Configurations at " + path +
                                                                            "\nConfiguration:" + config.getClass() +
                                                                            "\nSubConfiguration:" + subConfig.getClass());
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
                                                                        "\nConfiguration:" + config.getClass());
                        }
                        Map<Object, MultiConfiguration> mapConfigs = MapConverter.getMapFor((ParameterizedType)type);
                        Map<Object, MultiConfiguration> parentMapConfigs = (Map<Object, MultiConfiguration>)field.get(parentConfig);
                        for (Map.Entry<Object, MultiConfiguration> entry : parentMapConfigs.entrySet())
                        {
                            mapConfigs.put(entry.getKey(), entry.getValue().getClass().newInstance());
                        }
                        field.set(config, mapConfigs);
                        for (Map.Entry<Object, MultiConfiguration> entry : mapConfigs.entrySet())
                        {
                            Node keyNode = convertToNode(entry.getKey());
                            if (keyNode instanceof StringNode)
                            {
                                Node valueNode = loadFrom_Map.getNodeAt(((StringNode)keyNode).getValue(), PATH_SEPARATOR);
                                if (valueNode instanceof NullNode)
                                {
                                    valueNode = MapNode.emptyMap();
                                    loadFrom_Map.setNode((StringNode)keyNode, valueNode);
                                }
                                if (valueNode instanceof MapNode)
                                {
                                    errorNodes.addAll(this.dumpIntoFields(entry.getValue(), (MapNode)valueNode, parentMapConfigs.get(entry.getKey())));
                                }
                                else
                                {
                                    throw new InvalidConfigurationException("Invalid Value-Node for Map of Configuration at " + path +
                                                                                "\nConfiguration:" + config.getClass() +
                                                                                "\nSubConfiguration:" + entry.getValue().getClass());
                                }
                            }
                            else
                            {
                                throw new InvalidConfigurationException("Invalid Key-Node for Map of at " + path +
                                                                            "\nConfiguration:" + config.getClass() +
                                                                            "\nSubConfiguration:" + entry.getValue().getClass());
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                throw new InvalidConfigurationException(
                    "Error while dumping loaded config into fields!" +
                        "\ncurrent Configuration: " + config.getClass().toString() +
                        "\ncurrent Field:" + field.getName(), e);
            }
        }
        return errorNodes;
    }

    /**
     * Fills the map with values from the Fields to save
     *
     * @param parentConfig the parent config
     * @param config the config
     */
    @SuppressWarnings("unchecked cast")
    public <C extends MultiConfiguration> MapNode fillFromFields(C parentConfig, C config)
    {
        MapNode baseNode = MapNode.emptyMap();
        if (parentConfig != null)
        {
            if (!parentConfig.getClass().equals(config.getClass()))
            {
                throw new IllegalStateException("parent and child-config have to be the same type of config!");
            }
        }
        else
        {
            return this.fillFromFields(config);
        }
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
            if (config.isInheritedField(field))
            {
                continue;
            }
            if (isConfigField(field))
            {
                if (!advanced && field.getAnnotation(Option.class).advanced())
                {
                    continue;
                }
                String path = field.getAnnotation(Option.class).value().replace(".", PATH_SEPARATOR);
                this.fillFromField(field,parentConfig,config,baseNode,path);
            }
        }
        this.addMapComments(baseNode, config.getClass());
        baseNode.cleanUpEmptyNodes();
        return baseNode;
    }

    @SuppressWarnings("unchecked cast")
    protected void fillFromField(Field field, Configuration parentConfig, Configuration config, MapNode baseNode, String path)
    {
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
                    node = this.fillFromFields((MultiConfiguration)field.get(parentConfig), (MultiConfiguration)fieldValue);
                    break;
                case COLLECTION_CONFIG_FIELD:
                    throw new InvalidConfigurationException("ChildConfigs are not allowed for Configurations in Collections" +
                                                                "\nConfig:" + config.getClass());
                case MAP_CONFIG_FIELD:
                    node = MapNode.emptyMap();
                    Map<Object, MultiConfiguration> parentFieldMap = (Map<Object, MultiConfiguration>)field.get(parentConfig);
                    Map<Object, MultiConfiguration> childFieldMap = MapConverter.getMapFor((ParameterizedType)field.getGenericType());
                    for (Map.Entry<Object, MultiConfiguration> parentEntry : parentFieldMap.entrySet())
                    {
                        Node keyNode = convertToNode(parentEntry.getKey());
                        if (keyNode instanceof StringNode)
                        {
                            MapNode configNode = this.fillFromFields(parentEntry.getValue(), childFieldMap.get(parentEntry.getKey()));
                            ((MapNode)node).setNode((StringNode)keyNode, configNode);
                        }
                        else
                        {
                            throw new InvalidConfigurationException("Invalid Key-Node for Map of Configuration at " + path +
                                                                        "\nConfiguration:" + config.getClass());
                        }
                    }
                }
                this.addComment(node, field);
                baseNode.setNodeAt(path, PATH_SEPARATOR, node);
        }
        catch (Exception e)
        {
            throw new InvalidConfigurationException(
                "Error while dumping loaded config into fields!" +
                    "\ncurrent Configuration: " + config.getClass().toString() +
                    "\ncurrent Field:" + field.getName(), e);
        }
    }
}
