import json
import pathlib

import pytest
import jinja2.exceptions

from . import project_utils as pu


class TestTextFormatter:
    @classmethod
    def setup_class(cls):
        cls.text_formatter = pu.TextFormatter()

    @pytest.fixture
    def context(self, tmp_path: pathlib.Path):
        root = tmp_path / 'TestTextFormatter'
        root.mkdir()

        context_file_path = root / 'context.json'
        with open(context_file_path, 'w') as f:
            json.dump({'str': 'value', 'int': 123, 'float': 1.23}, f)

        return context_file_path

    @pytest.mark.parametrize(
        'template, expected',
        (
            ('Hello, world!', 'Hello, world!'),
            ('{{ ["Hello", "world!"]|join(", ") }}', 'Hello, world!'),
            ('Hello, {{ 2 + 2 }}!', 'Hello, 4!'),
            ('Hello, {% for i in [1, 2, 3, 4] %}{{ i }}{% endfor %}!', 'Hello, 1234!'),
        ),
        ids=('simple-text', 'template-with-filters', 'template-with-calculations', 'template-with-for'),
    )
    def test_simple_text_or_templates(self, template: str, expected: str):
        result = self.text_formatter.format(template)
        assert result == expected

    def test_sandbox(self):
        with pytest.raises(jinja2.exceptions.SecurityError):
            self.text_formatter.format('{{ "".__class__.__mro__[2].__subclasses__() }}')

        with pytest.raises(jinja2.exceptions.SecurityError):
            self.text_formatter.format('{{ {}.update({}) }}')

    def test_context(self, context: str):
        tu = pu.TextFormatter(issue='ISSUE-1')

        result = tu.format('Issue: {{ issue }}')
        expected = 'Issue: ISSUE-1'
        assert result == expected

        tu = pu.TextFormatter(context_file_path=context, issue='ISSUE-1')

        result = tu.format('Issue: {{ issue }} + {{ context.int }}')
        expected = 'Issue: ISSUE-1 + 123'
        assert result == expected

        result = tu.format('With format: {{ value }}', value=5)
        expected = 'With format: 5'
        assert result == expected
