package io.github.yeagy.tmc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class TaggedModularConfigTest {

    @Test
    public void json() throws Exception {
        TaggedModularConfig tmc = TaggedModularConfig.rootModule("test").applyTags("dev");
        testMapping(tmc);
        testWriting(tmc);
    }

    @Test
    public void yaml() throws Exception {
        TaggedModularConfig tmc = TaggedModularConfig.rootModule("testy").applyTags("dev");
        testMapping(tmc);
        testWriting(tmc);
    }

    private void testMapping(TaggedModularConfig tmc) {
        Config config = tmc.create(Config.class);
        assertNotNull(config);
        assertTrue("test".equals(config.getApp()));
        assertTrue("dev.lb".equals(config.getLb().getHost()));
        assertEquals(8080, config.getLb().getPort());
        assertTrue("5s".equals(config.getLb().getTimeout()));
    }


    private void testWriting(TaggedModularConfig tmc) throws java.io.IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Config controlConfig = mapper.readValue(this.getClass().getClassLoader().getResource("test-dev.json"), Config.class);

        File json = new File("gen.json");
        json.deleteOnExit();
        tmc.write(json, TaggedModularConfig.FileType.JSON);
        Config jsonConfig = mapper.readValue(json, Config.class);
        assertEquals(controlConfig, jsonConfig);

        File yaml = new File("gen.yml");
        yaml.deleteOnExit();
        tmc.write(yaml, TaggedModularConfig.FileType.YAML);
        Config yamlConfig = mapper.readValue(yaml, Config.class);
        assertEquals(controlConfig, yamlConfig);
    }

    @Test(expected = TmcException.class)
    public void treeMismatch() {
        TaggedModularConfig.rootModule("test").create(getClass());
    }

    @Test(expected = NullPointerException.class)
    public void npe() {
        TaggedModularConfig.rootModule("test").create(null);
    }

    @Test(expected = TmcException.class)
    public void selfCycle() {
        TaggedModularConfig.rootModule("cycle").create(null);
    }

    @Test(expected = TmcException.class)
    public void deepCycle() {
        TaggedModularConfig.rootModule("deepa").create(null);
    }

    @Test(expected = TmcException.class)
    public void missingModule() {
        TaggedModularConfig.rootModule("module404").create(null);
    }

    @Test(expected = TmcException.class)
    public void collision() {
        TaggedModularConfig.rootModule("collision").create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void extraTag() {
        TaggedModularConfig.rootModule("logging").applyTags("dev").create(null);
    }
}