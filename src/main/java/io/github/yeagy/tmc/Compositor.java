package io.github.yeagy.tmc;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.FileMatchProcessor;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TMC compisitor.
 */
public final class Compositor {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    private static final String[] VALID_FILE_SUFFIXES = {".tmc.json", ".tmc.yml", ".tmc.yaml"};

    private final List<String> appliedTags;

    private final Map<String, ObjectNode> moduleCache = new HashMap<>();
    private final Set<String> foundTags = new HashSet<>();
    private final ObjectNode composite;

    /**
     * Create a new compositor.
     * @param baseModuleName name of the base module
     * @param appliedTags tags to apply. left-to-right priority.
     * @throws TmcException error compositing
     */
    public Compositor(String baseModuleName, List<String> appliedTags) throws TmcException {
        this.appliedTags = appliedTags == null ? new ArrayList<String>() : appliedTags;
        composite = compositeModule(baseModuleName, new HashSet<String>());
        exceptOnMissingTags();
    }

    /**
     * map composite config to pojo
     * @param configClass pojo type
     * @param <C> pojo type
     * @return new mapped pojo
     * @throws TmcException error mapping
     */
    public <C> C create(Class<C> configClass) throws TmcException {
        try {
            return MAPPER.treeToValue(composite, configClass);
        } catch (JsonProcessingException e) {
            throw new TmcException("error mapping composite", e);
        }
    }

    /**
     * write composite config to file. json format.
     * @param target file to write to
     * @throws IOException error writing to file
     */
    public void write(File target) throws IOException {
        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(target));
             JsonGenerator generator = new JsonFactory().createGenerator(outputStream, JsonEncoding.UTF8)) {
            MAPPER.writeTree(generator, composite);
        }
    }

    private ObjectNode compositeModule(String moduleName, Set<String> moduleChain) throws TmcException {
        if (moduleChain.contains(moduleName)) {
            throw new TmcException("configuration contains a module cycle " + moduleName);
        }
        moduleChain.add(moduleName);

        ObjectNode module = moduleCache.get(moduleName);
        if (module == null) {
            module = walk(resourceTree(moduleName), moduleChain);
            ObjectNode tags = (ObjectNode) module.remove("_tags");
            if (tags != null && !appliedTags.isEmpty()) {
                for (int i = appliedTags.size() - 1; i >= 0; i--) {//go backwards through tags to give left-to-right overwrite behavior
                    String appliedTag = appliedTags.get(i);
                    ObjectNode tag = (ObjectNode) tags.get(appliedTag);
                    if (tag != null) {
                        foundTags.add(appliedTag);
                        ObjectNode configuredTag = walk(tag, moduleChain);
                        mergeTrees(module, configuredTag, true);
                    }
                }
            }
            moduleCache.put(moduleName, module);
        }

        moduleChain.remove(moduleName);
        return module;
    }

    private ObjectNode walk(ObjectNode node, Set<String> moduleChain) throws TmcException {
        Iterator<String> iter = node.fieldNames();
        while (iter.hasNext()) {
            JsonNode fieldValue = node.get(iter.next());
            if (fieldValue.isObject()) {
                walk((ObjectNode) fieldValue, moduleChain);
            }
        }
        JsonNode module = node.remove("_module");
        if (module != null) {
            mergeTrees(node, compositeModule(module.asText(), moduleChain), false);
        }
        return node;
    }

    private void exceptOnMissingTags() {
        StringBuilder missing = new StringBuilder();
        for (String appliedTag : appliedTags) {
            if (!foundTags.contains(appliedTag)) {
                missing.append(appliedTag).append(", ");
            }
        }
        if (missing.length() > 0) {
            throw new IllegalArgumentException("cannot find bodies for tags: " + missing.toString().substring(0, missing.length() - 2));
        }
    }

    private void mergeTrees(ObjectNode master, ObjectNode accessory, boolean replace) {
        Iterator<String> iter = accessory.fieldNames();
        while (iter.hasNext()) {
            String name = iter.next();
            if (master.has(name)) {
                JsonNode node = accessory.get(name);
                if (node.isObject()) {
                    mergeTrees((ObjectNode) master.get(name), (ObjectNode) node, replace);
                } else if (replace) {
                    master.set(name, node);
                }
            } else {
                master.set(name, accessory.get(name));
            }
        }
    }

    private ObjectNode resourceTree(final String moduleName) throws TmcException {
        final List<ObjectNode> modules = new ArrayList<>();
        FileMatchProcessor fileMatchProcessor = new FileMatchProcessor() {
            @Override
            public void processMatch(String relativePath, InputStream inputStream, long lengthBytes) throws IOException {
                modules.add((ObjectNode) MAPPER.readTree(inputStream));
            }
        };

        FastClasspathScanner scanner = new FastClasspathScanner();
        for (String suffix : VALID_FILE_SUFFIXES) {
            scanner.matchFilenamePathLeaf(moduleName + suffix, fileMatchProcessor);
        }
        scanner.scan();

        if (modules.isEmpty()) {
            throw new TmcException("cannot find tmc module: " + moduleName);
        }
        if (modules.size() > 1) {
            throw new TmcException("classpath contains more than one module " + moduleName);
        }
        return modules.get(0);
    }
}
