import yatest
from google.protobuf import text_format
import difflib


class Validator(object):
    def __init__(self, package=None, path=None):
        self._path = f'maps/doc/proto/yandex/maps/proto/{package}/examples' if package else path

    def validate_example(self, message, name):
        example_path = yatest.common.source_path(f'{self._path}/{name}')
        example = open(example_path, 'r').read()
        result = text_format.MessageToString(message, as_utf8=True)
        result_path = example_path + '.result'
        if example != result:
            open(result_path, 'w').write(result)

        assert example == result, '\n' + ''.join(difflib.unified_diff(
            example.splitlines(True),
            result.splitlines(True),
            example_path,
            result_path
        ))
