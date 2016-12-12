[![Build Status](https://travis-ci.org/yeagy/tmc.svg?branch=master)](https://travis-ci.org/yeagy/tmc)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.yeagy/tmc/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.yeagy/tmc)
[![Javadocs](http://javadoc-badge.appspot.com/io.github.yeagy/tmc.svg?label=javadocs)](http://javadoc-badge.appspot.com/io.github.yeagy/tmc)

#Tagged Modular Configuration
* Java configuration library. Will read and map your config file(s) to a POJO.
* Modular: Can reference other config files on the classpath to merge config at compile time.
* Tagged: Can define config blocks with tags to merge config at run time.
* Formats: JSON and YAML

Usage:
```
    List<String> tags = Arrays.asList(new String[]{"prod"});
    MyConfig config = new Compositor("main", tags).create(MyConfig.class);
    //your application code...
```

JSON example:
```json
//main.tmc.json
{
  "app": "example",
  "logging": {
    "level": "DEBUG",
    "enabled": true
  },
  "http": {
    "_module": "loadbalancer",
    "port": 8080
  },
  "_tags": {
    "prod": {
      "logging": {
        "level": "WARN"
      }
    }
  }
}
```
```json
//loadbalancer.tmc.json
{
  "host": "localhost",
  "_tags": {
    "prod": {
      "host": "lb.site.com"
    }
  }
}
```
* TMC modules have filename patterns \<module name\>.tmc.\<format\> (i.e. main.tmc.json)
* Modules be referenced using the "_module" field name. The module file will be loaded and merged into the node the "_module" was found on.
* Tags are defined on a module using the "_tags" field name. Tags specified at runtime will be merged onto the module at the root level, so full paths are needed in tag blocks.
* Module merging will not overwrite the leafs of the parent module. Tag merging will overwrite existing leafs of the parent module, whole object nodes are not replaced. Tag collisions will be won by the leftmost tag of the input list.
<br><br>
Running TMC on the "main" module with the "prod" tag would create the config below.
```json
{
  "app": "example",
  "logging": {
      "level": "WARN",
      "enabled": true
    },
    "http": {
      "host": "lb.site.com",
      "port": 8080
    }
}
```
TMC modules can reference other modules. TMC will resolve modules recursively, and error if a cycle is detected. Tags can also contain modules.
```xml
<dependency>
  <groupId>io.github.yeagy</groupId>
  <artifactId>tmc</artifactId>
  <version>0.1.0</version>
</dependency>
```