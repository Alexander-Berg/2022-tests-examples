from unittest.mock import MagicMock

import pytest
from botocore.exceptions import ClientError

from bcl.toolbox.mds import Mds, BUCKET_DEFAULT


def test_mds(mock_mds, monkeypatch):
    mds = Mds()

    mds.ident_prefix = 'autotest/'
    ident = 'dummy'

    result = mds.bucket_create(name=BUCKET_DEFAULT)
    assert result is None

    # Эмулируем исключение.
    def raiseme(Bucket):
        raise ClientError(MagicMock(), 'dummy')

    monkeypatch.setattr(mds.client, 'create_bucket', raiseme)
    with pytest.raises(ClientError):
        mds.bucket_create(name=BUCKET_DEFAULT)

    result = mds.buckets_list()
    assert list(result.keys()) == ['bcl']

    mds_path = mds.file_put(data=b'mymy', ident=ident, meta={'one': 'two'})
    assert mds_path == ':bcl:dummy'

    result = mds.file_get_meta(ident=ident)
    assert result == {'One': 'two'}

    result = mds.file_get_contents(ident)
    assert result == b'mymy'

    result = mds.file_get_contents(mds_path)
    assert result == b'mymy'

    assert mds.client.log == [
        'create bcl',
        'list buckets',
        "upload bcl.autotest/dummy {'Metadata': {'one': 'two'}}",
        'head bcl.autotest/dummy',
        'download bcl.autotest/dummy',
        'download bcl.autotest/dummy',
    ]
