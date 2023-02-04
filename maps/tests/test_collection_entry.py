from maps.doc.proto.testhelper.common import add_metadata, Image, add_image
from maps.doc.proto.testhelper.fakers import AvatarFaker
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.common2.geo_object_pb2 import GeoObject
from yandex.maps.proto.search import collection_response_pb2

url_template_faker = AvatarFaker('discovery-int', seed=1742)
validator = Validator('search')


def add_link(msg, title, uri, tags):
    link = msg.link.add()
    link.title = title
    link.uri = uri
    link.tag.extend(tags)


def add_feature(msg, type_, name, value):
    feature = msg.feature.add()
    feature.type = type_
    feature.name = name
    feature.value = value


def collection_entry_metadata(message):
    return add_metadata(message.geo_object.add(), collection_response_pb2.COLLECTION_ENTRY_METADATA)


def test_collection_entry_metadata():
    geo_object = GeoObject()
    metadata = collection_entry_metadata(geo_object)
    metadata.title = 'Джейкоб Бэнкс в Zal'
    metadata.annotation = 'Концерт британского певца Джейкоба Бэнкса в Zal'
    metadata.description = 'Джейкоб Бэнкс сотрудничает с электронными музыкантами и пишет саундтреки к FIFA 19...'

    add_image(metadata, Image(url_template=url_template_faker('photo-1')))
    add_image(metadata, Image(url_template=url_template_faker('photo-2')))
    add_image(metadata, Image(
        url_template=url_template_faker('paragraph-icon'),
        tags=['paragraph_icon']))
    add_image(metadata, Image(
        url_template=url_template_faker('placemark-icon'),
        tags=['placemark_icon']))

    add_link(metadata, 'Посмотреть афишу', 'https://afisha.yandex.ru/...', ['showtimes'])

    add_feature(metadata, 'phone', 'Телефон', '+7(495)123-45-67')
    add_feature(metadata, 'average_bill2', 'Средний чек', '1000 ₽')
    add_feature(metadata, 'custom', 'Когда', '1-2 января')

    metadata.tag.append('rich')

    validator.validate_example(geo_object, 'collection_entry')
