from google.protobuf import json_format
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.search import encyclopedia_pb2

validator = Validator('search')


def validate_encyclopedia_metadata(data, example_name):
    metadata = encyclopedia_pb2.EncyclopediaMetadata()

    metadata.title = data['title']
    metadata.description = data['description']

    metadata.attribution.author.name = data['name']
    metadata.attribution.link.href = data['href']

    if data.get('fact', []):
        for fact_dict in data['fact']:
            fact = metadata.fact.add()
            json_format.ParseDict(fact_dict, fact)

    validator.validate_example(metadata, example_name)


def test_encyclopedia():
    wiki_ru = {
        'title': "Москва",
        'description': "столица России, город федерального значения",
        'name': "Википедия",
        'href': "https://ru.wikipedia.org/wiki/%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0",
        'fact': [
            {
                'name': 'Население',
                'value': [
                    {
                        'title': {
                            'text': '12 692 466 чел. (2020 г.)',
                            'span': []
                        }
                    },
                    {
                        'title': {
                            'text': '12 500 123 чел. (2018 г.)',
                            'span': []
                        }
                    }
                ]
            },
            {
                'name': 'Граничит с',
                'value': [
                    {
                        'title': {
                            'text': 'Московская область',
                            'span': [
                                {
                                    'begin': 0,
                                    'end': 18
                                }
                            ]
                        },
                        'url': 'https://yandex.ru/search/?text=%D0%BC%D0%BE%D1%81%D0%BA%D0%BE%D0%B2%D1%81%D0%BA%D0%B0%D1%8F+%D0%BE%D0%B1%D0%BB%D0%B0%D1%81%D1%82%D1%8C'
                    },
                    {
                        'title': {
                            'text': 'Калужская область',
                            'span': [
                                {
                                    'begin': 0,
                                    'end': 9
                                }
                            ]
                        },
                        'url': 'https://ru.wikipedia.org/wiki/%D0%9A%D0%B0%D0%BB%D1%83%D0%B3%D0%B0'
                    }
                ]
            },
            {
                'name': 'Площадь',
                'value': [
                    {
                        'title': {
                            'text': '2 561 кв. км.',
                            'span': []
                        }
                    }
                ],
            }
        ]
    }
    validate_encyclopedia_metadata(wiki_ru, 'encyclopedia_wiki_ru')

    wiki_en = {
        'title': "Moscow",
        'description': "the capital and most populous city of Russia",
        'name': "Wikipedia",
        'href': "https://en.wikipedia.org/wiki/Moscow"
    }
    validate_encyclopedia_metadata(wiki_en, 'encyclopedia_wiki_en')

    bigenc_ru = {
        'title': "Москва",
        'description': "го­род фе­де­раль­но­го зна­че­ния. Адм. центр Центр. фе­де­раль­но­го окр. и Мо­с­ков­ской обл. (не вхо­дит в её со­став).",
        'name': "Большая российская энциклопедия",
        'href': "https://bigenc.ru/geography/text/2232312",
        'fact': [
            {
                'name': 'Население',
                'value': [
                    {
                        'title': {
                            'text': 'св. 15 млн. чел',
                            'span': []
                        }
                    }
                ],
            }
        ]
    }
    validate_encyclopedia_metadata(bigenc_ru, 'encyclopedia_bigenc_ru')
