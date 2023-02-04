from maps.doc.proto.testhelper.common import Image, add_metadata
from maps.doc.proto.testhelper.validator import Validator
from .collection_common import Author, Collection, set_collection
from yandex.maps.proto.common2.geo_object_pb2 import GeoObject
from yandex.maps.proto.search import collection_pb2

validator = Validator('search')


def collection_metadata(message):
    return add_metadata(message.geo_object.add(), collection_pb2.COLLECTION_METADATA)


def test_collection_metadata():
    geo_object = GeoObject()
    metadata = collection_metadata(geo_object)
    set_collection(metadata.collection, Collection(
        id_='krasnodar-best-restaurants-2018',
        title='Лучшие кафе и рестораны Краснодара',
        description='Пользователи Яндекса выбрали лучшие места 2018 года..',
        image=Image(url_template='https://avatars.mds.yandex.net/get-discovery-int/...'),
        rubric='Рестораны',
        item_count=19,
        author=Author(
            name='Выбор пользователей Яндекса',
            favicon=Image(url_template='https://avatars.mds.yandex.net/get-discovery-int/...')
        )
    ))

    validator.validate_example(geo_object, 'collection')
