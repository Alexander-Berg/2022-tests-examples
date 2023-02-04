import pytest
from freezegun import freeze_time

from intranet.audit.src.core import lookups
from intranet.audit.src.core import models
from intranet.audit.src.users.models import User


@pytest.fixture
def author(db):
    return User.objects.create(uid=1,
                               login='test',
                               first_name='name',
                               last_name='last_name',
                               )


@freeze_time('2017-07-21')
@pytest.fixture
def process_eng(db, author):
    return models.Process.objects.create(name='test',
                                         process_type=models.Process.TYPES.root,
                                         author=author,
                                         )


@pytest.fixture
def process_ru(db, author):
    return models.Process.objects.create(name='тест',
                                         process_type=models.Process.TYPES.root,
                                         author=author,
                                         )


@pytest.fixture
def subprocess_eng(db, author):
    return models.Process.objects.create(name='test',
                                         process_type=models.Process.TYPES.subprocess,
                                         author=author,
                                         )


@pytest.fixture
def deficiency(db, author):
    return models.Deficiency.objects.create(short_description='some test description',
                                            author=author,
                                            )


def test_case_insensitive_lookup_eng(process_eng):
    q = 'TEST', 'test', 'tESt',
    for param in q:
        resource = lookups.ProcessLookup()
        data = list(resource.get_query(param, request=None))
        assert len(data) == 1
        assert data[0].name == process_eng.name


def test_case_insensitive_lookup_ru(process_ru):
    q = 'ТЕСТ', 'тест', 'тЕСт',
    for param in q:
        resource = lookups.ProcessLookup()
        data = list(resource.get_query(param, request=None))
        assert len(data) == 1
        assert data[0].name == process_ru.name


def test_id_lookup(process_eng):
    q = str(process_eng.id)
    resource = lookups.ProcessLookup()
    data = list(resource.get_query(q, request=None))
    assert len(data) == 1
    assert data[0].name == process_eng.name


def test_root_lookup(process_eng):
    q = str(process_eng.id)
    resource = lookups.ProcessLookup()
    data = list(resource.get_query(q, request=None))
    assert len(data) == 1
    assert data[0].name == process_eng.name


def test_subprocess_lookup_fail(process_eng):
    q = str(process_eng.id)
    resource = lookups.SubProcessLookup()
    data = list(resource.get_query(q, request=None))
    assert len(data) == 0


def test_subprocess_lookup_success(subprocess_eng):
    q = str(subprocess_eng.id)
    resource = lookups.SubProcessLookup()
    data = list(resource.get_query(q, request=None))
    assert len(data) == 1
    assert data[0].name == subprocess_eng.name


def test_description_lookup(deficiency):
    q = 'test description'
    resource = lookups.DeficiencyLookup()
    data = list(resource.get_query(q, request=None))
    assert len(data) == 1
    assert data[0].short_description == deficiency.short_description
