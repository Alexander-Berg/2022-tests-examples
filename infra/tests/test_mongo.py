from enum import Enum
from mongoengine import Document

from sepelib.mongo.fields import EnumField


def test_mongo_field():
    class Enums(Enum):
        zero = 0
        one = 1

    class A(Document):
        e = EnumField(Enums, default=Enums.zero)

    a = A()
    assert a.e.value == 0
