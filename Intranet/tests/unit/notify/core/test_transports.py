from django.test import TestCase
from mock import Mock
from pretend import stub

from plan.notify.core import transports

DUMMY_CONTEXT = {}


class EmailTransportLanguageTest(TestCase):
    def setUp(self):
        self.notification = Mock()
        self.transport = transports.EmailTransport()
        self.transport.parse_rendered = lambda *args, **kwargs: None  # для этого теста мешает

    def test_choose_correct_language_only_recipient(self):
        Person = stub

        self.transport.compose(
            notification=self.notification,
            context=DUMMY_CONTEXT,
            recipients=[Person(lang_ui='kz')]
        )

        self.notification.render.assert_called_with(
            context=DUMMY_CONTEXT,
            format='html',
            lang=['kz', 'ru'],
        )

    def test_choose_correct_language_lots_of_recipients(self):
        Person = stub

        self.transport.compose(
            notification=self.notification,
            context=DUMMY_CONTEXT,
            recipients=[Person(lang_ui='kz'), Person(lang_ui='en')]
        )

        self.notification.render.assert_called_with(
            context=DUMMY_CONTEXT,
            format='html',
            lang=['ru'],
        )


class EmailTransportParsingTest(TestCase):
    def setUp(self):
        self.transport = transports.EmailTransport()

    def test_parse_normal_text(self):
        rendered = """
        Hello
        <-- SECTION -->
        How are you?
        """

        parsed = self.transport.parse_rendered(rendered)

        self.assertEqual(parsed, {
            'header': 'Hello',
            'body': 'How are you?'
        })

    def test_parse_text_with_whitespaces(self):
        rendered = """
        \nHello\n\t\r
        <-- SECTION -->
        \r\nHow are you?\n\n\t
        """

        parsed = self.transport.parse_rendered(rendered)

        self.assertEqual(parsed, {
            'header': 'Hello',
            'body': 'How are you?'
        })
