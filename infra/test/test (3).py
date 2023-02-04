import pytest
import subprocess
import base64
import requests
import logging
import time
import hashlib
import os


import yatest.common
from yatest.common.network import PortManager

import infra.skyboned.api


DB_PASS = base64.b64decode('''R2g4SmhaSzEwNQo=''')
SKYBONED_PORT = PortManager().get_port()
SERVERS = f'localhost:{SKYBONED_PORT}'

logger = logging.getLogger("test_logger")


pytest.rbtorrent = ""
pytest.source_id = []


def mock_hashes():
    md5_hash = hashlib.md5()
    sha1_hashes = []
    piece = os.urandom(4*1024*1024)
    md5_hash.update(piece)
    sha1_hashes.append(hashlib.sha1(piece).digest())
    size = len(piece)
    return (md5_hash.hexdigest(), sha1_hashes, size)


@pytest.fixture(scope='session', autouse=True)
def init():
    skyboned_bin = yatest.common.binary_path('infra/skyboned/go/cmd/skyboned-api')
    skyboned_api = subprocess.Popen([skyboned_bin,
                                        '--tvm-disable',
                                        '--no-wait',
                                        '--db-host', 'sas-sc0vohhumzm7e5mc.db.yandex.net,vla-6bbeftbz2f9vtt42.db.yandex.net,vla-7vu9h3ddg2n1wgbo.db.yandex.net',
                                        '--db-port', '6432',
                                        '--db-name', 'skyboned',
                                        '--db-user', 'skyboned',
                                        '--db-password', DB_PASS,
                                        '--http-port', str(SKYBONED_PORT),
                                        '--log-file', yatest.common.output_path()+'/skyboned.log'])
    time.sleep(2)
    yield skyboned_api


@pytest.fixture
def cleanup(autouse=True):
    pytest.rbtorrent = ""
    pytest.source_id = []
    yield
    rbtorrent = pytest.rbtorrent
    if len(pytest.source_id) > 0:
        for source_id in pytest.source_id:
            remove_resource(rbtorrent, source_id)
    else:
        remove_resource(rbtorrent)

    return


@pytest.fixture
def generate_resource():
    md5_hash, sha1_hashes, size = mock_hashes()
    items = {
        'stub': {
            'type': 'file',
            'md5': md5_hash,
            'executable': False,
            'size': size
        }
    }
    hashes = {
        md5_hash: b''.join(sha1_hashes)
    }
    links = {
        md5_hash: 'http://localhost/skyboned/link-one'
    }
    return items, hashes, links


def get_resource(rbtorrent):
    return requests.post(f'http://localhost:{SKYBONED_PORT}/get_resource', json={"uid": rbtorrent})


def get_resource_info(rbtorrent):
    return get_resource(rbtorrent).json()["info"]


def add_resource(items, hashes, links, source_id=None):
    return infra.skyboned.api.skyboned_add_resource(items, hashes, links,
                                                        servers=(SERVERS,),
                                                        source_id=source_id,
                                                        tvm_ticket="foo")[0]


def remove_resource(rbtorrent, source_id=None):
    return infra.skyboned.api.skyboned_remove_resource(rbtorrent,
                                                        servers=(SERVERS,),
                                                        source_id=source_id,
                                                        tvm_ticket="foo")


def test_ping(init):
    response = requests.get(f'http://localhost:{SKYBONED_PORT}/ping')
    assert response.status_code == 200


def test_add_v1(generate_resource):
    items, hashes, links = generate_resource
    rbtorrent = add_resource(items, hashes, links)
    pytest.rbtorrent = rbtorrent

    res_links = {}

    for md5hash in links:
        res_links[md5hash] = {
            "links": {
                links[md5hash]: {}
            }
        }

    assert get_resource_info(rbtorrent) == res_links

    remove_resource(rbtorrent)

    assert get_resource(rbtorrent).status_code == 404


def test_add_v1_source_id(generate_resource):
    items, hashes, links = generate_resource
    rbtorrent = add_resource(items, hashes, links, source_id='source_id')
    pytest.rbtorrent = rbtorrent
    pytest.source_id.append('source_id')

    res_links = {}

    for md5hash in links:
        res_links[md5hash] = {
            "links": {
                links[md5hash]: {'linkopts': {'0': 'source_id'}}
            }
        }
    assert get_resource_info(rbtorrent) == res_links

    remove_resource(rbtorrent, 'source_id')

    assert get_resource(rbtorrent).status_code == 404


def test_add_v2_source_id(generate_resource):
    items, hashes, links = generate_resource
    res_links = {}
    for md5hash in links:
        res_links[md5hash] = {
            'links': {
                links[md5hash]: {'linkopts': {'0': 'source_id'}}
            }
        }
        links[md5hash] = {links[md5hash]: {}}

    rbtorrent = add_resource(items, hashes, links, source_id='source_id')
    pytest.rbtorrent = rbtorrent
    pytest.source_id.append('source_id')

    assert get_resource_info(rbtorrent)== res_links

    remove_resource(rbtorrent, 'source_id')

    assert get_resource(rbtorrent).status_code == 404


def test_add_v2_no_source_id(generate_resource):
    items, hashes, links = generate_resource
    for md5hash in links:
        links[md5hash] = {links[md5hash]: {}}

    try:
        rbtorrent = add_resource(items, hashes, links)
    except requests.exceptions.HTTPError as e:
        assert e.response.status_code == 400
        return
    else:
        assert get_resource(rbtorrent).status_code == 404
        raise BaseException


def test_add_v1_over_v1(generate_resource):
    items, hashes, links = generate_resource
    rbtorrent = add_resource(items, hashes, links)
    pytest.rbtorrent = rbtorrent

    res_links = {}

    for md5hash in links:
        res_links[md5hash] = {'links': {links[md5hash]: {}}}
        links[md5hash] = 'http://localhost/skyboned/link-two'

    add_resource(items, hashes, links)

    assert get_resource_info(rbtorrent) == res_links


def test_add_v1_over_v1_with_source_id(generate_resource):
    items, hashes, links = generate_resource
    rbtorrent = add_resource(items, hashes, links)

    res_links = {}

    for md5hash in links:
        res_links[md5hash] = {'links': {links[md5hash]: {}, 'http://localhost/skyboned/link-two': {'linkopts': {'0': 'source_id'}}}}
        links[md5hash] = 'http://localhost/skyboned/link-two'

    add_resource(items, hashes, links, 'source_id')
    pytest.rbtorrent = rbtorrent
    pytest.source_id.append('source_id')

    assert get_resource_info(rbtorrent) == res_links


def test_add_v1_over_v2(generate_resource):
    items, hashes, links = generate_resource
    res_links = {}
    new_links = {}
    for md5hash in links:
        res_links[md5hash] = {
            'links': {
                links[md5hash]: {'linkopts': {'0': 'source_id'}}
            }
        }
        links[md5hash] = {links[md5hash]: {}}
        new_links[md5hash] = 'http://localhost/skyboned/link-two'

    rbtorrent = add_resource(items, hashes, links, source_id='source_id')
    pytest.rbtorrent = rbtorrent
    pytest.source_id.append('source_id')

    add_resource(items, hashes, new_links)

    assert get_resource_info(rbtorrent) == res_links


def test_add_v1_over_v2_with_source_id(generate_resource):
    items, hashes, links = generate_resource
    res_links = {}
    new_links = {}
    for md5hash in links:
        res_links[md5hash] = {
            'links': {
                links[md5hash]: {'linkopts': {'0': 'source_id'}},
                'http://localhost/skyboned/link-two': {'linkopts': {'0': 'source_id_2'}}
            }
        }
        links[md5hash] = {links[md5hash]: {}}
        new_links[md5hash] = 'http://localhost/skyboned/link-two'

    rbtorrent = add_resource(items, hashes, links, source_id='source_id')
    pytest.rbtorrent = rbtorrent
    pytest.source_id.append('source_id')

    add_resource(items, hashes, new_links, source_id='source_id_2')
    pytest.source_id.append('source_id_2')

    assert get_resource_info(rbtorrent) == res_links


def test_add_v2_over_v2(generate_resource):
    items, hashes, links = generate_resource
    res_links = {}
    new_links = {}
    for md5hash in links:
        res_links[md5hash] = {
            'links': {
                links[md5hash]: {'linkopts': {'0': 'source_id'}}
            }
        }
        links[md5hash] = {links[md5hash]: {}}
        new_links[md5hash] = {'http://localhost/skyboned/link-two': {}}

    rbtorrent = add_resource(items, hashes, links, source_id='source_id')
    pytest.rbtorrent = rbtorrent
    pytest.source_id.append('source_id')

    try:
        rbtorrent = add_resource(items, hashes, new_links)
    except requests.exceptions.HTTPError as e:
        assert e.response.status_code == 400
        pass

    assert get_resource_info(rbtorrent) == res_links


def test_add_v2_over_v2_with_source_id(generate_resource):
    items, hashes, links = generate_resource
    res_links = {}
    new_links = {}
    for md5hash in links:
        res_links[md5hash] = {
            'links': {
                links[md5hash]: {'linkopts': {'0': 'source_id'}},
                'http://localhost/skyboned/link-two': {'linkopts': {'0': 'source_id_2'}}
            }
        }
        links[md5hash] = {links[md5hash]: {}}
        new_links[md5hash] = {'http://localhost/skyboned/link-two': {}}

    rbtorrent = add_resource(items, hashes, links, source_id='source_id')
    pytest.rbtorrent = rbtorrent
    pytest.source_id.append('source_id')

    add_resource(items, hashes, new_links, source_id='source_id_2')
    pytest.source_id.append('source_id_2')

    assert get_resource_info(rbtorrent) == res_links


def test_add_v2_delete(generate_resource):
    items, hashes, links = generate_resource
    res_links = {}
    for md5hash in links:
        res_links[md5hash] = {
            'links': {
                links[md5hash]: {'linkopts': {'0': 'source_id'}}
            }
        }
        links[md5hash] = {links[md5hash]: {}}

    rbtorrent = add_resource(items, hashes, links, source_id='source_id')
    pytest.rbtorrent = rbtorrent
    pytest.source_id.append('source_id')

    remove_resource(rbtorrent)

    assert get_resource_info(rbtorrent) == res_links


def test_multiple_add_v2_straight_order(generate_resource):
    items, hashes, links = generate_resource
    res_links = {}
    inter_links = {}
    new_links = {}
    for md5hash in links:
        res_links[md5hash] = {
            'links': {
                links[md5hash]: {'linkopts': {'0': 'source_id'}},
                'http://localhost/skyboned/link-two': {'linkopts': {'0': 'source_id_2'}}
            }
        }
        inter_links[md5hash] = {'links': {'http://localhost/skyboned/link-two': {'linkopts': {'0': 'source_id_2'}}}}
        links[md5hash] = {links[md5hash]: {}}
        new_links[md5hash] = {'http://localhost/skyboned/link-two': {}}

    rbtorrent = add_resource(items, hashes, links, source_id='source_id')
    pytest.rbtorrent = rbtorrent

    add_resource(items, hashes, new_links, source_id='source_id_2')
    pytest.source_id.append('source_id_2')

    assert get_resource_info(rbtorrent) == res_links

    remove_resource(rbtorrent, 'source_id')

    assert get_resource_info(rbtorrent) == inter_links


def test_multiple_add_v2_with_reversed_order(generate_resource):
    items, hashes, links = generate_resource
    res_links = {}
    inter_links = {}
    new_links = {}
    for md5hash in links:
        res_links[md5hash] = {
            'links': {
                links[md5hash]: {'linkopts': {'0': 'source_id'}},
                'http://localhost/skyboned/link-two': {'linkopts': {'0': 'source_id_2'}}
            }
        }
        inter_links[md5hash] = {'links': {links[md5hash]: {'linkopts': {'0': 'source_id'}}}}
        links[md5hash] = {links[md5hash]: {}}
        new_links[md5hash] = {'http://localhost/skyboned/link-two': {}}

    rbtorrent = add_resource(items, hashes, links, source_id='source_id')
    pytest.rbtorrent = rbtorrent
    pytest.source_id.append('source_id')

    add_resource(items, hashes, new_links, source_id='source_id_2')

    assert get_resource_info(rbtorrent) == res_links

    remove_resource(rbtorrent, 'source_id_2')

    assert get_resource_info(rbtorrent) == inter_links


def test_multiple_add_v2_same_links(generate_resource):
    items, hashes, links = generate_resource
    res_links = {}
    for md5hash in links:
        res_links[md5hash] = {
            'links': {
                links[md5hash]: {'linkopts': {'0': 'source_id'}}
            }
        }
        links[md5hash] = {links[md5hash]: {}}

    rbtorrent = add_resource(items, hashes, links, source_id='source_id')
    pytest.rbtorrent = rbtorrent
    pytest.source_id.append('source_id')

    add_resource(items, hashes, links, source_id='source_id_2')
    pytest.source_id.append('source_id_2')

    assert get_resource_info(rbtorrent) == res_links
