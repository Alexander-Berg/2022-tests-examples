import pytest

from at.common import utils

@pytest.mark.parametrize('tree, result',
    [
        (("tag_name", "content"), b'<tag_name>content</tag_name>'),
        (("tag_name", {'attr_name': 'attr_value'}, "content"), b'<tag_name attr_name="attr_value">content</tag_name>'),
        (("tag_name", None), b'<tag_name></tag_name>'),
        (("tag_name", [("first_inner_tag", "content"), ("second_inner_tag", "content2")]),
        b"<tag_name><first_inner_tag>content</first_inner_tag><second_inner_tag>content2</second_inner_tag></tag_name>"),
        (("tag_name", ("inner_tag", ("inner_tag2", {'inner_attr': 'attr'}, "inner_content"))),
        b'<tag_name><inner_tag><inner_tag2 inner_attr="attr">inner_content</inner_tag2></inner_tag></tag_name>')
    ]
)
def test_generator(tree, result):
    assert utils.build_xml(tree) == result




