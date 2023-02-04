from maps.doc.proto.testhelper.common import add_metadata
from maps.doc.proto.testhelper.fakers import AvatarFaker
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.common2.geo_object_pb2 import GeoObject
from yandex.maps.proto.search import business_images_pb2

fake_image = AvatarFaker('altay', seed=15)
validator = Validator('search')


def business_images_metadata(message):
    return add_metadata(message.geo_object.add(), business_images_pb2.GEO_OBJECT_METADATA)


def test_business_images_metadata():
    geo_object = GeoObject()
    metadata = business_images_metadata(geo_object)

    metadata.logo.url_template = fake_image('logo')

    validator.validate_example(geo_object, 'business_images')
