import factory
import pytest

from watcher import enums
from watcher.db import Member
from .base import MEMBER_SEQUENCE


@pytest.fixture(scope='function')
def member_factory(meta_base, service_factory, staff_factory, role_factory):
    class MemberFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Member

        id = factory.Sequence(lambda n: n + MEMBER_SEQUENCE)
        staff = factory.SubFactory(staff_factory)
        service = factory.SubFactory(service_factory)
        role = factory.SubFactory(role_factory)
        state = enums.MemberState.active

    return MemberFactory
