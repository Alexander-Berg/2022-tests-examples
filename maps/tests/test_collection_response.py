from .collection_common import Author, Collection, set_collection, add_collection
from maps.doc.proto.testhelper.fakers import AvatarFaker
from maps.doc.proto.testhelper.common import Image, set_image
from maps.doc.proto.testhelper.validator import Validator
from yandex.maps.proto.search import collection_response_pb2


url_template_faker = AvatarFaker('discovery-int', seed=3177)
validator = Validator('search')


def get_image(name):
    return Image(url_template=url_template_faker(name))


def add_partner_link(msg, title, image_name, uri):
    link = msg.link.add()
    link.title = title
    set_image(link.image, get_image(image_name))
    link.uri = uri


def test_collection_response_metadata():
    metadata = collection_response_pb2.CollectionResponseMetadata()

    collection = metadata.collection
    set_collection(collection, Collection(
        id_='ponchiki',
        title='Где есть горячие пончики',
        description='Слово «пончик» даже звучит вкусно и соблазнительно...',
        image=get_image('main-photo'),
        rubric='Кафе',
        item_count=4,
        author=Author(
            name='KudaGo',
            description='Лучшее в городе — куда сходить сегодня, завтра, на выходных',
            favicon=get_image('author-icon'),
            uri='https://kudago.com/')
    ))

    partner_links = metadata.partner_links
    partner_links.title = 'Интересные места и события на KudaGo'
    add_partner_link(partner_links,
                     title='Бесплатная Москва: здесь всегда свободный вход',
                     image_name='partner-image-link-1',
                     uri='https://kudago.com/msk/list/besplatnaya-moskva')
    add_partner_link(partner_links,
                     title='Всё о ресторанах и гастрономической культуре в Москве',
                     image_name='partner-image-link-2',
                     uri='https://kudago.com/msk/city-food')

    add_collection(metadata.related_collections, Collection(
        id_='lutshie-panoramnye-restorany-moskvy',
        title='Рестораны с панорамным видом: выбор Localway',
        description='В мегаполисе своя романтика...',
        image=get_image('main-photo-2'),
        rubric='Рестораны',
        item_count=9))

    add_collection(metadata.related_collections, Collection(
        id_='gde-kupit-interesnye-aromaty-msk',
        title='Где покупать интересные ароматы в Москве',
        description='Кроме сетевых гигантов...',
        image=get_image('main-photo-3'),
        rubric='Магазины парфюмерии',
        item_count=8))

    validator.validate_example(metadata, 'collection_response')
