ConfigurationAPI
================

A Configuration API for Java

# Using the library

## Dependencies

### Maven

To add the dependency using Maven just add the following section to your dependencies:
```xml
<dependency>
    <groupId>de.cubeisland.engine</groupId>
    <artifactId>configuration</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Gradle

To add the dependency using Gradle just add the following line to your dependencies:
```groovy
compile 'de.cubeisland.engine:configuration:1.0.1'
```

## Dependencies for Android

Using this library with Android is a little more difficult as the snakeyaml dependency 
doesn't work with Android. Nevertheless you can use it by following these steps:

1. download the snakeyaml-android.jar from this page: [snakeyaml-android](http://code.google.com/p/snakeyaml/downloads/detail?name=snakeyaml-android-1.8-SNAPSHOT.jar&can=2&q=)
2. copy the file to your project/module
3. add the following section to your dependencies:
    * Maven:
    
    ```xml
    <dependency>
        <groupId>de.cubeisland.engine</groupId>
        <artifactId>configuration</artifactId>
        <version>1.0.1</version>
        <exclusions>
            <exclusion>
                <groupId>org.yaml</groupId>
                <artifactId>snakeyaml</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <artifactId>org.yaml</artifactId>
        <groupId>snakeyaml</groupId>
        <version>1.8</version>
        <scope>system</scope>
        <systemPath>${basedir}/RELATIVE_PATH_TO_SNAKEYAML_ANDROID_JAR</systemPath>
    </dependency>
    ```
    * Gradle:
    
    ```groovy
    compile ('de.cubeisland.engine:configuration:1.0.1')
    {
        exclude group: 'org.yaml', module: 'snakeyaml'
    }
    compile files('RELATIVE_PATH_TO_SNAKEYAML_ANDROID_JAR')
    ```

(Please note: edit the "RELATIVE_PATH_TO_SNAKEYAML_ANDROID_JAR" part!)

## Usage
```java
public class ExamplePlugin extends JavaPlugin
{
    // Create the Factory
    private ConfigurationFactory factory = new ConfigurationFactory();

    private MyConfig config;

    public void onEnable()
    {
        File file = new File(this.getDataFolder(), "config.yml");

        // load the configuration using the factory (this will also save after loading)
        config = factory.load(MyConfig.class, file);

        // at any time you can reload the configuration
        config.reload(); // this will also save the configuration!
        // or save the configuration
        config.save();
    }

    // The Configuration Class
    public class MyConfig extends YamlConfiguration // this is the same as extends Configuration<YamlCodec>
    {
        public transient boolean noSaving; // fields that are transient are ignored

        // the path is generated from the field: lots-of-saving
        public boolean lotsOfSaving = true; // set default values (will be set if not loaded OR field is missing in file)

        @Name("default")
        public String defaultString = "You can define the path with an annotation instead. e.g. if you want to use \"default\"";

        // path will be: sub.section
        @Comment({"You can comment every field.","Even with multiple lines\nand this linebreak works too"})
        public SubSection sub_section; // You do not NEED to set the default here ; it is done automatically

        // You can use sections instead of setting the whole path for every field
        public class SubSection implements Section
        {
            // path will be: sub.section.an-int
            public int anInt = 42;
            // path will be: sub.section.a-long
            public long aLong = 666;
        }
    }
}
```
