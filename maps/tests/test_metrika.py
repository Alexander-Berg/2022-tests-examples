from maps.doc.proto.testhelper.common import add_metadata
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.common2 import geo_object_pb2
from yandex.maps.proto.search import metrika_pb2

validator = Validator('search')


def test_metrika():
    message = geo_object_pb2.GeoObject()
    metadata = add_metadata(message, metrika_pb2.GEO_OBJECT_METADATA)

    metadata.counter = "12345"
    metadata.goals.call = "geo.make-call"
    metadata.goals.route = "geo.make-route"
    metadata.goals.cta = "geo.cta"

    validator.validate_example(message, 'metrika')
