package ru.yandex.whitespirit.it_tests.templates;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import lombok.SneakyThrows;
import lombok.val;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

public class TemplatesManager {
    private final Configuration cfg;

    public TemplatesManager() {
        this.cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setClassLoaderForTemplateLoading(TemplatesManager.class.getClassLoader(), "templates");
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setLocale(Locale.US);

        cfg.setWrapUncheckedExceptions(true);
    }

    public String processTemplate(Template template, Map<String, Object> substitutions) {
        return processTemplate(template.getTemplateName(), substitutions);
    }

    @SneakyThrows
    public String processTemplate(String templateName, Map<String, Object> substitutions) {
        val temp = cfg.getTemplate(templateName);
        return processTemplate(temp, substitutions);
    }

    @SneakyThrows
    private String processTemplate(freemarker.template.Template template, Map<String, Object> substitutions) {
        val writer = new StringWriter();
        template.process(substitutions, writer);
        return writer.toString();
    }

    @SneakyThrows
    public String processTemplate(String templateName, String templateBody, Map<String, Object> substitutions) {
        val temp = new freemarker.template.Template(templateName, new StringReader(templateBody), cfg);
        return processTemplate(temp, substitutions);
    }
}
