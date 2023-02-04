import binascii


from unittest.mock import Mock, patch


from infra.rtc.docker_registry.docker_torrents.clients.skyboned_helper import SkybonedTorrentHelper


def test_get_mds_url():
    # tested unit should contain namespace and startswith base url

    _mds_key = '12345-54321'
    m = Mock(spec=SkybonedTorrentHelper)
    m.mds_read_baseurl = 'mock://ex.y-t.ru'
    m.namespace = 'example'

    assert SkybonedTorrentHelper._get_mds_url(m, _mds_key) == 'mock://ex.y-t.ru/get-example/12345-54321'


def test_get_mds_content_size():
    # tested unit should get 'content-length' from head response

    ex = Mock()

    # mocked head response
    mrsp = Mock()
    mrsp.status_code = 200
    mrsp.headers = {'Content-Length': '666'}

    # mocked session
    ms = Mock()
    ms.__enter__ = Mock()
    ms.__exit__ = Mock()
    ms.__enter__.return_value.head = Mock(return_value=mrsp)

    # mocked skyboned helper
    m = Mock(spec=SkybonedTorrentHelper)
    m.logger = Mock()
    m.connect_timeout = Mock()

    m._get_http_session = Mock(return_value=ms)

    assert SkybonedTorrentHelper._get_mds_content_size(m, ex) == 666


def test_get_mds_content_ranges():
    # tested unit should return proper ranges

    # test content sizes, ranges expectations
    chunk = 1024 * 1024 * 4
    test_cases = [[1, [(0, 0)]], [4194304, [(0, 4194303)]], [6291456, [(0, 4194303), (4194304, 6291455)]]]

    for t in test_cases:
        assert SkybonedTorrentHelper.get_mds_content_ranges(t[0], chunk) == t[1]


def test_calc_stream_hashes():
    # tested unit should return calculated hashes

    # test case
    mocked_url = "mock://ex.y-t.ru/get-example/123456-789"
    test_size = 10
    test_content = b'\1' * 10
    expected_md5 = "484c5624910e6288fad69e572a0637f7"
    expected_sha1_digest = [binascii.a2b_hex("195ae9b612afbf92d2515cfee898f82f601a9107")]

    # mocked skyboned helper
    m = Mock(spec=SkybonedTorrentHelper)
    m.logger = Mock()

    m._get_mds_content_size = Mock(return_value=test_size)
    m._get_mds_content_with_range = Mock(return_value=test_content)

    assert SkybonedTorrentHelper.calc_stream_hashes(m, mocked_url) == (expected_md5, expected_sha1_digest, test_size)


def test_generate_rbtorrent():
    # tested unit should add torrent

    # test case
    test_filename = "test_image"
    test_mds_key = "9876-54321"

    # mocked skyboned helper
    m = Mock(spec=SkybonedTorrentHelper)
    m.logger = Mock()
    m.server = Mock()

    m._request_tvm_ticket = Mock(return_value="ok")
    m._get_mds_url = Mock(return_value='mock://ex.y-t.ru/get-example/9876-54321')
    m.calc_stream_hashes = Mock(return_value=("484c5624910e6288fad69e572a0637f7",
                                              [binascii.a2b_hex("195ae9b612afbf92d2515cfee898f82f601a9107")], 10))
    with patch('infra.skyboned.api.skyboned_add_resource',
               return_value=('rbtorrent:d1488ef73e8c2ffc1f3fe0cecc66b345ebba0e85', True)):
        assert SkybonedTorrentHelper.generate_rbtorrent(m, test_filename,
                                                        test_mds_key) == "rbtorrent:d1488ef73e8c2ffc1f3fe0cecc66b345ebba0e85"


def test_remove_torrent_byid():
    # tested unit should add torrent

    # test case
    torrent_id = "rbtorrent:d1488ef73e8c2ffc1f3fe0cecc66b345ebba0e85"

    # mocked skyboned helper
    m = Mock(spec=SkybonedTorrentHelper)
    m.logger = Mock()
    m.server = Mock()

    m._request_tvm_ticket = Mock(return_value="ok")
    with patch('infra.skyboned.api.skyboned_remove_resource', return_value=True):
        assert SkybonedTorrentHelper.remove_rbtorrent_byid(m, torrent_id)
