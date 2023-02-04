package ru.auto.tests.realtyapi.adaptor;

import freemarker.template.Configuration;
import freemarker.template.Template;
import ru.auto.tests.realtyapi.adaptor.offer.OfferTemplateData;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

/**
 * Freemarker template language processor
 */
public class FtlProcessor
{
    private final Configuration configuration;

    public FtlProcessor() {
        configuration = new Configuration(Configuration.VERSION_2_3_28);
        configuration.setTemplateUpdateDelayMilliseconds(0);
        configuration.setLocalizedLookup(false);
        configuration.setClassLoaderForTemplateLoading(FtlProcessor.class.getClassLoader(), "");
    }

    public String processOffer(String path) {
        return process(Collections.singletonMap("data", new OfferTemplateData()), path);
    }

    public String process(Object model, String path) {
        try (Writer writer = new StringWriter()) {
            final Template template = configuration.getTemplate(path);
            template.process(model, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could't process template", e);
        }
    }
}
