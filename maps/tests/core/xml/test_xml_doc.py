import lxml.etree

from maps_adv.export.lib.core.xml.base import XmlDoc


def test_will_remove_tag_without_children_when_use_operator_with():
    doc = XmlDoc("root")
    with doc.tag("parent"):
        pass

    result = len(doc.result.getchildren())

    assert result == 0


def test_will_not_remove_tag_if_has_children_when_use_operator_with():
    doc = XmlDoc("root")
    with doc.tag("parent"):
        doc.tag("child")

    result = len(doc.result.getchildren())

    assert result == 1


def test_will_add_attributes_to_tag_as_expected():
    doc, tag, _, attr, _ = XmlDoc("root").ttaa()
    with tag("child", attr1="1"):
        attr(attr2="2")
        tag("hasChild")

    result = doc.result.getchildren()[0].attrib

    assert result == dict(attr1="1", attr2="2")


def test_will_add_text_to_tag_as_expected():
    doc, tag, text, _, _ = XmlDoc("root").ttaa()
    tag("child", text="1")
    text("2")

    root_text = doc.result.getchildren()[0].text
    tag_text = doc.result.text

    assert tag_text == "2"
    assert root_text == "1"


def test_will_append_child_to_tag_as_expected():
    doc, tag, _, _, append = XmlDoc("root").ttaa()

    with tag("parent", text="1"):
        append(lxml.etree.Element("child"))

    result = len(doc.result.getchildren()[0])

    assert result == 1
