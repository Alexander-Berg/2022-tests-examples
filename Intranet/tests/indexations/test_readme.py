import pytest
from unittest import mock

from intranet.search.core.swarm.tasks import reindex
from intranet.search.tests.helpers.models_helpers import Service

pytestmark = pytest.mark.django_db

svn_data = {
    '/arc/trunk/arcadia/dir1/': None,
    '/arc/trunk/arcadia/dir1/file.txt': 'Some content',
    '/arc/trunk/arcadia/dir1/README.md': 'Some readme content',

    '/arc/trunk/arcadia/dir2/': None,
    '/arc/trunk/arcadia/dir2/script.py': 'Some python script',
    '/arc/trunk/arcadia/dir2/readme.md': 'Some readme cont',

    '/arc/trunk/arcadia/dir3/': None,
    '/arc/trunk/arcadia/dir3/script.py': 'Python script',
    '/arc/trunk/arcadia/dir3/file.txt': 'Some TXT notes',
    '/arc/trunk/arcadia/dir3/README.md/': None,
    '/arc/trunk/arcadia/dir3/README.md/Dockerfile': 'RUN bash',
    '/arc/trunk/arcadia/dir3/README.md/README.md': '<!--\n    Only comment\n-->',
}


def filter_recursive(rel_path=None, pattern=None, files_only=False):
    result = svn_data.keys()
    if rel_path:
        result = [val for val in result if val.startswith(rel_path)]
    if pattern:
        result = [val for val in result if val.rstrip('/').split('/')[-1].lower() == pattern.lower()]
    if files_only:
        result = [val for val in result if not val.endswith('/')]
    return result


def cat(rel_filepath, **kwargs):
    return svn_data[rel_filepath].encode('utf-8')


def test_readme_source_svn_client():
    mocked_client = mock.Mock()
    mocked_client.filter_recursive.side_effect = filter_recursive
    mocked_client.cat.side_effect = cat

    Service(name='doc')

    with mock.patch(
        'intranet.search.core.sources.doc.readme.IsearchSvnClient.__new__', mock.Mock(return_value=mocked_client)
    ):
        with mock.patch('intranet.search.core.swarm.indexer.Indexer.do_cleanup'):
            reindex('doc', index='readme', noqueue=True, nolock=True, dry_run=True)

    assert mocked_client.filter_recursive.call_count == 1
    assert mocked_client.filter_recursive.call_args == mock.call('/arc/trunk/arcadia', 'README.md', files_only=True)

    assert mocked_client.cat.call_count == 3
    assert mocked_client.cat.mock_calls == [
        mock.call('/arc/trunk/arcadia/dir1/README.md'),
        mock.call('/arc/trunk/arcadia/dir2/readme.md'),
        mock.call('/arc/trunk/arcadia/dir3/README.md/README.md'),
    ]
