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
still a todo
