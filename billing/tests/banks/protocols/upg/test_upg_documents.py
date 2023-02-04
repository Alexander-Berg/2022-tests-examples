import re

import pytest
from bcl.banks.protocols.upg.exceptions import UpgValidationError
from bcl.banks.protocols.upg.schemas.base import get_upg_namespaces, Document, UpgSchema
from bcl.toolbox.utils import XmlUtils
from marshmallow import fields


def test_xml_to_dict(read_fixture):
    xml = read_fixture('response.xml', decode='utf-8')
    d = XmlUtils.to_dict(xml)
    assert 'Response' in d
    assert d['Response']['@version'] == '1.1'
    assert d['Response']['Dummy']['='] == 'dummy_value'
    assert d['Response']['Dummy']['@attr1'] == '1'
    trans = d['Response']['StatementsRaif']['StatementRaif']['Docs']['TransInfo'][0]
    assert trans['@docSum'] == '1217.57'
    assert trans['PersonalName'] == 'ОАО Кто-то'


def test_xml_from_dict(read_fixture):

    src_dict = {
        'Request': {
            '@requestId': '85c6b6c3-c314-4cb1-a452-dbbc1c48e754',
            '@sender': 'cc289e3b-04f1-42c7-a156-9dedbd80a6d9',
            '@version': '1',
            'StmtReqRaif': {
                '@date': '2014-03-23',
                '@docExtId': 'ef5efe58-f720-4088-b1a7-a308b786d39d',
                '@docNumber': '1',
                '@docDate': '2014-03-23',
                '@orgName': 'ООО offline',
                '@stmtType': '6',
                '@inn': '1234567890',
                'Accounts': {
                    'Account': [
                        {
                            '@bic': '040173725',
                            '=': '33333810133333333333',
                        },
                    ],
                },
            },
            'Signs': {
                'Sign': [
                    {
                        'SN': '6FE9AAF900020006D6AC',
                        'Value': '1',
                        'Issuer': '2',
                        'DigestName': '3',
                        'DigestVersion': '4',
                        'SignType': '5',
                    },
                    {
                        'SN': '7FE9AAF900020006D6AC',
                        'Value': '11',
                        'Issuer': '22',
                        'DigestName': '33',
                        'DigestVersion': '44',
                        'SignType': '55',
                    }
                ]
            },
        },
    }
    ns_map = get_upg_namespaces('raif', 'request')
    xml = XmlUtils.from_dict(src_dict, 'upg', ns_map)
    xml_orig = read_fixture('request.xml', decode='utf-8')
    assert xml == re.sub(r'\s+', ' ', xml_orig).replace('> <', '><')


def test_repeatable_tags_getter():
    class Tag(UpgSchema):
        value = fields.String(load_from='Value')

    class Tags(UpgSchema):
        tag = fields.Nested(Tag, load_from='Tag', many=True)

    class SomeDocument(Document):
        tags = fields.Nested(Tags, load_from='Tags')

    assert SomeDocument._get_tag_hints() == (['Tag'], ['Value'])


def test_validation():
    class Tag(UpgSchema):
        value = fields.Int(load_from='ValueFrom', required=True)
        another_value = fields.Int(load_from='AnotherValueFrom', required=True)

    class Tags(UpgSchema):
        tag = fields.Nested(Tag, load_from='TagFrom', many=True, required=True)

    class SomeDocument(Document):
        tags = fields.Nested(Tags, load_from='TagsFrom', required=True)
        another_tags = fields.Nested(Tags, load_from='AnotherTagsFrom', required=True)

    dct = {
        'TagsFrom': {'TagFrom': [
            {'ValueFrom': 'NotAnInt', 'AnotherValueFrom': 'NotAnInt1'},
            {'ValueFrom': 'NotAnInt', 'AnotherValueFrom': 'NotAnInt2'}
        ]},
        'AnotherTagsFrom': {'TagFrom': [{'ValueFrom': 'NotAnInt', 'AnotherValueFrom': 'NotAnInt'}]}
    }

    expected = [
        {
            '/TagsFrom/TagFrom/0/AnotherValueFrom: `NotAnInt1` - Not a valid integer.',
            '/TagsFrom/TagFrom/0/ValueFrom: `NotAnInt` - Not a valid integer.',
            '/TagsFrom/TagFrom/1/AnotherValueFrom: `NotAnInt2` - Not a valid integer.',
            '/TagsFrom/TagFrom/1/ValueFrom: `NotAnInt` - Not a valid integer.'
        },
        {
            '/AnotherTagsFrom/TagFrom/0/AnotherValueFrom: `NotAnInt` - Not a valid integer.',
            '/AnotherTagsFrom/TagFrom/0/ValueFrom: `NotAnInt` - Not a valid integer.'
        }
    ]

    with pytest.raises(UpgValidationError) as e:
        SomeDocument(dct)

    def split_error_message(e):
        msg = str(e.value)
        return [set(group for group in groups.split('; ')) for groups in msg.split('\n')]

    messages = split_error_message(e)
    assert len(messages) == 2
    assert expected[0] in messages
    assert expected[1] in messages

    expected = [
        {
            '/TagsFrom/TagFrom/0/AnotherValueFrom: `None` - Missing data for required field.',
            '/TagsFrom/TagFrom/0/ValueFrom: `None` - Missing data for required field.'
        },
        {
            '/AnotherTagsFrom/TagFrom/0/AnotherValueFrom: `None` - Missing data for required field.',
            '/AnotherTagsFrom/TagFrom/0/ValueFrom: `None` - Missing data for required field.'
        }
    ]

    with pytest.raises(UpgValidationError) as e:
        SomeDocument({})

    messages = split_error_message(e)
    assert len(messages) == 2
    assert expected[0] in messages
    assert expected[1] in messages
