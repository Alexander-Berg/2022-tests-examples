package com.yandex.mail.yables;

import android.text.util.Rfc822Token;
import android.view.View;

import com.yandex.mail.runners.IntegrationTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(IntegrationTestRunner.class)
public class YableViewTest {

    @Test
    public void testEmpty() {
        YableView empty = YableView.create(IntegrationTestRunner.app(), null, "", false, false, 1);
        assertThat(empty).isNull();
    }

    @Test
    public void testInvalid() {
        YableView invalid = YableView.create(IntegrationTestRunner.app(), null, "invalid", false, false, 1);
        assertThat(invalid).isNotNull();
        assertThat(invalid.viewBinding.yableDeleteIcon.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(invalid.isValid()).isFalse();
    }

    @Test
    public void testValid() {
        YableView valid = YableView.create(IntegrationTestRunner.app(), null, "valid@valid.com", false, false, 1);
        assertThat(valid).isNotNull();
        assertThat(valid.viewBinding.yableDeleteIcon.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(valid.isValid()).isTrue();
    }

    /**
     * The {@link YableView#getRecipientText()} method is used for database serialization, so we
     * test backwards compatiblity here
     */
    @Test
    public void testValidRecipientText() {
        Rfc822Token token = new Rfc822Token("Alala Tralala", "email@ya.ru", null);
        YableView valid = YableView.create(IntegrationTestRunner.app(), null, token.toString(), false, false, 1);
        assertThat(valid.viewBinding.yableDeleteIcon.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(valid.getRecipientText()).isEqualTo(token.toString());
    }

    /**
     * The {@link YableView#getRecipientText()} method is used for database serialization, so we
     * test backwards compatiblity here
     */
    @Test
    public void testInvalidRecipientText() {
        String text = "invalid@@@yaa";
        YableView invalid = YableView.create(IntegrationTestRunner.app(), null, text, false, false, 1);
        assertThat(invalid.getRecipientText()).isEqualTo("<" + text + ">");
        assertThat(invalid.viewBinding.yableDeleteIcon.getVisibility()).isEqualTo(View.VISIBLE);
    }
}
