import pytest

from intranet.femida.src.notifications import communications as n
from intranet.femida.src.communications.choices import MESSAGE_TYPES

from intranet.femida.tests import factories as f


message_data = [
    (MESSAGE_TYPES.internal, n.InternalMessageCreatedNotification),
    (MESSAGE_TYPES.outcoming, n.ExternalMessageCreatedNotification),
    (MESSAGE_TYPES.note, n.NoteCreatedNotification),
]


@pytest.mark.parametrize('data', message_data)
def test_message_created(data):
    message_type, notification_class = data
    instance = f.MessageFactory(type=message_type)
    instance.html = 'Текст комментария'
    f.MessageAttachmentFactory.create_batch(3, message=instance)
    notification = notification_class(instance)
    notification.send()
