from urllib.parse import unquote

from wiki.utils.supertag import translit


def test_slugify():
    original_url = 'connect/support/tag/%D0%A2%D0%B5%D0%B3%D0%B8-%D0%92%D0%B8%D0%BA%D0%B8'
    supertag = translit(unquote(original_url))
    assert supertag == 'connect/support/tag/tegi-wiki'
