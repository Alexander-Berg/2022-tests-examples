import pytest

import infra.reconf
import infra.reconf_juggler


def test_docstring_absent():
    with pytest.raises(infra.reconf.ValidationError):
        class DoclessCheck(infra.reconf_juggler.Check):
            pass


def test_doc_url_absent():
    with pytest.raises(infra.reconf.ValidationError):
        class DoclessCheck(infra.reconf_juggler.Check):
            """docstring"""  # docstring validator should pass
            validate_class_doc_url = True  # disabled bu default


def test_doc_url_in_meta_links():
    class SomeCheck(infra.reconf_juggler.Check):
        """docstring"""  # docstring validator should pass
        doc_url = 'https://exaple.org/path/to/doc.txt'

    expected = [{
        'title': 'Docs',
        'type': 'doc_url',
        'url': 'https://exaple.org/path/to/doc.txt'
    }]

    assert expected == SomeCheck({}).build()['meta']['urls']
