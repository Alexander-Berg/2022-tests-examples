from maps.doc.proto.testhelper.validator import Validator

from yandex.maps.proto.inapp.common_pb2 import Payload
from yandex.maps.proto.inapp.service_pb2 import MessageList, InteractionList, READ, USED, UNREAD

validator = Validator('inapp')


def test_messages():
    reply = MessageList()

    reply.message.add(
        message_id='a8ca53170541462f',
        payload=Payload(
            title='Бронирование',
            text='Ваше бронирование подтверждено. Джонн Донн в 18:30'),
        state=UNREAD,
        revision=0)

    reply.message.add(
        message_id='524505192b0c658c',
        payload=Payload(
            title='Ваши правки опубликованы',
            text='Количество ваших изменений, попавших в это обновление — 1. Благодарим за участие!',
            uri='https://lk.maps.yandex.ru/#!/feedback/maps'),
        state=READ,
        revision=1)

    reply.message.add(
        message_id='524505192b0c658c',
        payload=Payload(
            title='Есть новые задания',
            text='Помогите нам сделать Яндекс.Карты точнее — выполните новые задания!',
            uri='https://lk.maps.yandex.ru/#!/feedback/tasks'),
        state=USED,
        revision=2)

    validator.validate_example(reply, 'messages')


def test_interactions():
    ints = InteractionList()

    ints.interaction.add(message_id='a8ca53170541462f', state=READ, revision=1)
    ints.interaction.add(message_id='524505192b0c658c', state=USED, revision=1)
    ints.interaction.add(message_id='1259707046c1cbba', state=UNREAD, revision=3)

    validator.validate_example(ints, 'interactions')
