from datetime import datetime

from factory import Faker, SubFactory
from factory.django import DjangoModelFactory

from intranet.hrdb_ext.src.amo.models import Ticket, TicketRequest


class FakeStatus:
    def __init__(self, key):
        self.key = key


class FakeIssue:
    def __init__(self, key=None, status_key=None):
        self.key = key
        self.status = FakeStatus(status_key)
        self.createdAt = datetime.now()
        self.updatedAt = datetime.now()
        self.statusStartTime = datetime.now()


class TicketFactory(DjangoModelFactory):
    key = Faker('sentence', nb_words=1)
    status = Faker('sentence', nb_words=1)

    class Meta:
        model = Ticket


class TicketRequestFactory(DjangoModelFactory):
    uuid = Faker('uuid4')
    external_id = Faker('sentence', nb_words=1)
    ticket = SubFactory(TicketFactory)

    class Meta:
        model = TicketRequest
