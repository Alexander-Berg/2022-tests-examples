# coding: utf-8
from unittest import TestCase

from lxml import etree

from at.aux_.entries import models
from at.aux_.entries.serializers.xml.entries import XMLSerializer, XMLDeserializer


class StringFieldsDeserializationTest(TestCase):

    def test_deserialize_without_body_node(self):
        xml = """
        <entry><content>
            <title>wow</title>
        </content></entry>
        """
        deserializer = XMLDeserializer(entry_cls=models.Text)

        data = deserializer.deserialize_xml_content(source=etree.XML(xml))

        # дефолты обрабатываются в модели при создании поля
        self.assertFalse('_body' in data)

    def test_deserialize_without_body_original_node(self):
        xml = """
        <entry><content>
            <title>wow</title>
        </content></entry>
        """
        deserializer = XMLDeserializer(entry_cls=models.Text)

        data = deserializer.deserialize_xml_content(source=etree.XML(xml))

        self.assertFalse('body_original' in data)

    def test_deserialize_with_empty_body_node(self):
        xml = """
        <entry><content>
            <title>wow</title>
            <body/>
        </content></entry>
        """
        deserializer = XMLDeserializer(entry_cls=models.Text)

        data = deserializer.deserialize_xml_content(source=etree.XML(xml))

        self.assertEqual(data['_body'], '')

    def test_deserialize_with_body_node_without_root_tag(self):
        xml = """
        <entry><content>
            <title>wow</title>
            <body><p>one</p><p>two</p></body>
        </content></entry>
        """
        deserializer = XMLDeserializer(entry_cls=models.Text)

        data = deserializer.deserialize_xml_content(source=etree.XML(xml))

        self.assertEqual(data['_body'], '<p>one</p><p>two</p>')

    def test_deserialize_with_body_node_with_comment_tag(self):
        xml = """
        <entry><content>
            <title>wow</title>
            <body><div>one</div><!--wf-ws v1--></body>
        </content></entry>
        """
        deserializer = XMLDeserializer(entry_cls=models.Text)

        data = deserializer.deserialize_xml_content(source=etree.XML(xml))

        self.assertEqual(data['_body'], '<div>one</div><!--wf-ws v1-->')

    def test_deserialize_with_body_node_with_root_tag(self):
        xml = """
        <entry><content>
            <title>wow</title>
            <body><div>one</div></body>
        </content></entry>
        """
        deserializer = XMLDeserializer(entry_cls=models.Text)

        data = deserializer.deserialize_xml_content(source=etree.XML(xml))

        self.assertEqual(data['_body'], '<div>one</div>')

    def test_deserialize_with_body_node_with_some_text(self):
        xml = """
        <entry><content>
            <title>wow</title>
            <body>Hello<div>one</div>WAT</body>
        </content></entry>
        """
        deserializer = XMLDeserializer(entry_cls=models.Text)

        data = deserializer.deserialize_xml_content(source=etree.XML(xml))

        self.assertEqual(data['_body'], 'Hello<div>one</div>WAT')

    def test_deserialize_with_body_node_with_attrs(self):
        xml = """
        <entry><content>
            <title>wow</title>
            <body type="text/plain">Hello<div>one</div>WAT</body>
        </content></entry>
        """
        deserializer = XMLDeserializer(entry_cls=models.Text)

        data = deserializer.deserialize_xml_content(source=etree.XML(xml))

        self.assertEqual(data['_body'], 'Hello<div>one</div>WAT')

    def test_deserialize_no_title(self):
        xml = """
        <entry><content>
        </content></entry>
        """
        deserializer = XMLDeserializer(entry_cls=models.Text)

        data = deserializer.deserialize_xml_content(source=etree.XML(xml))

        self.assertFalse('_title' in data)

    def test_deserialize_empty_title(self):
        xml = """
        <entry><content>
            <title/>
        </content></entry>
        """
        deserializer = XMLDeserializer(entry_cls=models.Text)

        data = deserializer.deserialize_xml_content(source=etree.XML(xml))

        self.assertEqual(data['_title'], '')


class TextModelMock(object):

    fields = models.Text.fields

    def __init__(self, **kwargs):
        self.__dict__.update(kwargs)


class StringFieldsSerializationTest(TestCase):

    def test_serialize_body_empty_string(self):
        model = TextModelMock(_body='')
        serializer = XMLSerializer(object=model)

        body_node = serializer.serialize_body()
        body_node_str = etree.tostring(body_node)

        self.assertEqual(body_node.text, None)
        self.assertEqual(body_node.tag, 'body')
        self.assertEqual(body_node_str, b'<body/>')

    def test_serialize_body_one_tag(self):
        model = TextModelMock(_body='<p>hello</p>')
        serializer = XMLSerializer(object=model)

        body_node = serializer.serialize_body()
        body_node_str = etree.tostring(body_node)

        self.assertEqual(body_node_str, b'<body><p>hello</p></body>')

    def test_serialize_body_some_tags(self):
        model = TextModelMock(_body='<p>hello</p><div>brabu</div>')
        serializer = XMLSerializer(object=model)

        body_node = serializer.serialize_body()
        body_node_str = etree.tostring(body_node)

        self.assertEqual(
            body_node_str,
            b'<body><p>hello</p><div>brabu</div></body>',
        )

    def test_serialize_body_complex_tags_mix(self):
        model = TextModelMock(_body='OMG<p>hello</p>WOW')
        serializer = XMLSerializer(object=model)

        body_node = serializer.serialize_body()
        body_node_str = etree.tostring(body_node)

        self.assertEqual(
            body_node_str,
            b'<body>OMG<p>hello</p>WOW</body>',
        )

    def test_serialize_body_original_empty(self):
        model = TextModelMock(body_original='')
        serializer = XMLSerializer(object=model)

        content_node = etree.Element("content")
        serializer.serialize_section(content_node, serializer.meta.content_fields)

        body_original = content_node.find('body-original')
        self.assertNotEqual(body_original, None)
        self.assertEqual(body_original.text, '')

    def test_serialize_body_original_empty_with_type(self):
        model = TextModelMock(body_original='', content_type='text/wiki')
        serializer = XMLSerializer(object=model)

        content_node = etree.Element("content")
        serializer.serialize_section(content_node, serializer.meta.content_fields)
        serializer.build_content_type_node(content_node)

        body_original = content_node.find('body-original')
        self.assertNotEqual(body_original, None)
        self.assertEqual(body_original.text, '')
        self.assertEqual(body_original.attrib['type'], 'text/wiki')

