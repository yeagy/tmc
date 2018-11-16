package io.github.yeagy.tmc;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.FileMatchProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class ConfigFileLocator<T> {
    private static final String[] VALID_FILE_SUFFIXES = {".tmc.json", ".tmc.yml", ".tmc.yaml"};

    //could be a Function in Java8+
    interface Mapper<T> {
        T map(InputStream inputStream) throws IOException;
    }

    ConfigFileLocator(Mapper<T> mapper) {
        this.mapper = mapper;
    }

    private final Mapper<T> mapper;

    T loadModule(final String moduleName) throws TmcException {
        final List<T> modules = new ArrayList<>();
        final FileMatchProcessor fileMatchProcessor = new FileMatchProcessor() {
            @Override
            public void processMatch(String relativePath, InputStream inputStream, long lengthBytes) throws IOException {
                modules.add(mapper.map(inputStream));
            }
        };

        final FastClasspathScanner scanner = new FastClasspathScanner();
        for (String suffix : VALID_FILE_SUFFIXES) {
            scanner.matchFilenamePathLeaf(moduleName + suffix, fileMatchProcessor);
        }
        scanner.scan();

        if (modules.isEmpty()) {
            throw new TmcException("cannot find tmc module: " + moduleName);
        }
        if (modules.size() > 1) {
            throw new TmcException("classpath contains more than one module: " + moduleName);
        }
        return modules.get(0);
    }
}
