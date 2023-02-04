# -*- coding: utf-8 -*-
import logging
import random
from textwrap import dedent

import pytest
import requests

from local_settings import AUTH_TOKEN, NAMESPACE
from mdswrapper import client

logging.basicConfig()
logging.getLogger('').setLevel(logging.DEBUG)


def test_parse_mds_upload_response():
    xml = dedent(
        """\
        <?xml version="1.0" encoding="utf-8"?>
        <post obj="$namespacename.file1" id="81d8ba78474fa83" groups="3" size="4" key="221/file1">
        <complete addr="141.8.145.55:1032" path="/srv/storage/8/data-0.0" group="223" status="0"/>
        <complete addr="141.8.145.116:1032" path="/srv/storage/8/data-0.0" group="221" status="0"/>
        <complete addr="141.8.145.119:1029" path="/srv/storage/5/data-0.0" group="225" status="0"/>
        <written>3</written>
        </post>
        """
    ).strip()

    assert client.parse_key_from_mds_upload_response(xml) == '221/file1'


def test_parse_xml_post_key():
    xml = dedent(
        """\
    <?xml version="1.0" encoding="utf-8"?>
    <post>
    <key>4375/test_filename</key>
    </post>
        """
    ).strip()
    assert client.parse_xml_post_key(xml) == '4375/test_filename'



ENV = 'testing'

FILENAME = 'test_filename'


def test_write_read_delete():
    mds = client.MdsClient(NAMESPACE, AUTH_TOKEN, ENV)

    data = '='.join(
        ['mds_test_%s' % str(random.random())] *
        random.randrange(2, 4)
    )

    # Проверяем что прочиталось то, что записывали
    def rewrite():
        attempts = 12
        for _ in xrange(attempts):
            try:
                return mds.write(FILENAME, data)
            except MdsFilenameAlreadyExists as e:
                mds.delete(e.key)
        raise MdsError('File still exists after %s deletions' % attempts)

    key = rewrite()
    assert mds.read(key) == data

    mds.delete(key)


def test_timeout():
    mds = client.MdsClient(NAMESPACE, AUTH_TOKEN, ENV, timeout=0.001)

    with pytest.raises(requests.exceptions.ReadTimeout):
        mds.read('1234/whatever')
