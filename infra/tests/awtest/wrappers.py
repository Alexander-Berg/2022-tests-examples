import os
import six
from google.protobuf import text_format

from awacs import yamlparser
from awacs.wrappers.base import wrap
from awacs.wrappers.errors import ValidationError
from awacs.model.util import clone_pb
from awtest.core import raises
from infra.awacs.proto import modules_pb2


IS_ARCADIA = 'ARCADIA_SOURCE_ROOT' in os.environ


def get_exception_msg(fn, *args, **kwargs):
    try:
        fn(*args, **kwargs)
    except ValidationError as e:
        return six.text_type(e)
    except Exception:
        raise
    else:
        return None


def get_wrapped_validation_exception_msg(pb, *args, **kwargs):
    return get_exception_msg(wrap(pb).validate, *args, **kwargs)


def get_validation_exception(func, *args, **kwargs):
    with raises(ValidationError) as e:
        func(*args, **kwargs)
    return e


def get_default_l7_macro_pb():
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.ports.append(80)
    pb.https.ports.append(443)
    pb.https.certs.add(id='test-cert')
    return holder_pb


def parse_lua_into_pb(lua_text, target_pb):
    text_format.Parse(lua_text, target_pb)


class FixtureStorage(object):
    def __init__(self, test_dir, test_subdir=None):
        super(FixtureStorage, self).__init__()
        if IS_ARCADIA:
            from yatest import common
            tests_path = common.source_path('infra/awacs/vendor/awacs/tests')
        else:
            tests_path = 'tests'
        self.fixture_path = os.path.join(tests_path, 'test_wrappers', test_dir, 'fixtures')
        if test_subdir is not None:
            self.fixture_path = os.path.join(self.fixture_path, test_subdir)

    def _get_fixture_path(self, file_name):
        return os.path.join(self.fixture_path, file_name)

    def write_fixture_file(self, macro, file_name):
        wrap(macro).expand_immediate_contained_macro()
        pb_txt = text_format.MessageToString(macro)
        with open(self._get_fixture_path(file_name), 'w') as outfile:
            outfile.write(pb_txt)

    def assert_pb_is_equal_to_file(self, macro_pb, file_name):
        wrap(macro_pb).expand_immediate_contained_macro()
        pb_txt = text_format.MessageToString(macro_pb)
        with open(self._get_fixture_path(file_name)) as infile:
            # parse and dump fixture to avoid float formatting differences
            expected_txt = infile.read().strip()
            expected_macro_pb = type(macro_pb)()
            text_format.Parse(expected_txt, expected_macro_pb)
            assert pb_txt == text_format.MessageToString(expected_macro_pb)


def parse(yaml):
    def _parse(x, cls=modules_pb2.Holder):
        return yamlparser.parse(cls, x)

    pb = _parse(yaml)
    parsed_unparsed_pb = _parse(yamlparser.dump(pb))
    assert pb == parsed_unparsed_pb
    return pb
