from maps.doc.proto.testhelper.fakers import AvatarFaker
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.search.booking_pb2 import BookingResponse

validator = Validator('search')
favicon = AvatarFaker('travel-hotels', seed=42)


def test_booking_response():
    response = BookingResponse()
    response.params.check_in.value = 1560176140
    response.params.check_in.tz_offset = 0
    response.params.check_in.text = '2019-06-10'
    response.params.nights = 1
    response.params.persons = 2

    offer = response.offer.add()
    offer.partner_name = 'Booking.com'
    offer.booking_link.add().uri = \
        'https://travel.yandex.net/redir?PUrl=-FWEOknM...'
    link = offer.booking_link.add()
    link.uri = 'https://travel.yandex.net/redir?PUrl=-SJRBxaZ...'
    link.type = 'web'
    offer.favicon.url_template = favicon(name='png_icons_booking')
    offer.price.value = 6153
    offer.price.currency = 'RUB'
    offer.price.text = '6153 ₽'

    offer = response.offer.add()
    offer.partner_name = 'Ostrovok.ru'
    offer.booking_link.add().uri = \
        'https://travel.yandex.net/redir?PUrl=HH5ofKb...'
    offer.favicon.url_template = favicon(name='png_icons_ostrovok')
    offer.price.value = 7896
    offer.price.currency = 'RUB'
    offer.price.text = '7896 ₽'

    validator.validate_example(response, 'booking')
