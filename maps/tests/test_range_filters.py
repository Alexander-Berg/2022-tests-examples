from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.search.business_pb2 import ResponseMetadata
import math

validator = Validator('search')


def test_range_filters():
    message = ResponseMetadata()

    f = message.filter.add()
    f.id = 'hotel_reservation'
    f.name = 'Hotel reservation dates'
    f.date_filter.SetInParent()

    f = message.filter.add()
    f.id = 'price_room_range'
    f.name = 'Цена проживания за ночь'
    setattr(f.range_filter, 'from', 4000)
    f.range_filter.to = 20000

    f = message.filter.add()
    f.id = 'rating_range'
    f.name = 'Рейтинг организации'
    setattr(f.range_filter, 'from', 0)
    f.range_filter.to = 5

    f = message.filter.add()
    f.id = 'number_of_lanes'
    f.name = 'Количество дорожек (недоступный признак)'
    f.disabled = True
    setattr(f.range_filter, 'from', math.nan)
    f.range_filter.to = math.nan

    validator.validate_example(message, 'range_filters')
