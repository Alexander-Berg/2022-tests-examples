from balance.mapper import DomainObject
from balance.xmlizer import Xmlizer
from balance.xmlizer import getxmlizer, xml2str


class ObjectA(DomainObject):
    def __init__(self):
        self.id = 1
        self.asd = u'asd'


class ObjectB(DomainObject):
    def __init__(self):
        self.id = 2
        self.bcd = 123
        self.objecta = ObjectA()


class ObjectAXmlizer(Xmlizer):
    xmlized_class = ObjectA


class ObjectBXmlizer(Xmlizer):
    xmlized_class = ObjectB


class TestRecursiveXMLizer(object):
    def test_simple_xml(self):
        a = ObjectA()
        assert '<object-a><id>1</id><asd>asd</asd></object-a>' == xml2str(getxmlizer(a).auto_xmlize())

    def test_two_objects(self):
        b = ObjectB()
        assert '<object-b><bcd>123</bcd><id>2</id><object-a><id>1</id><asd>asd</asd></object-a></object-b>' == xml2str(
            getxmlizer(b).auto_xmlize())

    def test_level(self):
        b = ObjectB()
        assert '<object-b><bcd>123</bcd><id>2</id></object-b>' == xml2str(getxmlizer(b).auto_xmlize(0))

    def test_xmlize_null(self):
        class Asd(DomainObject):
            def __init__(self):
                self.asd = None

        class asdXmlizer(Xmlizer):
            xmlized_class = Asd

        a = Asd()
        assert '<asd><asd is_null="1" /></asd>' == xml2str(getxmlizer(a).auto_xmlize())
