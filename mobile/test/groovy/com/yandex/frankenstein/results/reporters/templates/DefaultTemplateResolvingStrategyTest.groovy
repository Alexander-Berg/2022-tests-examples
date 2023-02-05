package com.yandex.frankenstein.results.reporters.templates

import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat

class DefaultTemplateResolvingStrategyTest {

    @Test
    void testGetByTypeAndName() {
        final TemplateResolvingStrategy templateResolvingStrategy = new DefaultTemplateResolvingStrategy()
        final InputStream inputStream = templateResolvingStrategy.getTemplateInputStream("type_1", "name_1")
        assertThat(inputStream.getText()).isEqualTo("template by type_1 and name_1\n")
    }

    @Test
    void testGetByType() {
        final TemplateResolvingStrategy templateResolvingStrategy = new DefaultTemplateResolvingStrategy()
        final InputStream inputStream = templateResolvingStrategy.getTemplateInputStream("type_2", "name_2")
        assertThat(inputStream.getText()).isEqualTo("template by type_2\n")
    }

    @Test
    void testGetDefaultByTypeAndName() {
        final TemplateResolvingStrategy templateResolvingStrategy = new DefaultTemplateResolvingStrategy()
        final InputStream inputStream = templateResolvingStrategy.getTemplateInputStream("type_3", "name_3")
        assertThat(inputStream.getText()).isEqualTo("default template by type_3 and name_3\n")
    }

    @Test
    void testGetDefaultByType() {
        final TemplateResolvingStrategy templateResolvingStrategy = new DefaultTemplateResolvingStrategy()
        final InputStream inputStream = templateResolvingStrategy.getTemplateInputStream("type_4", "name_4")
        assertThat(inputStream.getText()).isEqualTo("default template by type_4\n")
    }
}
