from maps.doc.proto.testhelper.common import add_metadata
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.common2.geo_object_pb2 import GeoObject
from yandex.maps.proto.search.business_pb2 import EnumItem, GEO_OBJECT_METADATA
from yandex.maps.proto.search.kind_pb2 import COUNTRY, PROVINCE, LOCALITY, STREET, HOUSE

validator = Validator('search')


def business_metadata(message: GeoObject):
    return add_metadata(message.geo_object.add(), GEO_OBJECT_METADATA)


def test_business_metadata():
    geo_object = GeoObject()
    org = business_metadata(geo_object)

    # misc
    org.id = "1382578436"
    org.name = "Falconeri"

    # category
    category = org.category.add(name='Магазин одежды')
    setattr(category, 'class', "clothes shop")
    category.tag.append("icon:clothes shop")

    # address
    address = org.address
    address.country_code = "RU"
    address.postal_code = "125171"
    address.formatted_address = "Россия, Москва, Ленинградское шоссе, 16Ас4"
    address.additional_info = "ТЦ Метрополис, эт. 1"
    address.component.add(kind=[COUNTRY], name="Россия")
    address.component.add(kind=[PROVINCE], name="Центральный федеральный округ")
    address.component.add(kind=[PROVINCE], name="Москва")
    address.component.add(kind=[LOCALITY], name="Москва")
    address.component.add(kind=[STREET], name="Ленинградское шоссе")
    address.component.add(kind=[HOUSE], name="16А, стр. 4")

    # features
    org.feature.add(id='delivery', name='доставка').value.boolean_value = True
    org.feature.add(id='pickup', name='самовывоз').value.boolean_value = True

    org.feature.add(id='shop_types', name='виды одежды').value.enum_value.extend([
        EnumItem(id='men_clothing_shop', name='мужская одежда'),
        EnumItem(id='women_clothing_shop', name='женская одежда')
    ])
    org.snippet.feature_ref.extend(['shop_types', 'pickup'])

    # properties
    org.properties.item.add(key='level', value='1')

    # profile
    org.profile.description = "Falconeri - это волшебное сочетание тончайших натуральных волокон, итальянских ремесленных навыков и инновационных производственных систем."

    validator.validate_example(geo_object, 'business')
