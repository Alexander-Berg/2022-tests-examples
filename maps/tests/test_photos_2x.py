from maps.doc.proto.testhelper.common import add_metadata
from maps.doc.proto.testhelper.fakers import AvatarFaker
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.common2.geo_object_pb2 import GeoObject
from yandex.maps.proto.search import photos_2x_pb2

fake_image = AvatarFaker('altay', seed=33)
validator = Validator('search')


def photos_2x_metadata(message):
    return add_metadata(message.geo_object.add(), photos_2x_pb2.GEO_OBJECT_METADATA)


def add_photo(md, name: str):
    return md.photo.add(url_template=fake_image(name))


def test_photos_2x_metadata():
    geo_object = GeoObject()
    metadata = photos_2x_metadata(geo_object)
    metadata.count = 8

    photo = add_photo(metadata, 'best-photo')
    photo.link.add(type='panorama',
                   uri='ymapslink://panorama?id=1298163377_673666336_23_1586687010&span=90.0%2C45.0&direction=-97.5%2C10.0')
    photo.link.add(type='menu',
                   uri='http://cafe-anderson.ru/menu/breakfasts')

    add_photo(metadata, 'photo1')
    add_photo(metadata, 'photo2')

    validator.validate_example(geo_object, 'photos_2x')
