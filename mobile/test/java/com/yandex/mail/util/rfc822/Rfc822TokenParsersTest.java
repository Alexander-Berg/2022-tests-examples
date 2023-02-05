package com.yandex.mail.util.rfc822;

import com.yandex.mail.runners.UnitTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(UnitTestRunner.class)
public class Rfc822TokenParsersTest {

    @Test
    public void composeParser_shouldReturnInstanceOfComposeRfc822TokenParser() throws Exception {
        assertThat(Rfc822TokenParsers.composeParser()).isExactlyInstanceOf(ComposeRfc822TokenParser.class);
    }

    @Test
    public void newConcurrentParserWithLruCache_shouldReturnInstanceOfRfc822TokenParserWithCache() throws Exception {
        assertThat(Rfc822TokenParsers.newConcurrentParserWithLruCache(8)).isExactlyInstanceOf(Rfc822TokenParserWithCache.class);
    }

    @Test
    public void newConcurrentParserWithCache_shouldReturnInstanceOfRfc822TokenParserWithCache() throws Exception {
        assertThat(Rfc822TokenParsers.newConcurrentParserWithCache(8)).isExactlyInstanceOf(Rfc822TokenParserWithCache.class);
    }
}