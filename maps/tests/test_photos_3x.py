from maps.doc.proto.testhelper.common import add_metadata
from maps.doc.proto.testhelper.fakers import AvatarFaker
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.common2.geo_object_pb2 import GeoObject
from yandex.maps.proto.common2 import image_pb2
from yandex.maps.proto.search import photos_3x_pb2

fake_image = AvatarFaker('altay', seed=33)
validator = Validator('search')


def photos_3x_metadata(message):
    return add_metadata(message.geo_object.add(), photos_3x_pb2.PHOTOS_3X_METADATA)


def add_group(md, id, name, count):
    return md.group.add(id=id, name=name, count=count)


def make_image(name):
    return image_pb2.Image(url_template=fake_image(size=f'{name}_%s'))


def add_photo(md, name):
    return md.photo.add(image=make_image(name))


def test_photos_3x_metadata():
    geo_object = GeoObject()
    metadata = photos_3x_metadata(geo_object)

    gr1 = add_group(metadata, 'All', 'All photos', 8)
    add_photo(gr1, 'photo1')

    gr2 = add_group(metadata, 'Panorama', 'Panorama', 1)
    photo = add_photo(gr2, 'best-photo')
    photo.link.add(type='panorama',
                   uri='ymapslink://panorama?id=1298163377_673666336_23_1586687010&span=90.0%2C45.0&direction=-97.5%2C10.0')

    gr3 = add_group(metadata, 'Exterior', 'Exterior', 3)
    add_photo(gr3, 'photo2')

    validator.validate_example(geo_object, 'photos_3x')
