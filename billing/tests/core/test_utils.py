from refs.core.utils import get_media_path, camelcase_to_underscores, query_string_to_dict


def test_qs_to_string():
    src = query_string_to_dict('draw=7&columns%5B0%5D%5Bdata%5D=0')

    assert "    'draw': '7',\n" in src
    assert "    'columns[0][data]': '0',\n" in src


def test_get_media_path():
    filename = 'filen'
    path = get_media_path('test')

    assert 'media' in path
    assert 'test' in path
    assert filename not in path

    path = get_media_path('test', filename)

    assert 'media' in path
    assert 'test' in path
    assert filename in path


def test_to_camelcase():
    assert camelcase_to_underscores('SomeName') == 'some_name'
