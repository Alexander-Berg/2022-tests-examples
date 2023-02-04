from maps.doc.proto.testhelper.validator import Validator

from yandex.maps.proto.inapp.common_pb2 import Payload
from yandex.maps.proto.inapp.backoffice_pb2 import SendData, Recipient

validator = Validator('inapp')


def test_send():
    data = SendData(
        payload=Payload(
            title='Есть новые задания',
            text='Помогите нам сделать Яндекс.Карты точнее — выполните новые задания!',
            uri='https://lk.maps.yandex.ru/#!/feedback/tasks'
        ),
        recipient=[Recipient(uid="2721238324"), Recipient(uid="12500670")]
    )

    validator.validate_example(data, 'backoffice')
