from dataclasses import dataclass

import pytest

from intranet.paste.src.coreapp.models import Code
from intranet.paste.src.test_utils import BetterRestClient
from django.contrib.auth import get_user_model
from model_mommy import mommy

User = get_user_model()


@pytest.fixture
def client():
    return BetterRestClient()


@pytest.fixture
def users():
    @dataclass
    class Users:
        thasonic: User
        robot_wiki: User

    return Users(thasonic=mommy.make(User, username='thasonic'), robot_wiki=mommy.make(User, username='robot_wiki'))


@pytest.fixture
def pastes(users):
    out = []
    for lang, txt in [
        ('python', 'print(123)'),
        ('python', 'exit(0)'),
        ('plain', 'some text here'),
        ('plain', 'another text here'),
        ('c#', 'boo hoo'),
    ]:
        t = Code(syntax=lang, author=users.thasonic)
        t.text = txt
        t.assign_uuid_and_save()
        out.append(t)

    return out
