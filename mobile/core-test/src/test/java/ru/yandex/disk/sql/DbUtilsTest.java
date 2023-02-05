package ru.yandex.disk.sql;

import org.junit.Test;
import ru.yandex.disk.test.TestCase2;

import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.disk.sql.DbUtils.escapeLikeExpressionParameter;
import static ru.yandex.disk.sql.DbUtils.like;

public class DbUtilsTest extends TestCase2 {

    @Test
    public void testLike() throws Exception {
        assertThat(like("?/%"), equalTo(" LIKE  ? || '/%' ESCAPE '\\'"));
    }

    @Test
    public void testLike2() throws Exception {
        assertThat(like("?/?/%"), equalTo(" LIKE  ? || '/' ? || '/%' ESCAPE '\\'"));
    }

    @Test
    public void testLike3() throws Exception {
        assertThat(like("%?%"), equalTo(" LIKE  '%' || ? || '%' ESCAPE '\\'"));
    }

    @Test
    public void testLikeExpressionParameterShouldNoEscapeRegularCharacters() throws Exception {
        assertThat(escapeLikeExpressionParameter("/A"), equalTo("/A"));
    }

    @Test
    public void testLikeExpressionParameterShouldEscapeEscaper() throws Exception {
        assertThat(escapeLikeExpressionParameter("/\\A"), equalTo("/\\\\A"));
    }

    @Test
    public void testLikeExpressionParameterShouldEscapePercent() throws Exception {
        assertThat(escapeLikeExpressionParameter("/%A"), equalTo("/\\%A"));
    }

    @Test
    public void testLikeExpressionParameterShouldEscapeUnderline() throws Exception {
        assertThat(escapeLikeExpressionParameter("/_A"), equalTo("/\\_A"));
    }

}