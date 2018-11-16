package io.github.yeagy.tmc;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JacksonCompositor {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());
    private static final ConfigFileLocator<ObjectNode> LOCATOR = new ConfigFileLocator<>(new ConfigFileLocator.Mapper<ObjectNode>() {
        @Override
        public ObjectNode map(InputStream inputStream) throws IOException {
            return (ObjectNode) MAPPER.readTree(inputStream);
        }
    });

    private final Map<String, ObjectNode> moduleCache = new HashMap<>();
    private final List<String> appliedTags;
    private final Set<String> foundTags = new HashSet<>();
    private final ObjectNode composite;

    JacksonCompositor(String baseModuleName, List<String> appliedTags) throws TmcException {
        this.appliedTags = appliedTags == null ? new ArrayList<String>() : appliedTags;
        composite = loadAndMergeModule(baseModuleName, new HashSet<String>());
        exceptOnMissingTags();
    }

    <C> C create(Class<C> configClass) throws TmcException {
        try {
            return MAPPER.treeToValue(composite, configClass);
        } catch (JsonProcessingException e) {
            throw new TmcException("error mapping composite", e);
        }
    }

    void writeJson(OutputStream outputStream) throws IOException {
        try (JsonGenerator generator = new JsonFactory().createGenerator(outputStream, JsonEncoding.UTF8)) {
            MAPPER.writeTree(generator, composite);
        }
    }

    void writeYaml(OutputStream outputStream) throws IOException {
        try (YAMLGenerator generator = new YAMLFactory().createGenerator(outputStream, JsonEncoding.UTF8)) {
            MAPPER.writeTree(generator, composite);
        }
    }

    //loads the config file of the given name. walks the tree looking for nested modules to load and merge into the tree.
    private ObjectNode loadAndMergeModule(String moduleName, Set<String> moduleChain) throws TmcException {
        if (moduleChain.contains(moduleName)) {
            throw new TmcException("configuration contains a module cycle " + moduleName);
        }
        moduleChain.add(moduleName);

        ObjectNode module = moduleCache.get(moduleName);
        if (module == null) {
            module = walkAndMerge(LOCATOR.loadModule(moduleName), moduleChain);
            mergeTags(module, moduleChain);
            moduleCache.put(moduleName, module);
        }

        moduleChain.remove(moduleName);
        return module;
    }

    //walk over the object fields, looking for nested object nodes to walk. while walking load and merge any modules found.
    private ObjectNode walkAndMerge(ObjectNode node, Set<String> moduleChain) throws TmcException {
        final Iterator<String> iter = node.fieldNames();
        while (iter.hasNext()) {
            final JsonNode field = node.get(iter.next());
            if (field.isObject()) {
                walkAndMerge((ObjectNode) field, moduleChain);
            }
        }
        final JsonNode module = node.remove("_module");
        if (module != null) {
            mergeTrees(node, loadAndMergeModule(module.asText(), moduleChain), false);
        }
        return node;
    }

    //merge any applied tags this module has into the module tree.
    private void mergeTags(ObjectNode module, Set<String> moduleChain) throws TmcException {
        final ObjectNode tags = (ObjectNode) module.remove("_tags");
        if (tags != null && !appliedTags.isEmpty()) {
            for (int i = appliedTags.size() - 1; i >= 0; i--) {//go backwards through tags to give left-to-right precedence
                final String appliedTag = appliedTags.get(i);
                final ObjectNode tagTree = (ObjectNode) tags.get(appliedTag);
                if (tagTree != null) {
                    foundTags.add(appliedTag);
                    mergeTrees(module, walkAndMerge(tagTree, moduleChain), true);
                }
            }
        }
    }

    //deep merge the accessory tree into the master tree. if replace is false, existing leafs in the master will not be changed.
    private void mergeTrees(ObjectNode master, ObjectNode accessory, boolean replace) {
        final Iterator<String> iter = accessory.fieldNames();
        while (iter.hasNext()) {
            final String accFieldName = iter.next();
            final JsonNode accField = accessory.get(accFieldName);
            if (master.has(accFieldName)) {
                if (accField.isObject()) {
                    mergeTrees((ObjectNode) master.get(accFieldName), (ObjectNode) accField, replace);
                } else if (replace) {
                    master.set(accFieldName, accField);
                }
            } else {
                master.set(accFieldName, accField);
            }
        }
    }

    private void exceptOnMissingTags() {
        final StringBuilder missing = new StringBuilder();
        for (String appliedTag : appliedTags) {
            if (!foundTags.contains(appliedTag)) {
                missing.append(appliedTag).append(", ");
            }
        }
        if (missing.length() > 0) {
            throw new IllegalArgumentException("cannot find bodies for tags: " + missing.toString().substring(0, missing.length() - 2));
        }
    }

}
