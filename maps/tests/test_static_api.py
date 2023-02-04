from maps.geoq.hypotheses.lib.static_api import (
    _format_geometry, _process_map_params,
    _sign_url,
    generate_polygon,
    generate_point
)

import base64


def test_format_geometry():
    assert _format_geometry([]) == ''
    assert _format_geometry([(1, 2), (3, 4)]) == '1,2,3,4'


def test_process_map_params():
    assert _process_map_params(a=1, b=None, c=(1, 2), d='d') == {'a': 1, 'c': '1,2', 'd': 'd'}


def test_sign_url():
    url_path = '/1.x/?l=map&size=1000%2C1000&pt=37.62007%2C55.75363%2C&api_key=key'
    secret = base64.b64encode(b'secret')

    assert _sign_url(url_path, secret) == 'D_g4higJPxVpIYxSCT8WUKU8WES6X4I5KOMK-1VlCHQ='


def test_generate_polygon():
    assert generate_polygon([(1, 2), (3, 4)], 'FFFFFFFF', 10, '00000000', 12) == 'c:FFFFFFFF,w:10,bc:00000000,bw:12,1,2,3,4'
    assert generate_polygon([(1, 2), (3, 4)]) == '1,2,3,4'


def test_generate_point():
    assert generate_point(1, 2, 'pm', 'wt', 's', 99) == '1,2,pmwts99'
    assert generate_point(1, 2) == '1,2'
