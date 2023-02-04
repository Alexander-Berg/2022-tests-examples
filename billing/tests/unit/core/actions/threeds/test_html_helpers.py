import json
import re
from inspect import cleandoc

from hamcrest import assert_that, contains_string, equal_to

from billing.yandex_pay_plus.yandex_pay_plus.core.actions.threeds.html_helpers import (
    render_autopostform,
    render_hidden_iframe,
    render_postmessage_bubbler,
    render_postmessage_document,
    render_threeds_method_waiter,
)
from billing.yandex_pay_plus.yandex_pay_plus.tests.utils import beautiful_soup


class TestRenderHiddenIFrame:
    def test_returned(self):
        iframe_html = render_hidden_iframe('https://foo.test')

        iframe_parsed = beautiful_soup(iframe_html)
        assert_that(
            iframe_parsed.iframe['src'],
            equal_to('https://foo.test')
        )

    def test_malicious_input(self):
        iframe_html = render_hidden_iframe('https://foo"/><script>evil()</script>')
        assert_that(
            iframe_html,
            contains_string('src="https://foo&quot;/&gt;&lt;script&gt;evil()&lt;/script&gt;"')
        )


class TestRenderPostform:
    def test_returned(self):
        form_html = render_autopostform('https://foo.test', {'x': 'y', 'ABC': '1'}, 100)

        form_parsed = beautiful_soup(form_html)
        body = form_parsed.body
        form = body.form
        inputs = list(form.find_all('input'))
        form_vars = {item['name']: item['value'] for item in inputs}

        assert_that(body['onload'], equal_to("setTimeout(function() { document.forms['form'].submit(); }, 100)"))
        assert_that(form['action'], equal_to('https://foo.test'))
        assert_that(form_vars, equal_to({'x': 'y', 'ABC': '1'}))

    def test_malicious_input(self):
        form_html = render_autopostform('https://foo"test', {'x"': 'y"'}, 100)
        assert_that(form_html, contains_string('action="https://foo&quot;test"'))
        assert_that(form_html, contains_string('name="x&quot;"'))
        assert_that(form_html, contains_string('value="y&quot;"'))


class TestRenderPostMessageDocument:
    def test_returned(self):
        document_html = render_postmessage_document(
            {'xxx': 'yyy"', 'ABC': 1, 'bool': True},
            target_origin='https://foo.test',
        )

        document_parsed = beautiful_soup(document_html)
        head = document_parsed.head
        script = head.script
        script_text = script.text

        raw_post_message_data = re.search('var postMessageData = ([^;]+);', script_text).group(1)
        parsed_post_message_data = json.loads(raw_post_message_data)

        raw_target_origin = re.search('var targetOrigin = ([^;]+);', script_text).group(1)
        parsed_target_origin = json.loads(raw_target_origin)

        assert_that(parsed_post_message_data, equal_to({'xxx': 'yyy"', 'ABC': 1, 'bool': True}))
        assert_that(parsed_target_origin, equal_to('https://foo.test'))

    def test_malicious_input(self):
        document_html = render_postmessage_document(
            {'malicious': 'hello, world</script><script>alert("evil")</script><script>'},
            target_origin='https://<malicious>'
        )

        assert_that(
            document_html,
            contains_string('hello, world&lt;/script&gt;&lt;script&gt;alert(\\"evil\\")&lt;/script&gt;&lt;script&gt;')
        )
        assert_that(document_html, contains_string('https://&lt;malicious&gt;'))


class TestRenderPostMessageBubbler:
    def test_returned(self):
        bubbler_html = render_postmessage_bubbler()
        bubbler_parsed = beautiful_soup(bubbler_html)
        script = bubbler_parsed.script

        assert_that(
            cleandoc(script.string),
            equal_to(
                cleandoc(
                    """
                    window.addEventListener("message", function(ev) {
                        if (parent !== window && parent.postMessage) {
                            parent.postMessage(ev.data, "*");
                        }
                    });
                    """
                )
            )
        )


class TestRenderThreeDSMethodWaiter:
    def test_returned(self):
        waiter_html = render_threeds_method_waiter("https://method.test", 20000)
        waiter_parsed = beautiful_soup(waiter_html)
        script = waiter_parsed.script

        assert_that(
            cleandoc(script.string),
            equal_to(
                cleandoc(
                    """
                    var timeoutId = setTimeout(function () {
                        var xhr = new XMLHttpRequest();
                        xhr.open("POST", "https://method.test", true);
                        xhr.send(null);
                    }, 20000);

                    window.addEventListener("message", function(ev) {
                        clearTimeout(timeoutId);
                    });
                    """
                )
            )
        )
