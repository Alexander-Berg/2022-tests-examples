import pytest
import lib.server as server

from data_types.order import Order, Car
from data_types.user import User, Phone


def test_get_user_phone(user):
    data = server.get_user_phone(user) >> 200
    assert data["phone_id"] == user.phones[0].id
    assert data["masked_phone"] == user.phones[0].masked_phone


def test_get_user_phone_not_russian(user):
    phones = [
        Phone(number="+91112223344"),
        Phone(number="+7121112223344"),
        Phone()
    ]
    user = User(phones=phones)
    user.register()

    data = server.get_user_phone(user) >> 200
    assert data["phone_id"] == user.phones[2].id
    assert data["masked_phone"] == user.phones[2].masked_phone


def test_user_phone_without_phone():
    user = User(phones=[])
    user.register()

    data = server.get_user_phone(user) >> 422
    assert data["code"] == "PHONE_NOT_FOUND"


def test_additional_phone():
    phones = [
        Phone(is_secured=False),
        Phone(is_secured=True)
    ]
    user = User(phones=phones)
    user.register()

    data = server.get_user_phone(user) >> 200
    assert data["phone_id"] == phones[1].id
    assert data["masked_phone"] == phones[1].masked_phone

    del user.phones[1]
    user.register()

    data = server.get_user_phone(user) >> 200
    assert data["phone_id"] == phones[0].id
    assert data["masked_phone"] == phones[0].masked_phone
