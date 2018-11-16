[![Build Status](https://travis-ci.org/yeagy/tmc.svg?branch=master)](https://travis-ci.org/yeagy/tmc)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.yeagy/tmc/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.github.yeagy/tmc)
[![Javadocs](http://javadoc-badge.appspot.com/io.github.yeagy/tmc.svg?label=javadocs)](http://javadoc-badge.appspot.com/io.github.yeagy/tmc)

# Tagged Modular Configuration
* Java configuration library. Read and map your config file(s) to a POJO.
* Modular: Reference other config files on the classpath to merge into your config.
* Tagged: Define config blocks with tags to merge config at runtime.
* Formats: JSON and YAML

Usage:
```
    MyConfig config = TaggedModularConfig.rootModule("main").applyTags("prod").create(MyConfig.class);
```

* TMC modules have filename patterns \<module name\>.tmc.\<format\> (ex: main.tmc.json, config.tmc.yml)
* Modules are referenced using the "_module" field name. The module file will be loaded and merged into the node the "_module" was found on.
* Tags are defined on a module root using the "_tags" field name. Tags specified at runtime will be merged onto the module at the root level, so full paths are needed in tag blocks.
* TMC modules can reference other modules. TMC will resolve modules recursively, and error if a cycle is detected. Tags can also contain modules.
* Collision precedence order: root tags > root tree > module tags > module tree. Module tags will be resolved before the module is merged into it's parent tree. Module merging *will not* overwrite existing leafs of the parent module. Tag merging *will* overwrite existing leafs of the parent module. Multi-tag collisions will be won by the firstmost tag of the applied tag list.
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
  "timeout": "5s",
  "_tags": {
    "prod": {
      "host": "lb.site.com"
    }
  }
}
```
Running TMC on the "main" module with the "prod" tag would create the config below.
```json
{
  "app": "example",          //root tree
  "logging": {               //root tree
    "level": "WARN",         //root tag
    "enabled": true          //root tree
  },
  "http": {                  //root tree
    "host": "lb.site.com",   //module tag
    "timeout": "5s",         //module tree
    "port": 8080             //root tree
  }
}
```
```xml
<dependency>
  <groupId>io.github.yeagy</groupId>
  <artifactId>tmc</artifactId>
  <version>0.2.0</version>
</dependency>
```