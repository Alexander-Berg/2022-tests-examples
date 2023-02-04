# coding=utf-8

import os
import json
import tempfile

from instancectl.cms import CmsType
from instancectl.cms import detect_cms_agent_type


CURRENT_DIR = os.getcwd()


def teardown_function(_):
    os.chdir(CURRENT_DIR)


DUMP_JSON_EXAMPLE = {
    "slot": "17319@sas1-1956.search.yandex.net",
    "configurationId": "parallel_rlsfacts_iss_test#98e9c70cd691bd5fcd94ceaa2b4dd88e",
    "targetState": "PREPARED",
    "properties": {
        "BSCONFIG_INAME": "sas1-1956.search.yandex.net:17319",
        "BSCONFIG_IPORT": "17319",
        "BSCONFIG_IHOST": "sas1-1956.search.yandex.net",
        "BSCONFIG_SHARDDIR": "rlsfacts-000-1393251816",
        "BSCONFIG_SHARDNAME": "rlsfacts-000-1393251816",
        "SAS": "MISC_ISS_RLSFACTS",
        "a": "topology_stable-62-r10",
        "geo": "sas",
        "newstyle": "upload",
        "parallel": "rlsfacts_iss_test",
        "shard_name": "rlsfacts-000-1393251816",
        "tags": "newstyle_upload a_dc_sas geo_sas parallel_autofacts a_ctype_isstest",
    },
    "resources": {
        "apache.ywsearch.cfg": {
            "uuid": "apache.ywsearch.cfg-63ffb17ff1a2d225ee95a62136069afb",
            "verification": {
                "checksum": "MD5:63ffb17ff1a2d225ee95a62136069afb",
                "checkPeriod": "0d0h0m"
            },
            "urls": ["http://cmsearch.yandex.ru/res/miscsearch/rlsfacts/rls_bs.cfg"]
        },
        "httpsearch": {
            "uuid": "httpsearch-fb21e24a3c9c7b095174ee0a409c4533",
            "verification": {
                "checksum": "MD5:fb21e24a3c9c7b095174ee0a409c4533",
                "checkPeriod": "0d0h0m"
            },
            "urls": ["rbtorrent:bfb5fdfc95525d035a21c3c1db41c241a23c9b64"]
        },
        "instancectl": {
            "uuid": "instancectl-200182e22c6de1b19090beaa9e2a1739",
            "verification": {
                "checksum": "MD5:200182e22c6de1b19090beaa9e2a1739",
                "checkPeriod": "0d0h0m"
            },
            "urls": ["rbtorrent:85319df3ad95b93f9f663ae4ffe8db42e170d71e"]
        },
        "instancectl.conf": {
            "uuid": "instancectl.conf-85cc28a312e6d951257d51bafee91274",
            "verification": {
                "checksum": "MD5:85cc28a312e6d951257d51bafee91274",
                "checkPeriod": "0d0h0m"
            },
            "urls": ["http://cmsearch.yandex.ru/res/web/base.conf"]
        },
        "iss_hook_start": {
            "uuid": "iss_hook_start-87d93df9cb6f80262adeb60362417b33",
            "verification": {
                "checksum": "MD5:87d93df9cb6f80262adeb60362417b33",
                "checkPeriod": "0d0h0m"
            },
            "urls": ["rbtorrent:0b964cf44a06d0cfd28eea2665e02c341325e725"]
        },
        "iss_hook_status": {
            "uuid": "iss_hook_status-36c173f2e4003e9ce7dc8a1c020ab688",
            "verification": {
                "checksum": "MD5:36c173f2e4003e9ce7dc8a1c020ab688",
                "checkPeriod": "0d0h0m"
            },
            "urls": ["rbtorrent:62f81c4f4aa03b6c398b180a5733bf3056c34059"]
        },
        "loop-httpsearch": {
            "uuid": "loop-httpsearch-eaccc9e4a2cbb5ed0f9d0b74bb5bdf3b",
            "verification": {
                "checksum": "MD5:eaccc9e4a2cbb5ed0f9d0b74bb5bdf3b",
                "checkPeriod": "0d0h0m"
            },
            "urls": ["rbtorrent:1e93e95326bf3ed1df12ccfcd08a77da974f5b47"]
        },
        "loop.conf": {
            "uuid": "loop.conf-85cc28a312e6d951257d51bafee91274",
            "verification": {
                "checksum": "MD5:85cc28a312e6d951257d51bafee91274",
                "checkPeriod": "0d0h0m"
            },
            "urls": ["http://cmsearch.yandex.ru/res/web/base.conf"]
        },
        "rlsfacts-000-1393251816": {
            "uuid": "rlsfacts-000-1393251816",
            "verification": dict(checksum="EMPTY:", checkPeriod="0d0h0m"),
            "urls": ["rbtorrent:1fa96054c0344a498ac6deefd0f805c10029e3a2"]
        }
    }
}


def get_temp_iss_dump_file(content):
    temp_dir = tempfile.mkdtemp()
    temp_file = os.path.join(temp_dir, 'dump.json')
    with open(temp_file, 'w')as dump_file:
        dump_file.write(json.dumps(content))
    return temp_file


def test_cms_type_class():
    iss_cms = CmsType(CmsType.ISS)
    assert iss_cms.is_iss()

    bsconfig_cms = CmsType(CmsType.BSCONFIG)
    assert bsconfig_cms.is_bsconfig()

    iss_cms = CmsType('iss')
    assert iss_cms.is_iss()

    bsconfig_cms = CmsType('bsconfig')
    assert bsconfig_cms.is_bsconfig()


def test_detect_cms_agent_type_function():
    dump_file = get_temp_iss_dump_file({})
    assert detect_cms_agent_type().is_bsconfig()
    os.chdir(os.path.dirname(dump_file))
    assert detect_cms_agent_type().is_iss()
