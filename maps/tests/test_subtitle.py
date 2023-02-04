from maps.doc.proto.testhelper.common import add_metadata, add_property
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.common2.geo_object_pb2 import GeoObject
from yandex.maps.proto.search import subtitle_pb2

validator = Validator('search')


def next_geo_object_subtitle_metadata(message):
    return add_metadata(message.geo_object.add(), subtitle_pb2.GEO_OBJECT_METADATA)


def test_subtitle_fuel():
    message = GeoObject()
    metadata = next_geo_object_subtitle_metadata(message)

    item = metadata.subtitle_item.add()
    item.type = 'fuel'
    item.text = 'АИ-95 39.15 руб'
    add_property(item, 'name', 'АИ-95')
    add_property(item, 'price', '39.15')
    add_property(item, 'currency', 'RUB')

    item = metadata.subtitle_item.add()
    item.type = 'fuel'
    item.text = 'АИ-92 37.22 руб'
    add_property(item, 'name', 'АИ-92')
    add_property(item, 'price', '37.22')
    add_property(item, 'currency', 'RUB')

    validator.validate_example(message, 'subtitle_fuel')


def test_subtitle_exchange():
    message = GeoObject()
    metadata = next_geo_object_subtitle_metadata(message)

    item = metadata.subtitle_item.add()
    item.type = 'exchange'
    item.text = 'EUR 68.36 / 68.64'
    add_property(item, 'currency', 'EUR')
    add_property(item, 'buy', '68.36')
    add_property(item, 'sell', '68.64')

    validator.validate_example(message, 'subtitle_exchange')


def test_subtitle():
    message = GeoObject()

    metadata = next_geo_object_subtitle_metadata(message)

    item = metadata.subtitle_item.add()
    item.type = 'travel_time'
    add_property(item, 'type', 'walking')
    add_property(item, 'text', '5 мин')

    item = metadata.subtitle_item.add()
    item.type = 'travel_time'
    add_property(item, 'type', 'driving')
    add_property(item, 'text', '15 мин')

    item = metadata.subtitle_item.add()
    item.type = 'travel_time'
    add_property(item, 'type', 'transit')
    add_property(item, 'text', '10 мин')

    metadata = next_geo_object_subtitle_metadata(message)
    item = metadata.subtitle_item.add()
    item.type = 'working_hours'
    item.text = 'Закрыто до 10:00'
    add_property(item, 'type', 'closed')

    validator.validate_example(message, 'subtitle')


def test_serp_subtitle():
    message = GeoObject()
    metadata = next_geo_object_subtitle_metadata(message)

    def fill_manicure_item(item):
        item.type = "classic_female_manicure_coated"
        item.text = "Маникюр: 1900 ₽"
        add_property(item, "price", "1900")
        add_property(item, "currency", "₽")
        add_property(item, "prefix", "Маникюр")

    item = metadata.subtitle_item.add()
    item.type = 'rating'
    item.text = '10'
    add_property(item, 'value', '10')
    add_property(item, 'value_5', '5')

    fill_manicure_item(metadata.subtitle_item.add())
    fill_manicure_item(metadata.serp_subtitle_item.add())

    validator.validate_example(message, 'subtitle_serp')
