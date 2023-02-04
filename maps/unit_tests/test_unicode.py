from maps.b2bgeo.pipedrive_gate.lib.logic.util import unicode_unescape


def test_unicode_unescape():
    assert unicode_unescape('') == ''
    assert unicode_unescape('foo') == 'foo'
    assert unicode_unescape(r'\u0431\u043e\u043b\u0435\u0435 50') == 'более 50'
