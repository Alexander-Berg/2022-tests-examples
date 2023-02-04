import io
import os
import json
import pytz

from django.utils import timezone
from django.utils.dateparse import parse_datetime

from wiki.arc_compat import is_arc, is_arc_runtime_testing

try:
    import yatest  # noqa
    from library.python import resource  # noqa

except ImportError:
    pass


def get_asset_path(filename):
    return os.path.abspath(os.path.join(os.path.split(__file__)[0], '..', 'test_data', filename))


def get_cassette_library_dir():
    if is_arc():
        if is_arc_runtime_testing():
            return yatest.common.source_path('intranet/wiki/tests/wiki_tests/test_data/cassettes')
        else:
            return ''

    return get_asset_path('cassettes')


def read_test_asset(filename, prefix='intranet/wiki/tests/wiki_tests/test_data') -> bytes:
    if is_arc():
        return resource.resfs_read(os.path.join(prefix, filename))
    else:
        return open(get_asset_path(filename), 'rb').read()


def read_test_asset_as_stream(filename, prefix='intranet/wiki/tests/wiki_tests/test_data'):
    data = read_test_asset(filename, prefix)

    if not data:
        raise ValueError(f'Asset {prefix}/{filename} not found')

    return io.BytesIO(data)


def open_json_fixture(fname, dateaware=None):
    fixture_data = read_test_asset(f'fixtures/{fname}')
    fixture = json.loads(fixture_data)
    if dateaware:
        for f in fixture:
            for field in dateaware:
                val = f['fields'][field]
                if val:
                    dt = parse_datetime(val)
                    if not timezone.is_aware(dt):
                        dt = timezone.make_aware(dt, pytz.UTC)
                        f['fields'][field] = dt

    return fixture
