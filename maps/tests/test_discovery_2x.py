from .collection_common import Author, Collection, add_collection
from maps.doc.proto.testhelper.common import Image, add_metadata
from maps.doc.proto.testhelper.fakers import AvatarFaker
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.common2.geo_object_pb2 import GeoObject
from yandex.maps.proto.search import discovery_2x_pb2

url_template_faker = AvatarFaker('discovery-int', seed=15116)
validator = Validator('search')


def discovery_metadata(message):
    return add_metadata(message.geo_object.add(), discovery_2x_pb2.GEO_OBJECT_METADATA)


def test_discovery_2x_metadata():
    geo_object = GeoObject()
    metadata = discovery_metadata(geo_object)

    add_collection(metadata, Collection(
        id_='vistavki-v-peterburge',
        title='Главные выставочные пространства Санкт-Петербурга',
        author=Author(
            name='Яндекс.Афиша',
            favicon=Image(url_template=url_template_faker('afisha-yandex')))
    ))

    add_collection(metadata, Collection(
        id_='luchshie-muzei-peterburga',
        title='Лучшие музеи Петербурга',
        author=Author(
            name='KudaGo',
            favicon=Image(url_template=url_template_faker('kudago')))
    ))

    validator.validate_example(geo_object, 'discovery_2x')
