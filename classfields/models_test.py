import random

import pytest

from tree.models import Settings


@pytest.fixture
def test_setting(db):
    return Settings.objects.create(key="test", value=[random.randint(0, 100)])


@pytest.mark.django_db
def test_get_item(test_setting: Settings):
    result = Settings[test_setting.key]
    assert result == test_setting.value
    result = Settings.get(test_setting.key)
    assert result == test_setting.value


@pytest.mark.xfail(raises=KeyError)
@pytest.mark.django_db
def test_no_item(test_setting: Settings):
    Settings[f"{test_setting.key} {random.randint(10000, 99999)}"]


@pytest.mark.django_db
def test_set_item_updating(test_setting: Settings):
    key = test_setting.key
    wanted = True
    Settings[key] = wanted
    assert Settings[key] == wanted


@pytest.mark.django_db
def test_set_item_creating():
    wanted = str(random.randint(100, 999))
    some_value = True
    # first test given key does not exists yet
    assert Settings.get(wanted) is None

    Settings[wanted] = some_value
    assert Settings[wanted] == some_value


@pytest.mark.django_db
def test_in_lookup_working(test_setting: Settings):
    assert test_setting.key in Settings
    assert f"{test_setting.key}_nonexistent" not in Settings
