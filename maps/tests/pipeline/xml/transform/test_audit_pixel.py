import pytest

from maps_adv.export.lib.pipeline.xml.transform.audit_pixel import transform_audit_pixel


@pytest.mark.parametrize(
    ["verification_data", "expected", "ctype"],
    [
        ({}, {}, None),
        (  # Empty platform
            [{"platform": "", "params": {}}],
            {},
            None,
        ),
        (  # Invalid platform
            [{"platform": "unknown", "params": {"account": 1, "tte": 2, "app": 3}}],
            {},
            None,
        ),
        (  # Valid weborama config
            [{"platform": "weborama", "params": {"account": 1, "tte": 2, "app": 3}}],
            {
                "audit-pixel-click-templated": [
                    "https://verify.yandex.ru/verify?platformid=4&a.si=1&a.te=2&a.aap=3&maid=[rdid]&a.A=cl"
                ],
                "audit-pixel-load-templated": [
                    "https://verify.yandex.ru/verify?platformid=4&a.si=1&a.te=2&a.aap=3&maid=[rdid]&a.A=ev&a.evn=load"
                ],
                "audit-pixel-im-templated": [
                    "https://verify.yandex.ru/verify?platformid=4&a.si=1&a.te=2&a.aap=3&maid=[rdid]&a.A=im"
                ],
                "audit-pixel-mrc50-templated": [
                    "https://verify.yandex.ru/verify?platformid=4&a.si=1&a.te=2&a.aap=3&maid=[rdid]&a.A=ev&a.evn=MRCViewable"
                ],
                "audit-pixel-mrc100-templated": [
                    "https://verify.yandex.ru/verify?platformid=4&a.si=1&a.te=2&a.aap=3&maid=[rdid]&a.A=ev&a.evn=MRC100Viewable"
                ],
            },
            None,
        ),
        (  # Invalid weborama config
            [{"platform": "weborama", "params": {"tte": 2, "app": 3}}],
            {},
            None,
        ),
        (  # Valid DCM config
            [
                {
                    "platform": "dcm",
                    "params": {
                        "url": "https://ad.doubleclick.net/path?arg=[timestamp]"
                    },
                },
                {
                    "platform": "dcm",
                    "params": {"url": "https://ad.doubleclick.net/path"},
                },
                {
                    "platform": "dcm",
                    "params": {
                        "url": "https://ad.doubleclick.net/path;dc_rdid=;other_param?param=value"
                    },
                },
            ],
            {
                "audit-pixel-im-templated": [
                    "https://ad.doubleclick.net/path;dc_rdid=[uadid];dc_lat=[lat]?arg=[timestamp]",
                ],
                "audit-pixel-im": [
                    "https://ad.doubleclick.net/path;dc_rdid=[uadid];dc_lat=[lat]",
                    "https://ad.doubleclick.net/path;dc_rdid=[uadid];other_param=;dc_lat=[lat]?param=value",
                ],
            },
            None,
        ),
        (  # Invalid DCM URL
            [
                {
                    "platform": "dcm",
                    "params": {
                        "url": "https://invalid-ad.doubleclick.net/path?arg=[timestamp]"
                    },
                }
            ],
            {},
            None,
        ),
        (  # Two DCM configs
            [
                {
                    "platform": "dcm",
                    "params": {
                        "url": "https://ad.doubleclick.net/path1?arg=[timestamp]"
                    },
                },
                {
                    "platform": "dcm",
                    "params": {
                        "url": "https://ad.doubleclick.net/path2?arg=[timestamp]"
                    },
                },
            ],
            {
                "audit-pixel-im-templated": [
                    "https://ad.doubleclick.net/path1;dc_rdid=[uadid];dc_lat=[lat]?arg=[timestamp]",
                    "https://ad.doubleclick.net/path2;dc_rdid=[uadid];dc_lat=[lat]?arg=[timestamp]",
                ],
            },
            None,
        ),
        (  # Valid sizmek config
            [
                {
                    "platform": "sizmek",
                    "params": {
                        "url": "https://bs.serving-sys.com/path?arg=[timestamp]"
                    },
                }
            ],
            {
                "audit-pixel-im-templated": [
                    "https://bs.serving-sys.com/path?arg=[timestamp]"
                ],
            },
            None,
        ),
        (  # Invalid sizmek config
            [
                {
                    "platform": "sizmek",
                    "params": {"url": "https://example.com/path?arg=[timestamp]"},
                }
            ],
            {},
            None,
        ),
        (  # Multiple configs
            [
                {  # Invalid platform
                    "platform": "unknow",
                    "params": {},
                },
                {  # Valid DCM
                    "platform": "dcm",
                    "params": {
                        "url": "https://ad.doubleclick.net/path1?arg=[timestamp]"
                    },
                },
                {  # Invalid Weborama
                    "platform": "weborama",
                    "params": {"tte": 2, "app": 3},
                },
                {  # Valid Weborama
                    "platform": "weborama",
                    "params": {"account": 1, "tte": 2, "app": 3},
                },
                {  # Invalid
                    "platform": "dcm",
                    "params": {
                        "url": "https://invalid-ad.doubleclick.net/path1?arg=[timestamp]"
                    },
                },
                {  # Valid Weborama
                    "platform": "weborama",
                    "params": {"account": 100, "tte": 200, "app": 300},
                },
                {  # Valid sizmek
                    "platform": "sizmek",
                    "params": {
                        "url": "https://bs.serving-sys.ru/path1?arg=[timestamp]"
                    },
                },
                {  # Invalid sizmek
                    "platform": "sizmek",
                    "params": {"url": "https://example.com/path1?arg=[timestamp]"},
                },
            ],
            {
                "audit-pixel-click-templated": [
                    "https://verify.yandex.ru/verify?platformid=4&a.si=1&a.te=2&a.aap=3&maid=[rdid]&a.A=cl",
                    "https://verify.yandex.ru/verify?platformid=4&a.si=100&a.te=200&a.aap=300&maid=[rdid]&a.A=cl",
                ],
                "audit-pixel-load-templated": [
                    "https://verify.yandex.ru/verify?platformid=4&a.si=1&a.te=2&a.aap=3&maid=[rdid]&a.A=ev&a.evn=load",
                    "https://verify.yandex.ru/verify?platformid=4&a.si=100&a.te=200&a.aap=300&maid=[rdid]&a.A=ev&a.evn=load",
                ],
                "audit-pixel-im-templated": [
                    "https://ad.doubleclick.net/path1;dc_rdid=[uadid];dc_lat=[lat]?arg=[timestamp]",
                    "https://verify.yandex.ru/verify?platformid=4&a.si=1&a.te=2&a.aap=3&maid=[rdid]&a.A=im",
                    "https://verify.yandex.ru/verify?platformid=4&a.si=100&a.te=200&a.aap=300&maid=[rdid]&a.A=im",
                    "https://bs.serving-sys.ru/path1?arg=[timestamp]",
                ],
                "audit-pixel-mrc50-templated": [
                    "https://verify.yandex.ru/verify?platformid=4&a.si=1&a.te=2&a.aap=3&maid=[rdid]&a.A=ev&a.evn=MRCViewable",
                    "https://verify.yandex.ru/verify?platformid=4&a.si=100&a.te=200&a.aap=300&maid=[rdid]&a.A=ev&a.evn=MRCViewable",
                ],
                "audit-pixel-mrc100-templated": [
                    "https://verify.yandex.ru/verify?platformid=4&a.si=1&a.te=2&a.aap=3&maid=[rdid]&a.A=ev&a.evn=MRC100Viewable",
                    "https://verify.yandex.ru/verify?platformid=4&a.si=100&a.te=200&a.aap=300&maid=[rdid]&a.A=ev&a.evn=MRC100Viewable",
                ],
            },
            None,
        ),
        (  # Valid DCM config, production
            [
                {
                    "platform": "dcm",
                    "params": {
                        "url": "https://ad.doubleclick.net/path?arg=[timestamp]"
                    },
                },
                {
                    "platform": "dcm",
                    "params": {"url": "https://ad.doubleclick.net/path"},
                },
            ],
            {
                "audit-pixel-im-templated": [
                    "https://ad.doubleclick.net/path?arg=[timestamp]",
                    "https://ad.doubleclick.net/path",
                ],
            },
            "production",
        ),
    ],
)
def test_transform_audit_pixel(verification_data, expected, ctype):
    result = transform_audit_pixel(verification_data, ctype=ctype or "testing")
    assert result == expected
