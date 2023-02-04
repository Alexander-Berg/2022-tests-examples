from maps.doc.proto.testhelper.fakers import AvatarFaker
from maps.doc.proto.testhelper.common import add_property, Image, add_image
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.menu.menu_pb2 import MenuInfo

url_template = AvatarFaker('maps-menu', seed=22575)


def add_menu_item(msg, name, log_id, subtitle='', type_=None, props={}, images=[]):
    menu_item = msg.menu_item.add()
    menu_item.title = name
    if subtitle:
        menu_item.subtitle = subtitle
    menu_item.search_query = name
    menu_item.log_id = log_id
    if type_:
        menu_item.type.append(type_)

    for key, value in props.items():
        add_property(menu_item, key, value)

    for image in images:
        add_image(menu_item, image)


def test_menu():
    menu = MenuInfo()

    add_menu_item(menu, 'Еда', log_id='1000', props={'iconStyle': 'food'})
    add_menu_item(menu, 'Покупки', log_id='1001',
                  images=[Image(url_template('shopping_day_menu_icon'), ['day']),
                          Image(url_template('shopping_night_menu_icon'), ['night'])])
    add_menu_item(menu, 'Бургер Кинг', log_id='e4b82c06', type_="advert",
                  images=[Image(url_template('burgerking'))])
    add_menu_item(menu, 'Развлечения', log_id='1002', props={'iconStyle': 'entertainment'})
    add_menu_item(menu, 'Гостиницы', log_id='1003', props={'iconStyle': 'hotels'})
    add_menu_item(menu, 'Красота', log_id='1004', props={'iconStyle': 'beauty'})
    add_menu_item(menu, 'Здоровье', log_id='1005', props={'iconStyle': 'healthcare'})

    # special project
    add_menu_item(menu, 'Выборы 8 сентября', subtitle='Избирательные участки на карте',
                  type_='special', log_id='1006')

    Validator('menu').validate_example(menu, 'menu')
