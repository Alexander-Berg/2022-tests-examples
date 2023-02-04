import irt.utils


def test_strip_html():
    text = '<a href="https://apple.com">Apple</a>'
    assert irt.utils.strip_html(text) == 'Apple'
