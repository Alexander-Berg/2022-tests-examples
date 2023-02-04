import pytest
from mock import patch, Mock
from datetime import datetime
from django.conf import settings
from smarttv.droideka.proxy import cache

from smarttv.droideka.proxy.models import SharedPreferences

GET_CACHE_METHOD = 'django.core.cache.cache'

SHARED_PREF_CLASS = 'smarttv.droideka.proxy.models.SharedPreferences'
SHARED_PREF_SAVE_METHOD = 'smarttv.droideka.proxy.models.SharedPreferences._save_in_cache'

CACHE_CLASS = settings.CACHE_BACKEND_CLASS
CACHE_SET_METHOD = f'{CACHE_CLASS}.set'
CACHE_DELETE_METHOD = f'{CACHE_CLASS}.delete'
CACHE_GET_METHOD = f'{CACHE_CLASS}.get'


class TestInt:

    def __init__(self, value):
        self.val = value

    @property
    def key(self):
        return 'test_int'

    @property
    def value(self):
        return self.val

    @property
    def type_cast(self):
        return int

    def get_kw_args(self):
        return {
            'key': self.key,
            'int_value': self.value,
        }


class TestBool:

    def __init__(self, value):
        self.val = value

    @property
    def key(self):
        return 'test_bool'

    @property
    def value(self):
        return self.val

    @property
    def type_cast(self):
        return bool

    def get_kw_args(self):
        return {
            'key': self.key,
            'bool_value': self.value,
        }


class TestChar:

    def __init__(self, value):
        self.val = value

    @property
    def key(self):
        return 'test_bool'

    @property
    def value(self):
        return self.val

    @property
    def type_cast(self):
        return str

    def get_kw_args(self):
        return {
            'key': self.key,
            'char_value': self.value,
        }


def _get_db_assert_params(key=None, int_value=None, bool_value=None, char_value='', datetime_value=None):
    return {
        'key': key,
        'int_value': int_value,
        'bool_value': bool_value,
        'char_value': char_value,
        'datetime_value': datetime_value
    }


def _get_cache_set_assert_params(key=None, int_value=None, bool_value=None, char_value=None):
    if int_value is not None:
        value = int_value
    elif bool_value is not None:
        value = bool_value
    else:
        value = char_value
    return {
        'key': key,
        'value': str(value)
    }


NON_EMPTY_TEST_PREFS = [
    TestInt(123),
    TestInt(0),
    TestBool(True),
    TestBool(False),
    TestChar('abcd'),
]

EMPTY_TEST_PREFS = [
    TestInt(None),
    TestBool(None)
]

TEST_PREFS = NON_EMPTY_TEST_PREFS + EMPTY_TEST_PREFS

TEST_KEY = 'some key'
TEST_DATE = datetime(2020, 5, 22, 21, 39, 40)
TIME_STAMP = int(TEST_DATE.timestamp())


def get_any_value(model):
    if model.int_value is not None:
        return model.int_value
    if model.bool_value is not None:
        return model.bool_value
    if model.char_value:
        return model.char_value


@pytest.fixture(autouse=True)
def clean_db():
    yield
    SharedPreferences.objects.all().delete()
    cache.default.delete(TEST_KEY)


@pytest.mark.django_db
@pytest.mark.parametrize('value_provider', TEST_PREFS)
@patch(SHARED_PREF_SAVE_METHOD)
def test_save_to_db_with_correct_params(shared_pref_save_cache_method, value_provider):
    SharedPreferences(**value_provider.get_kw_args()).save()
    shared_pref_save_cache_method.assert_called_with(**_get_db_assert_params(**value_provider.get_kw_args()))


@pytest.mark.django_db
@pytest.mark.parametrize('value_provider', NON_EMPTY_TEST_PREFS)
@patch(CACHE_SET_METHOD)
def test_save_to_cache_correct_params(save_method, value_provider):
    save_method.side_effect = None
    SharedPreferences(**value_provider.get_kw_args()).save()
    save_method.assert_called_once_with(**_get_cache_set_assert_params(**value_provider.get_kw_args()))


@pytest.mark.django_db
@pytest.mark.parametrize('value_provider', EMPTY_TEST_PREFS)
@patch(CACHE_DELETE_METHOD)
@patch(CACHE_SET_METHOD)
@patch(SHARED_PREF_CLASS)
def test_try_to_save_none_values(shared_pref_mock, set_method, delete_method, value_provider):
    delete_method.side_effect = None
    set_method.side_effect = None
    SharedPreferences(**value_provider.get_kw_args()).save()
    set_method.assert_not_called()
    delete_method.assert_called_once_with(value_provider.key)


@pytest.mark.django_db
@pytest.mark.parametrize('value_provider', TEST_PREFS)
def test_save_to_db(value_provider):
    assert len(SharedPreferences.objects.all()) == 0

    SharedPreferences(**value_provider.get_kw_args()).save()

    assert len(SharedPreferences.objects.all()) == 1
    saved_obj = SharedPreferences.objects.first()
    assert saved_obj.key == value_provider.key
    assert get_any_value(saved_obj) == value_provider.value


@pytest.mark.django_db
def test_save_to_db_empty_string():
    value_provider = TestChar('')
    assert len(SharedPreferences.objects.all()) == 0

    SharedPreferences(**value_provider.get_kw_args()).save()

    assert len(SharedPreferences.objects.all()) == 1
    saved_obj = SharedPreferences.objects.first()
    assert saved_obj.key == value_provider.key
    assert saved_obj.char_value == value_provider.value


@pytest.mark.parametrize('value_provider', TEST_PREFS)
@pytest.mark.django_db
def test_read_from_db(value_provider):
    assert SharedPreferences.get_pref(value_provider.key, type_cast=value_provider.type_cast) is None

    SharedPreferences(**value_provider.get_kw_args()).save()

    assert SharedPreferences.get_pref(
        key=value_provider.key, type_cast=value_provider.type_cast) == value_provider.value


@pytest.mark.django_db
@patch(CACHE_SET_METHOD)
def test_save_date_time_string_representation_saved(redis_save_method):
    redis_save_method.side_effect = None

    SharedPreferences.put_datetime(TEST_KEY, TEST_DATE)

    redis_save_method.assert_called_with(key=TEST_KEY, value=SharedPreferences.DATETIME_PREFIX + str(TIME_STAMP))


@pytest.mark.django_db
@patch(CACHE_GET_METHOD, Mock(return_value=f'{SharedPreferences.DATETIME_PREFIX}{TIME_STAMP}'))
def test_get_datetime_from_redis():
    result_date = SharedPreferences.get_datetime('any key')

    assert result_date == TEST_DATE


@pytest.mark.django_db
def test_get_from_db():
    assert len(SharedPreferences.objects.all()) == 0
    SharedPreferences.put_datetime(TEST_KEY, TEST_DATE)

    assert len(SharedPreferences.objects.all()) == 1
    assert SharedPreferences.get_datetime(TEST_KEY) == TEST_DATE
