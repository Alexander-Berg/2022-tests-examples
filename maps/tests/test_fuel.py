from maps.doc.proto.testhelper.common import set_price, RUR
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.search import fuel_pb2

validator = Validator('search')


def add_fuel(metadata, name, price):
    fuel = metadata.fuel.add()
    fuel.name = name
    set_price(fuel, RUR(price))


def test_fuel():
    metadata = fuel_pb2.GeoObjectMetadata()

    metadata.timestamp = 1570512084

    add_fuel(metadata, "АИ 95", 46.45)
    add_fuel(metadata, "АИ 95+", 48.05)
    add_fuel(metadata, "АИ 92", 42.5)
    add_fuel(metadata, "ДТ+", 45.7)
    add_fuel(metadata, "АИ 98+", 53.15)

    metadata.attribution.author.name = "Яндекс.Заправки"
    metadata.attribution.author.uri = "https://zapravki.yandex.ru"

    validator.validate_example(metadata, 'fuel_snippet')
