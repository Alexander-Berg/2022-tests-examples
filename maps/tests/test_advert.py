from maps.doc.proto.testhelper.fakers import AvatarFaker
from maps.doc.proto.testhelper.common import RUR
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.common2 import i18n_pb2, image_pb2, keyvalue_pb2
from yandex.maps.proto.search import advert_pb2
from yandex.maps.proto.search.advert_pb2 import Action, Banner, AdvertLink, Promo, Product, TextData

tycoon = AvatarFaker('tycoon', seed=2648)
validator = Validator('search')


def make_product(title, price, url, disclaimer=[]):
    return Product(
        title=title,
        url=url,
        photo=advert_pb2.Image(url=tycoon(size='product')),
        price=i18n_pb2.Money(value=price.value, text=price.text, currency=price.currency),
        disclaimer=disclaimer)


def make_image(suffix, tags):
    return image_pb2.Image(url_template=tycoon(size=f'{suffix}_%s'), tag=tags)


def make_icon(suffix, tags, anchor):
    return image_pb2.Icon(
        image=make_image(suffix, tags),
        anchor=image_pb2.ImagePoint(x=anchor[0], y=anchor[1]))


def props(**kw):
    return [keyvalue_pb2.KeyValuePair(key=k, value=v) for (k, v) in kw.items()]


def test_advert():
    description = (
        'Высокое качество услуг при доступных ценах - принципиальная позиция барбершопов BRITVA. '
        'С материнским энтузиазмом мы заботимся о клиентах и с их помощью становимся лучше. '
        'Наша цель - гармоничный тандем барбершопа и гостя, где каждый будет счастлив. '
        'Основатели сети BRITVA — ведущие специалисты центральных барбершопов Москвы. '
        'Сообразив, что мужские цирюльни не предлагают качественных услуг в спальных районах, '
        'команда профессионалов создала за пределами центра барбершопы, доступные каждому. '
        'Доступные, как территориально, так и по цене. '
        'Профессионализм, демократия и экономия вашего времени — три кита, на которых стоит BRITVA'
    )

    ad = advert_pb2.Advert(
        about=description,
        log_id='2599204',
        highlighted=True,
        property=props(advOrderId='2599204'),

        text_data=TextData(
            title='Сеть Барбершопов Britva',
            text='Мужские стрижки для крепких духом! Качественная стрижка в Вашем районе.',
            url='http://britvabarber.ru/o-nas/',
            disclaimer=['Посоветуйтесь с врачом']
        ),

        promo=Promo(
            title="Cтрижка happy hours за 850 рублей!",
            details="С понедельника по четверг с 12.00 до 15.00 стрижем всего за 850 руб, вместо 1200 руб.",
            disclaimer=["Кроме праздничных дней"],
            url="http://britvabarber.ru/category/aktsii/",
            banner=advert_pb2.Image(url=tycoon(size='banner')),
            full_disclaimer=' '.join([
                'Акция действительна с понедельника по четверг с 12.00 до 15.00.',
                'Во время записи произнесите кодовую фразу “хочу счастливые часы”.'
            ])
        ),

        product=[
            make_product('Детская стрижка', RUR(800), url='http://britvabarber.ru/place/tulskaya/',
                         disclaimer=['Для детей старше трех лет']),
            make_product('Камуфляж бороды', RUR(500), url='http://britvabarber.ru/place/tulskaya/'),
            make_product('Королевское бритье', RUR(1200), url='http://britvabarber.ru/place/tulskaya/'),
            make_product('Креативная стрижка', RUR(1200), url='http://britvabarber.ru/place/tulskaya/',
                         disclaimer=['Креатив законодательно ограничен'])
        ],
        banner=Banner(
            image=advert_pb2.Image(url=tycoon(size='banner')),
            link=[AdvertLink(uri='http://britvabarber.ru/discount-lottery/signup.html')],
            disclaimer='Условия акции уточняйте у мастера-распорядителя'
        ),

        image=[
            make_image('logo', tags=['logo']),
            make_image('banner', tags=['balloonBanner'])
        ],
        icon=[
            make_icon('drop', tags=['icon'], anchor=(0.5, 0.875)),
            make_icon('dust', tags=['dust'], anchor=(0.5, 0.5)),
            make_icon('pin', tags=['selected'], anchor=(0.5, 0.96875))
        ],
        action=[
            Action(type='Call', property=props(title='Позвонить', value='8 (800) 250-54-51'))
        ]
    )

    validator.validate_example(ad, 'advert')
