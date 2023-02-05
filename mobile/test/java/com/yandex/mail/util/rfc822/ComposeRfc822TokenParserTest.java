package com.yandex.mail.util.rfc822;

import android.text.util.Rfc822Token;

import org.junit.Test;

import java.util.List;

import androidx.annotation.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

public class ComposeRfc822TokenParserTest extends Rfc822TokenParserTest {

    @NonNull
    @Override
    protected Rfc822TokenParser createParser() {
        return new ComposeRfc822TokenParser();
    }

    @Test
    public void parse_shouldReturnAddressIfTextStartsWithRoundBracket() throws Exception {
        List<Rfc822Token> tokens = createParser().parse("(");
        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getAddress()).isEqualTo("(");
    }
}
