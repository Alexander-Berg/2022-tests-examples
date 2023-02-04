from maps.doc.proto.testhelper.fakers import AvatarFaker
from maps.doc.proto.testhelper.common import RUR, set_price, add_metadata
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.common2.geo_object_pb2 import GeoObject
from yandex.maps.proto.search import goods_register_pb2, goods_1x_pb2, goods_metadata_pb2


validator = Validator('search')
fake_eda_link = AvatarFaker('eda', group_id_size=7, seed=7616)
fake_pills_link = AvatarFaker('med-pills', group_id_size=6, seed=8493)


def add_goods(msg, name, price, photo_link=fake_eda_link, **kwargs):
    goods = msg.goods.add()
    goods.name = name
    set_price(goods, price)
    if photo_link:
        link = goods.link.add()
        link.uri = photo_link(size='450x300')
    for arg in kwargs:
        setattr(goods, arg, kwargs[arg])
    return goods


def test_goods_register():
    register = goods_register_pb2.GoodsRegister()
    register.tag.append('menu')

    c = register.goods_category.add()
    c.name = 'Закуски'
    add_goods(c, 'Картофель по-деревенски', price=RUR(200), unit='150 г', photo_link=None,
              description='С домашним имбирным майонезом (на 100 г: 244,5 ккал, белки - 3, жиры - 16,5, углеводы - 21)')
    add_goods(c, 'Большая булочка', price=RUR(81), description='Булочка для бургера с кунжутом', photo_link=None)
    add_goods(c, 'Говяжий язык', price=RUR(580), unit='285 г',
              description='Язык говяжий, листья салата романо, крем сливочный, тостовый хлеб, крем-бальзамик, соленый огурец')

    c = register.goods_category.add()
    c.name = 'Напитки'
    add_goods(c, '7 Up', price=RUR(120), unit='250 мл', photo_link=None)
    add_goods(c, 'Pepsi', price=RUR(120), unit='250 мл', photo_link=None)
    add_goods(c, 'Вода Evian', price=RUR(250), unit='330 мл', photo_link=None)

    validator.validate_example(register, 'goods_register')


def test_goods_snippet():
    snippet = goods_1x_pb2.GoodsSnippet()

    add_goods(snippet, 'Пицца "Пепперони"', price=RUR(499),
              description='Пицца "Пепперони" с острыми колбасками')
    add_goods(snippet, 'Пицца "Аль Помидорини"', price=RUR(539),
              description='Пицца "Аль Помидорини" томатный соус, сыр моцарелла, ветчина, вяленные томаты')
    add_goods(snippet, 'Пицца "Бьякини с пармой, ветчиной, страчателлой"', price=RUR(699),
              description='Пицца "Бьякини с пармой, ветчиной, страчателлой" рукколой и сыром моцарелла')
    add_goods(snippet, 'Пицца "Маргарита"', price=RUR(419),
              description='Пицца "Маргарита" томатный соус, сыр моцарелла')

    validator.validate_example(snippet, 'goods_snippet')


def test_goods_metadata():
    geo_object = GeoObject()

    metadata = add_metadata(
        geo_object,
        goods_metadata_pb2.GOODS_METADATA
    )
    goods = add_goods(
        metadata,
        name='Парацетамол, таблетки 500 мг',
        price=RUR(12.5),
        photo_link=fake_pills_link,
        id='drug-yandex-42',
        quantity=4
    )
    attribution = goods.attribution
    attribution.author.name = 'Яндекс.Здоровье'
    attribution.author.uri = 'https://health.yandex.ru'
    attribution.link.href = 'https://yandex.ru/health/pills/product/paracetamol-20775'

    validator.validate_example(geo_object, 'goods_metadata')
