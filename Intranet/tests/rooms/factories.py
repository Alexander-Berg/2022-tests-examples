import factory


class RoomFactory(factory.django.DjangoModelFactory):
    class Meta:
        model = 'rooms.Room'

    event = None
    name = factory.Sequence(lambda n: 'Room %s' % n)
    room_id = factory.Sequence(lambda n: '%s' % n)
    email = factory.Sequence(lambda n: 'conf_room_%s@yandex-team.ru' % n)
    office_id = '123'
    codec_ip = factory.Sequence(lambda n: '[2001:db8:0:0:0:0:1:%s]' % n)
    timezone = 'Europe/Moscow'
    language = 'ru'
