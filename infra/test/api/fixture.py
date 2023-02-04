from genisys.web.model import _serialize


def get_fixture():
    yield 'section', {
        'name': 'genisys',
        'path': '',
        'revision': 1,
        '_id': 0,
        'subsections': _serialize({
            'skynet': {'_id': 1, 'subsections': {
                'versions': {'_id': 2, 'subsections': {}},
                'services': {'_id': 3, 'subsections': {
                    'service1': {'_id': 4, 'subsections': {}},
                    'service2': {'_id': 5, 'subsections': {}},
                }},
            }},
            'newsection': {'_id': 6, 'subsections': {
                'newsubsection': {'_id': 7, 'subsections': {}}
            }},
            'functest': {'_id': 8, 'subsections': {}},
        })
    }

    yield 'volatile', {
        "vtype": "section",
        "key": "hash('')",
        "atime": 1448868382.318278,
        "ctime": 1447324963.759573,
        "etime": 1448868682.318819,
        "mtime": 1447325114.681502,
        "tcount": 4909,
        "ttime": 1448868382.318819,
        "ucount": 4909,
        "utime": 1448868382.318819,
        "mcount": 1,
        "meta": {
            "changed_by": "user1",
            "mtime": 1447324963.759573,
            "owners": [
              "user1",
              "user2",
            ],
            "revision": 1
        },
        "value": _serialize({
          "configs": {},
          "hosts": {}
        }),
        "source": _serialize(
          {"stype": "yaml", "stype_options": None}
        ),
        "last_status": "same",
    }

    yield 'volatile', {
        "vtype": "section",
        "key": "hash('skynet')",
        "atime": 1448868382.327987,
        "ctime": 1447324964.412732,
        "etime": 1448868682.328492,
        "last_status": "same",
        "mcount": 1,
        "meta": {
            "changed_by": "user1",
            "mtime": 1447324964.412732,
            "owners": [],
            "revision": 9
        },
        "mtime": 1447325115.690523,
        "tcount": 4909,
        "ttime": 1448868382.328492,
        "ucount": 4909,
        "utime": 1448868382.328492,
        "value": _serialize({
          "configs": {},
          "hosts": {}
        }),
        "source": _serialize(
          {"stype": "yaml", "stype_options": None}
        ),
    }

    yield 'volatile', {
        "vtype": "section",
        "key": "hash('skynet.services')",
        "atime": 1448869700.327637,
        "ctime": 1447324964.485181,
        "etime": 1448870000.328099,
        "last_status": "same",
        "mcount": 1,
        "meta": {
          "changed_by": "user6",
          "mtime": 1447324964.485181,
          "owners": ["user6"],
          "revision": 20
        },
        "mtime": 1447325116.249975,
        "tcount": 4913,
        "ttime": 1448869700.328099,
        "ucount": 4913,
        "utime": 1448869700.328099,
        "value": _serialize({
          "configs": {},
          "hosts": {}
        }),
        "source": _serialize(
          {"stype": "yaml", "stype_options": None}
        ),
    }

    yield 'volatile', {
        "vtype": "section",
        "key": "hash('newsection')",
        "atime": 1448884427.1181922,
        "ctime": 1448884427.1181922,
        "etime": 1448884427.1181922,
        "last_status": "new",
        "mcount": 0,
        "meta": {
          "changed_by": "user2",
          "mtime": 1448884427.1181922,
          "owners": [],
          "revision": 149
        },
        "mtime": None,
        "tcount": 0,
        "ttime": None,
        "ucount": 0,
        "utime": None,
        "value": _serialize(None),
        "source": _serialize(
          {"stype": "yaml", "stype_options": None}
        ),
    }

    yield 'volatile', {
        "vtype": "section",
        "key": "hash('newsection.newsubsection')",
        "atime": 1448995121.4803107,
        "ctime": 1448995121.4803107,
        "etime": 1448995121.4803107,
        "last_status": "new",
        "mcount": 0,
        "meta": {
          "changed_by": "user1",
          "mtime": 1448995121.4803107,
          "owners": [],
          "revision": 150
        },
        "mtime": None,
        "tcount": 0,
        "ttime": None,
        "ucount": 0,
        "utime": None,
        "value": _serialize(None),
        "source": _serialize(
          {"stype": "yaml", "stype_options": None}
        ),
    }

    yield 'volatile', {
        "vtype": "section",
        "key": "hash('skynet.versions')",
        "atime": 1448831689.5682273,
        "ctime": 1447242679.507396,
        "etime": 1448831989.649433,
        "last_status": "modified",
        "mcount": 10,
        "meta": {
          "changed_by": "user1",
          "mtime": 1447242679.507396,
          "notified_on_broken_selectors": [],
          "owners": [],
          "revision": 123
        },
        "mtime": 1448831689.649433,
        "status": "new",
        "tcount": 63,
        "ttime": 1448831689.649433,
        "ucount": 59,
        "utime": 1448831689.649433,
        "value": _serialize({
          "configs": {
            -1: {
              "config": SKYVERSION1,
              "config_hash": "9e7ebd8de19beb956876346e132d2bfadb20693c",
              "matched_rules": ["Default Rule"]
            },
            1: {
              "config": SKYVERSION2,
              "config_hash": "d52168b597af10253e3b2336914f045ed97dc03c",
              "matched_rules": ["Torkve testing"]
            },
            2: {
              "config": SKYVERSION3,
              "config_hash": "6ef43dddc13428339ff119da384fc58c4e0d63e5",
              "matched_rules": ["Opl testing"]
            },
          },
          "hosts": {
            "h2": 1,
            "h3": 2,
            "h4": 2
          },
        }),
        "source": _serialize(
          {"stype": "sandbox_resource",
           "stype_options": {"resource_type": "SKYNET_BINARY"}}
        ),
    }

    yield 'volatile', {
        "vtype": "section",
        "key": "hash('skynet.services.service1')",
        "atime": 1448870436.914418,
        "ctime": 1447324964.823972,
        "etime": 1448870736.938978,
        "last_status": "same",
        "mcount": 461,
        "meta": {
          "changed_by": "user1",
          "mtime": 1447324964.823972,
          "notified_on_broken_selectors": [],
          "owners": [],
          "revision": 47
        },
        "mtime": 1448869828.625905,
        "tcount": 4926,
        "ttime": 1448870436.938978,
        "ucount": 4907,
        "utime": 1448870436.938978,
        "value": _serialize({
          "configs": {
            -1: {
              "config": {"confignum": -1, 'service': 1},
              "matched_rules": ["DEFAULT"],
              "config_hash": "35f9ef717248c41a630f40a27d51a5f6329a0fdb"
            },
            1025: {
              "config": {"confignum": 1025},
              "matched_rules": ["PREALLOCATE_ENABLE", "APRIMUS", "DEFAULT"],
              "config_hash": "faa7a45ba0c30d7fa4bdcf1b58c2d6b5ab04c3eb"
            },
            63: {
              "config": {"confignum": 63},
              "matched_rules": ["APRIMUS", "DEFAULT"],
              "config_hash": "ee73579693c066f353e5c69c94c5d08c7443eb2f"
            },
            77: {
              "config": {"confignum": 77},
              "matched_rules": ["IMGS_base", "ALL_SEARCH", "DEFAULT"],
              "config_hash": "b8899a7b7cdfe1d86eb2197b185a4ce272c15c4a"
            },
          },
          "hosts": {
            "h2": 1025,
            "h3": 63,
            "h4": 77
          }
        }),
        "source": _serialize(
          {"stype": "yaml", "stype_options": None}
        ),
    }

    yield 'volatile', {
        "vtype": "section",
        "key": "hash('skynet.services.service2')",
        "atime": 1448871420.14219,
        "ctime": 1447324965.181499,
        "etime": 1448871720.171249,
        "last_status": "same",
        "mcount": 363,
        "meta": {
          "changed_by": "user2",
          "mtime": 1447324965.181499,
          "notified_on_broken_selectors": [],
          "owners": [],
          "revision": 75
        },
        "mtime": 1448869093.437578,
        "tcount": 4952,
        "ttime": 1448871420.171249,
        "ucount": 4901,
        "utime": 1448871420.171249,
        "value": _serialize({
          "configs": {
            -1: {
              "config": {"confignum": -1, 'service': 2},
              "matched_rules": ["DEFAULT", "PACKAGE_DEFAULT"],
              "config_hash": "27ad5f2d0ae9391971bb4a7f885feee732d3bdbd"
            },
            130: {
              "config": {"confignum": 130},
              "matched_rules": ["service enabled out of SEARCH_ALL", "DEFAULT"],
              "config_hash": "d28da1f534c838e7af7b20a8bdbb74d13dfa536d"
            },
            465: {
              "config": {"confignum": 465},
              "matched_rules": ["CpuAffinity on SANDBOX", "DEFAULT"],
              "config_hash": "67f580833c75e276bca8ce0d165d7007ddb11e56"
            },
            458: {
              "config": {"confignum": 458},
              "matched_rules": ["Handshake timeout on ZM", "DEFAULT"],
              "config_hash": "dd9982e5dcb76f464ea0b837af875e51c61ad361"
            },
          },
          "hosts": {
            "h2": 130,
            "h3": 465,
            "h4": 458,
          }
        }),
        "source": _serialize(
          {"stype": "yaml", "stype_options": None}
        ),
    }

    yield 'volatile', {
        "vtype": "section",
        "key": "hash('functest')",
        "atime": 1448879869.078304,
        "ctime": 1448876628.418099,
        "etime": 1448880169.080212,
        "last_status": "same",
        "mcount": 1,
        "meta": {
          "changed_by": "robot-genisys",
          "mtime": 1448876628.418099,
          "owners": [],
          "revision": 219
        },
        "mtime": 1448876630.036458,
        "tcount": 10,
        "ttime": 1448879869.080212,
        "ucount": 10,
        "utime": 1448879869.080212,
        "value": _serialize({
          "configs": {
            -1: {
              "config": {"foo": "default"},
              "config_hash": "0c28268895fc67ef214b609e9356f28f3d1c7ab7",
              "matched_rules": ["DEFAULT"]
            },
            3: {
              "config": {"foo": "2015-11-30 13:43:48"},
              "config_hash": "68bbc01402969a1844d924575fca148b1e4b8263",
              "matched_rules": ["testrule", "DEFAULT"]
            }
          },
          "hosts": {"genisys.yandex-team.ru": 3}
        }),
        "source": _serialize(
          {"stype": "yaml", "stype_options": None}
        ),
    }


SKYVERSION1 = {
    "arch": "any",
    "attributes": {
      "backup_task": 43244456,
      "mds": "30724/90358745.tar.gz",
      "released": "stable",
      "svn_url": "svn+ssh://arcadia.yandex.ru/arc/branches/skynet/release-14.10@1967022",
      "ttl": "inf",
      "version": "14.10.17"
    },
    "description": "skynet.bin (14.10.17 (tc:2095))",
    "file_name": "dist/skynet.bin",
    "http": {
      "links": [
        "http://lucid.build.skydev.search.yandex.net:13578/1/3/43240331/dist/skynet.bin",
        "http://sandbox-storage2.search.yandex.net:13578/1/3/43240331/dist/skynet.bin",
        "http://sandbox-storage7.search.yandex.net:13578/1/3/43240331/dist/skynet.bin"
      ],
      "proxy": "http://proxy.sandbox.yandex-team.ru/90358745"
    },
    "id": 90358745,
    "md5": "3bcc31da4dffa8661073e6d7638cfb42",
    "owner": "SKYNET",
    "rights": "read",
    "rsync": {
      "links": [
        "rsync://lucid.build.skydev.search.yandex.net/sandbox-tasks/1/3/43240331/dist/skynet.bin",
        "rsync://sandbox-storage2.search.yandex.net/sandbox-tasks/1/3/43240331/dist/skynet.bin",
        "rsync://sandbox-storage7.search.yandex.net/sandbox-tasks/1/3/43240331/dist/skynet.bin"
      ]
    },
    "size": 143688704,
    "skynet_id": "rbtorrent:1dc4e7ed2db0aa0574c7f8a9f518f260970257fd",
    "sources": [
      "sandbox-storage2",
      "sandbox-storage7",
      "zeleniy_krocodil"
    ],
    "state": "READY",
    "task": {
      "id": 43240331,
      "status": "RELEASED",
      "url": "https://sandbox.yandex-team.ru/api/v1.0/task/43240331"
    },
    "time": {
      "accessed": "2015-11-26T19:29:57.362000Z",
      "created": "2015-11-02T13:52:33Z"
    },
    "type": "SKYNET_BINARY",
    "url": "https://sandbox.yandex-team.ru/api/v1.0/resource/90358745"
}

SKYVERSION2 = {
    "arch": "any",
    "attributes": {
      "backup_task": 43257645,
      "svn_url": "svn+ssh://arcadia.yandex.ru/arc/branches/skynet/release-14.11@1967321",
      "version": "14.11.0a6"
    },
    "description": "skynet.bin (14.11.0a6 (tc:2100))",
    "file_name": "dist/skynet.bin",
    "http": {
      "links": [
        "http://lucid.build.skydev.search.yandex.net:13578/2/7/43250072/dist/skynet.bin",
        "http://sandbox-storage1.search.yandex.net:13578/2/7/43250072/dist/skynet.bin",
        "http://sandbox-storage13.search.yandex.net:13578/2/7/43250072/dist/skynet.bin"
      ],
      "proxy": "http://proxy.sandbox.yandex-team.ru/90385971"
    },
    "id": 90385971,
    "md5": "32ef5aaa5253cb5c6b35642b5ac50104",
    "owner": "SKYNET",
    "rights": "read",
    "rsync": {
      "links": [
        "rsync://lucid.build.skydev.search.yandex.net/sandbox-tasks/2/7/43250072/dist/skynet.bin",
        "rsync://sandbox-storage1.search.yandex.net/sandbox-tasks/2/7/43250072/dist/skynet.bin",
        "rsync://sandbox-storage13.search.yandex.net/sandbox-tasks/2/7/43250072/dist/skynet.bin"
      ]
    },
    "size": 143729664,
    "skynet_id": "rbtorrent:2296f2e578e3e6f3d99aeef15fc264b33ed2a668",
    "sources": [
      "sandbox-storage1",
      "sandbox-storage13",
      "zeleniy_krocodil"
    ],
    "state": "DELETED",
    "task": {
      "id": 43250072,
      "status": "SUCCESS",
      "url": "https://sandbox.yandex-team.ru/api/v1.0/task/43250072"
    },
    "time": {
      "accessed": "2015-11-16T19:02:59.222000Z",
      "created": "2015-11-02T16:56:51Z"
    },
    "type": "SKYNET_BINARY",
    "url": "https://sandbox.yandex-team.ru/api/v1.0/resource/90385971"
}

SKYVERSION3 = {
    "arch": "any",
    "attributes": {
      "backup_task": 43011611,
      "svn_url": "svn+ssh://arcadia.yandex.ru/arc/branches/skynet/release-15.0@1964982",
      "version": "15.0.0a17"
    },
    "description": "skynet.bin (15.0.0a17 (tc:2076))",
    "file_name": "dist/skynet.bin",
    "http": {
      "links": [
        "http://lucid.build.skydev.search.yandex.net:13578/7/7/43000877/dist/skynet.bin",
        "http://sandbox-storage10.search.yandex.net:13578/7/7/43000877/dist/skynet.bin",
        "http://sandbox-storage8.search.yandex.net:13578/7/7/43000877/dist/skynet.bin"
      ],
      "proxy": "http://proxy.sandbox.yandex-team.ru/89735588"
    },
    "id": 89735588,
    "md5": "4eb6e13701522276f673262a7bfb3033",
    "owner": "SKYNET",
    "rights": "read",
    "rsync": {
      "links": [
        "rsync://lucid.build.skydev.search.yandex.net/sandbox-tasks/7/7/43000877/dist/skynet.bin",
        "rsync://sandbox-storage10.search.yandex.net/sandbox-tasks/7/7/43000877/dist/skynet.bin",
        "rsync://sandbox-storage8.search.yandex.net/sandbox-tasks/7/7/43000877/dist/skynet.bin"
      ]
    },
    "size": 143606784,
    "skynet_id": "rbtorrent:246bc295c611c30dbd3c069d43283ce4e4a7528b",
    "sources": [
      "sandbox-storage10",
      "sandbox-storage8",
      "zeleniy_krocodil"
    ],
    "state": "DELETED",
    "task": {
      "id": 43000877,
      "status": "SUCCESS",
      "url": "https://sandbox.yandex-team.ru/api/v1.0/task/43000877"
    },
    "time": {
      "accessed": "2015-11-12T20:41:24.318000Z",
      "created": "2015-10-29T17:17:39Z"
    },
    "type": "SKYNET_BINARY",
    "url": "https://sandbox.yandex-team.ru/api/v1.0/resource/89735588"
}
