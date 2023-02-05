package ru.yandex.market.util.phone;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import org.junit.Test;

import java.nio.CharBuffer;

import androidx.test.core.app.ApplicationProvider;
import ru.yandex.market.BaseTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class PhoneNumberFormattingTextWatcherTest extends BaseTest {

    private static final String USER_INPUT = "79876543210";

    private PhoneNumberFormatter phoneNumberFormatter;
    private EditText editText;
    private TextWatcher textWatcher;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        editText = new EditText(ApplicationProvider.getApplicationContext());
        phoneNumberFormatter = new PhoneNumberFormatter();
        textWatcher = new PhoneNumberFormattingTextWatcher(phoneNumberFormatter);
        editText.addTextChangedListener(textWatcher);
    }

    @Test
    public void testEveryInputDigitIsProperlyAddedToEditText() {
        final StringBuilder currentDigitsInput = new StringBuilder();
        for (int i = 0; i < USER_INPUT.length(); i++) {
            final char currentDigit = USER_INPUT.charAt(i);
            final Editable currentText = editText.getText();
            currentText.append(currentDigit);

            currentDigitsInput.append(currentDigit);
            final CharBuffer editTextText = CharBuffer.wrap(editText.getText());
            final CharBuffer formattedText = CharBuffer.wrap(
                    phoneNumberFormatter.format(currentDigitsInput));
            assertThat(editTextText, equalTo(formattedText));
        }
    }

    @Test
    public void testEveryBackslashFromEditTextConsideredAsDigitRemoval() {
        editText.removeTextChangedListener(textWatcher);
        editText.setText(phoneNumberFormatter.format(USER_INPUT));
        editText.addTextChangedListener(textWatcher);

        final StringBuilder currentInput = new StringBuilder(USER_INPUT);
        for (int i = currentInput.length() - 1; i >= 0; i--) {
            final Editable editTextText = editText.getText();
            final int editTextTextLength = editTextText.length();
            editTextText.replace(editTextTextLength - 1, editTextTextLength, "");

            currentInput.setLength(i);
            final String currentUserInput = currentInput.toString();
            final CharBuffer editTextCharBuffer = CharBuffer.wrap(editText.getText());
            final CharBuffer formattedCharBuffer = CharBuffer.wrap(
                    phoneNumberFormatter.format(currentUserInput));
            assertThat(editTextCharBuffer, equalTo(formattedCharBuffer));
        }
    }

    @Test
    public void testBackslashFromEveryPositionIsHandledProperly() {
        final CharSequence fullText = phoneNumberFormatter.format(USER_INPUT).toString();
        for (int i = 0; i < fullText.length(); i++) {
            editText.removeTextChangedListener(textWatcher);
            editText.setText(fullText.toString());
            editText.addTextChangedListener(textWatcher);

            editText.getText().replace(i, i + 1, "");
            final CharBuffer editTextText = CharBuffer.wrap(editText.getText());
            final CharBuffer referenceText = CharBuffer.wrap(
                    phoneNumberFormatter.handleBackspace(fullText, i + 1));
            assertThat(editTextText, equalTo(referenceText));
        }
    }
}