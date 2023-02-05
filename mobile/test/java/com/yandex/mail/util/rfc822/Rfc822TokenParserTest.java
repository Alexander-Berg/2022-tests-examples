package com.yandex.mail.util.rfc822;

import android.text.util.Rfc822Token;

import com.yandex.mail.runners.UnitTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import androidx.annotation.NonNull;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(UnitTestRunner.class)
public abstract class Rfc822TokenParserTest {

    @NonNull
    protected abstract Rfc822TokenParser createParser();

    @Test
    public void shouldReturnEmptyListForNullAddress() {
        List<Rfc822Token> tokens = createParser()
                .parse(null);

        assertThat(tokens).isEmpty();
    }

    @Test
    public void shouldReturnEmptyListForEmptyAddress() {
        List<Rfc822Token> tokens = createParser()
                .parse("");

        assertThat(tokens).isEmpty();
    }

    @Test
    public void shouldParseOneToken() {
        List<Rfc822Token> tokens = createParser()
                .parse("Alfred Neuman <Neuman@BBN-TENEXA>");

        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getAddress()).isEqualTo("Neuman@BBN-TENEXA");
        assertThat(tokens.get(0).getName()).isEqualTo("Alfred Neuman");
    }

    @Test
    public void shouldParseThreeTokens() {
        List<Rfc822Token> tokens = createParser()
                .parse("Jones@Host.Net," +
                                "Alfred Neuman <Neuman@BBN-TENEXA>," +
                                "Doe@Somewhere-Else;"
                );

        assertThat(tokens).hasSize(3);

        assertThat(tokens.get(0).getAddress()).isEqualTo("Jones@Host.Net");
        assertThat(tokens.get(0).getName()).isNull();

        assertThat(tokens.get(1).getAddress()).isEqualTo("Neuman@BBN-TENEXA");
        assertThat(tokens.get(1).getName()).isEqualTo("Alfred Neuman");

        assertThat(tokens.get(2).getAddress()).isEqualTo("Doe@Somewhere-Else");
        assertThat(tokens.get(2).getName()).isNull();
    }

    @Test
    public void shouldParseEmailWithRussianDomainButEnglishUsernameAndEnglishName() {
        List<Rfc822Token> tokens = createParser()
                .parse("Alfred Neuman <Alfred@яндекс.ру>");

        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getAddress()).isEqualTo("Alfred@яндекс.ру");
        assertThat(tokens.get(0).getName()).isEqualTo("Alfred Neuman");
    }

    @Test
    public void shouldParseEmailWithRussianDomainAndRussianUsernameAndEnglishName() {
        List<Rfc822Token> tokens = createParser()
                .parse("Alfred Neuman <Альфред@яндекс.ру>");

        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getAddress()).isEqualTo("Альфред@яндекс.ру");
        assertThat(tokens.get(0).getName()).isEqualTo("Alfred Neuman");
    }

    @Test
    public void shouldParseEmailWithRussianDomainAndRussianUsernameAndRussianName() {
        List<Rfc822Token> tokens = createParser()
                .parse("Альфред Нейман <Альфред@яндекс.ру>");

        assertThat(tokens).hasSize(1);
        assertThat(tokens.get(0).getAddress()).isEqualTo("Альфред@яндекс.ру");
        assertThat(tokens.get(0).getName()).isEqualTo("Альфред Нейман");
    }

    @Test
    public void parse_shouldReturnAddressAndNameAndComment() throws Exception {
        List<Rfc822Token> tokens = createParser().parse("Test name <test@yandex-team.ru> (comment)");
        assertThat(tokens).hasSize(1);
        Rfc822Token token = tokens.get(0);
        assertThat(token.getName()).isEqualTo("Test name");
        assertThat(token.getAddress()).isEqualTo("test@yandex-team.ru");
        assertThat(token.getComment()).isEqualTo("comment");
    }
}
