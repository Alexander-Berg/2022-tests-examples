from maps.doc.proto.testhelper.fakers import AvatarFaker
from maps.doc.proto.testhelper.common import add_metadata, add_property, Point, set_point
from maps.doc.proto.testhelper.common import Image, add_image, Icon, add_icon
from maps.doc.proto.testhelper.validator import Validator

from yandex.maps.proto.common2 import geo_object_pb2
from yandex.maps.proto.billboard import billboard_pb2

url_template = AvatarFaker('geoadv-ext', seed=1984)


def make_image(type_, suffix):
    return Image(url_template(size=f'{suffix}%s'), tags=[type_])


def add_action(msg, type_, props):
    a = msg.action.add()
    a.type = type_
    for key, value in props:
        add_property(a, key, value)


def add_creative(msg, id_, type_, image, props=[]):
    c = msg.creative.add()
    c.id = id_
    c.type = type_
    add_image(c, image)
    for key, value in props:
        add_property(c, key, value)


def test_billboard():
    message = geo_object_pb2.GeoObject()
    md = add_metadata(message, billboard_pb2.RESPONSE_METADATA)
    md.reqid = '1562932445533157-1707729674-vla1-1979-vla-addrs-advert-18103'

    geo_object = message.geo_object.add()
    md = add_metadata(geo_object, billboard_pb2.GEO_OBJECT_METADATA)
    md.place_id = 'altay:170916721672'
    md.title = 'Лента'
    md.address = 'Россия, Москва, Большая Черёмушкинская улица, 1'
    md.log_id = '{"advertiserId": "None", "campaignId": "10139", "product": "pin_on_route_v2"}'

    url = 'https://bags.lenta.com/?utm_source=yandex_rtb&utm_medium=display&utm_campaign=lenta_americantourister_msk_rw-jul19&utm_content=banner_pin_gm'
    add_action(md, 'OpenSite', [('url', url), ('title', 'На сайт')])

    add_creative(md,
                 id_='70c03229b2d10f39ff41884a856286ffc1cddc0abc37abfb0b8209f2d5768267',
                 type_='banner',
                 image=make_image('balloonBanner', 'banner'))

    add_property(md, 'campaignId', '10139')
    add_property(md, 'product', 'pin_on_route_v2')
    add_property(md, 'pinTitle', 'ЛЕНТА')
    add_property(md, 'pinSubtitle', 'гипермаркет')

    add_icon(md, Icon(make_image('dust', 'geo_adv_dust')))
    add_icon(md, Icon(make_image('icon', 'geo_adv_drop')))
    add_icon(md, Icon(make_image('selected', 'geo_adv_pin'), anchor=(0.5, 0.9375)))

    add_image(md, make_image('balloonBanner', 'banner'))

    set_point(geo_object.geometry.add(), Point(37.601612, 55.689297))

    Validator('billboard').validate_example(message, 'billboard')
