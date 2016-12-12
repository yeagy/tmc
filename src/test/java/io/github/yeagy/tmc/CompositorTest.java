package io.github.yeagy.tmc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static org.junit.Assert.*;

public class CompositorTest {

    @Test
    public void json() throws Exception {
        String[] tags = {"dev"};
        Compositor compositor = new Compositor("test", Arrays.asList(tags));
        testCompositor(compositor);
    }

    @Test
    public void yaml() throws Exception {
        String[] tags = {"dev"};
        Compositor compositor = new Compositor("testy", Arrays.asList(tags));
        testCompositor(compositor);
    }

    private void testCompositor(Compositor compositor) throws java.io.IOException, TmcException {
        Config config = compositor.create(Config.class);
        assertNotNull(config);
        assertTrue("test".equals(config.getApp()));
        assertTrue("dev.lb".equals(config.getLb().getHost()));
        assertEquals(8080, config.getLb().getPort());
        assertTrue("5s".equals(config.getLb().getTimeout()));

        File target = new File("gen.json");
        target.deleteOnExit();
        compositor.write(target);
        ObjectMapper mapper = new ObjectMapper();
        Config outputConfig = mapper.readValue(target, Config.class);
        Config controlConfig = mapper.readValue(this.getClass().getClassLoader().getResource("test-dev.json"), Config.class);
        assertEquals(controlConfig, outputConfig);
    }

    @Test(expected = TmcException.class)
    public void selfCycle() throws Exception {
        new Compositor("cycle", null);
    }

    @Test(expected = TmcException.class)
    public void deepCycle() throws Exception {
        new Compositor("deepa", null);
    }

    @Test(expected = TmcException.class)
    public void missingModule() throws Exception {
        new Compositor("module404", null);
    }

    @Test(expected = TmcException.class)
    public void collision() throws Exception {
        new Compositor("collision", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void extraTag() throws Exception {
        String[] tags = {"dev"};
        new Compositor("logging", Arrays.asList(tags));
    }
}