package ru.auto.tests.publicapi.adaptor.offer;

import freemarker.template.Configuration;
import freemarker.template.Template;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomLicensePlate;
import static ru.auto.tests.publicapi.utils.UtilsPublicApi.getRandomVin;

/**
 * https://st.yandex-team.ru/VERTISTEST-1021
 */
public class OfferTemplate {

    public String process(String path, OfferTemplateData data) {
        try (Writer writer = new StringWriter()) {
            Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
            configuration.setTemplateUpdateDelayMilliseconds(0);
            configuration.setLocalizedLookup(false);
            configuration.setClassLoaderForTemplateLoading(getClass().getClassLoader(), "");
            final Template template = configuration.getTemplate(path);
            template.process(Collections.singletonMap("data", data), writer);
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Could't process offer template", e);
        }
    }

    public String process(String path, String phone) {
        return process(path, new OfferTemplateData().withPhone(phone).withLicensePlate(getRandomLicensePlate())
                .withVin(getRandomVin()));
    }
}
