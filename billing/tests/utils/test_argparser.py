import copy
import json
import argparse
from typing import (
    Union,
)
from unittest import mock

import pytest

from . import project_utils as pu


def test_any_exists_file_type(tmp_path):
    file_path = tmp_path / 'test_any_exists_file_type' / 'anyfile.txt'
    file_path.parent.mkdir()
    file_path.touch()

    parser = argparse.ArgumentParser(exit_on_error=False)
    parser.add_argument('-f', type=pu.argparser.AnyExistsFileType)
    args = parser.parse_args(['-f', str(file_path)])
    assert args.f == file_path

    file_path.unlink()
    expected_message = f'argument -f: file is not exists: {file_path}'
    with pytest.raises(argparse.ArgumentError, match=expected_message):
        parser.parse_args(['-f', str(file_path)])


@pytest.mark.parametrize(
    'argument, expected',
    [
        ('-f', 'f'),
        ('--foo', 'foo'),
        (('-f', '--foo'), 'foo'),
        (('-f', '--foo', '--bar'), 'foo'),
        ('-f-', 'f_'),
        ('--foo-bar', 'foo_bar'),
    ],
    ids=(
        'just-f',
        'just-foo',
        'f-and-foo',
        'f-foo-bar',
        '-f-',
        'foo-bar',
    ),
)
def test_argument_name(argument: Union[str, tuple[str, ...]], expected: str):
    a = pu.argparser.Argument(argument=argument)
    assert a.name == expected


def test_argument_name_overriding():
    a = pu.argparser.Argument(argument='--foo', dest='bar')
    assert a.name == 'bar'


class TestArgumentParser:
    class AnyArgumentParser(pu.argparser.ArgumentParser):
        class Namespace(argparse.Namespace):
            string: str
            integer: int
            boolean: bool
            string_list: list[str]

        @property
        def namespace(self) -> argparse.Namespace:
            return self.Namespace()

        @classmethod
        def arguments(cls):
            return (
                pu.argparser.Argument('-s', default='any', choices=('any', 'value'), dest='string'),
                pu.argparser.Argument(('-i', '--integer'), type=int),
                pu.argparser.Argument('-b', dest='boolean', required=True, action=argparse.BooleanOptionalAction),
                pu.argparser.Argument('-v', action='append', dest='string_list'),
            )

        @property
        def processors(self):
            return (self._processor_string2upper,)

        @staticmethod
        def _processor_string2upper(args: Namespace) -> Namespace:
            args.string = args.string.upper()
            return args

        @property
        def secrets(self):
            return {'integer'}

    @property
    def parser(self):
        return self.AnyArgumentParser()

    def test_arguments(self):
        # Test action
        parsed = self.parser.parse('-b', '-v', '1', '-v', '2', '-i', '3')
        assert parsed.boolean
        assert parsed.string_list == ['1', '2']

        # Test default and processors
        assert parsed.string == 'ANY'

        # Test type
        assert parsed.integer == 3

        # Test choices
        self.parser.parse('-b', '-s', 'value')
        with mock.patch('sys.exit') as m:
            self.parser.parse('-s', 'foo')
        m.assert_called_with(2)

        # Test requirements
        with mock.patch('sys.exit') as m:
            self.parser.parse()
        m.assert_called_once_with(2)

    def test_secrets(self):
        with mock.patch('logging.info') as m:
            self.parser.parse('-b', '-i', '666')

        m.assert_called_once()
        assert m.call_args.args[0] == 'Parsed arguments: %s'
        assert json.loads(m.call_args.args[-1]) == {
            'string': 'ANY',
            'integer': '***',
            'boolean': True,
            'string_list': None,
        }

    def test_from_namespace(self):
        initial = self.AnyArgumentParser.Namespace(
            string='any',
            integer=6,
            boolean=True,
            string_list=['1', '2', '3'],
        )
        expected = copy.deepcopy(initial)
        expected.string = expected.string.upper()

        result = self.parser.from_namespace(initial)
        assert result == expected

        initial.string = 'fail'
        with mock.patch('sys.exit') as m:
            self.parser.from_namespace(initial)
        m.assert_called_with(2)
