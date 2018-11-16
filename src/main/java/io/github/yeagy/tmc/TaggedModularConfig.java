package io.github.yeagy.tmc;

import ru.vyarus.java.generics.resolver.GenericsResolver;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

/**
 *
 */
public class TaggedModularConfig {
    public enum FileType{JSON, YAML}

    private final String rootModuleName;
    private List<String> appliedTags;

    private TaggedModularConfig(String rootModuleName) {
        this.rootModuleName = rootModuleName;
    }

    /**
     * Create a TMC builder for the root module name, ex. appFoo.tmc.yml would have the name "appFoo"
     *
     * @param rootModuleName file name without TMC extension
     * @return TMC builder
     */
    public static TaggedModularConfig rootModule(String rootModuleName) {
        return new TaggedModularConfig(rootModuleName);
    }

    /**
     * Apply tags to the mutate the config tree.
     *
     * @param appliedTags list of tag tree names
     * @return this TMC builder
     */
    public TaggedModularConfig applyTags(String... appliedTags) {
        this.appliedTags = Arrays.asList(appliedTags);
        return this;
    }

    /**
     * Apply tags to the mutate the config tree.
     *
     * @param appliedTags list of tag tree names
     * @return this TMC builder
     */
    public TaggedModularConfig applyTags(List<String> appliedTags) {
        this.appliedTags = appliedTags;
        return this;
    }

    /**
     * Map the composite config tree onto a POJO class.
     * <p>
     * Example:
     * AppConfig config = TaggedModularConfig.rootModule("main").applyTags("prod").create(AppConfig.class);
     *
     * @param configClass pojo type
     * @param <C>         pojo type
     * @return new mapped pojo
     * @throws TmcException error mapping
     */
    public <C> C create(Class<C> configClass) throws TmcException {
        return new JacksonCompositor(rootModuleName, appliedTags).create(configClass);
    }

    /**
     * Map the composite config tree onto a POJO class.
     * <p>
     * Use case for this method is that the POJO class you want to map to is one of the generic type parameters of another class.
     * This can extract the generic class type and map to it at runtime.
     * <p>
     * Example:
     * App<AppConfig, Foo, Bar, String> app = ...
     * AppConfig config = TaggedModularConfig.rootModule("main").create(app.getClass(), App.class, 0);
     *
     * @param classInstance         an instance of the class with a generic type of your config
     * @param classDefinition       the raw class of the instance class
     * @param genericParameterIndex the index (zero based) of the generic parameter type to be used
     * @param <C>                   the class of the generic index
     * @return new mapped pojo
     * @throws TmcException error mapping
     */
    @SuppressWarnings("unchecked")
    public <C> C create(Class<?> classInstance, Class<?> classDefinition, int genericParameterIndex) throws TmcException {
        final Class<C> genericType = (Class<C>) GenericsResolver.resolve(classInstance).type(classDefinition).generic(genericParameterIndex);
        return create(genericType);
    }


    /**
     * write composite config tree to file. json format.
     *
     * @param target file to write to
     * @param fileType type of file to write
     * @throws IOException error writing to file
     */
    public void write(File target, FileType fileType) throws IOException {
        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(target))) {
            write(outputStream, fileType);
        }

    }


    /**
     * write composite config tree to output stream. json format.
     *
     * @param outputStream output stream to write to
     * @param fileType type of file to write
     * @throws IOException error writing to stream
     */
    public void write(OutputStream outputStream, FileType fileType) throws IOException {
        JacksonCompositor comp = new JacksonCompositor(rootModuleName, appliedTags);
        if(fileType != null && fileType == FileType.YAML) {
            comp.writeYaml(outputStream);
        } else {
            comp.writeJson(outputStream);
        }
    }
}
