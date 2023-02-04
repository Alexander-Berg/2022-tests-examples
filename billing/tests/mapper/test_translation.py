from unittest import mock
import pytest
import mongoengine as me

from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys.mapper.translation import NeedUniqueFieldError, NotExistingFieldError
from billing.apikeys.apikeys.cacher import LFUCache


def test_translations_model_create(mongomock):
    # не переданы поля для перевода
    with pytest.raises(me.errors.ValidationError):
        mapper.Translation.create(mapper.Service, 'cc', [])

    # несуществующее поле в качестве поля для перевода
    with pytest.raises(NotExistingFieldError):
        mapper.Translation.create(mapper.Service, 'cc', 'names')

    # некорректное название вложенного поля для перевода, т.к. поля 'manager' у BalanceContractConfig нет
    with pytest.raises(NotExistingFieldError):
        mapper.Translation.create(mapper.Service, 'id', 'balance_config.contract_config.manager')

    # поле, переданное в качестве поля-ключа, не является уникальным
    with pytest.raises(NeedUniqueFieldError):
        mapper.Translation.create(mapper.Service, 'name', 'name')

    # корректное название поля
    t = mapper.Translation.create(mapper.Service, 'cc', 'name')
    assert t.fields == ['name']
    t.delete()

    # несколько корректных полей
    t = mapper.Translation.create(mapper.Service, 'cc', 'name, token')
    assert t.fields == ['name', 'token']


def test_translations_model_add(mongomock):
    t = mapper.Translation.create(mapper.Service, 'cc', 'name')

    # корректное название поля
    t.add('token')
    assert set(t.fields) == {'name', 'token'}


def test_translations_model_remove(mongomock):
    t = mapper.Translation.create(mapper.Service, 'cc', 'name, token')

    # удаление одного из нескольких поля
    t.remove('name')
    assert t.fields == ['token']

    # удаление последнего поля (приводит к удалению объекта Translation)
    t.remove('token')
    assert not mapper.Translation.objects


class FakeTankerTranslations:

    _translations = LFUCache()

    @classmethod
    def add_translations(cls, obj, field_name, new_translations):
        translations = cls._translations.find('keysets') or {}

        keyset = mapper.Translation.tanker_keyset_name(obj._class_name, field_name)
        key = mapper.Translation.tanker_key(obj)
        if keyset not in translations:
            translations[keyset] = {'keys': {}}
        key_data = translations[keyset]['keys'].get(key, {})
        tr = key_data.get('translations', {})
        for lang, value in new_translations.items():
            if lang not in tr:
                tr[lang] = {'form': value}
        key_data['translations'] = tr
        translations[keyset]['keys'][key] = key_data

        cls._translations.put('keysets', translations)


def test_tanker_translations_get_translation(mongomock, empty_tariff):
    mapper.Translation.create(mapper.Tariff, 'cc', 'name')
    FakeTankerTranslations.add_translations(empty_tariff, 'name', {'ru': empty_tariff.name})

    with mock.patch('billing.apikeys.apikeys.mapper.TankerTranslations._translations',
                    new=FakeTankerTranslations._translations):
        with pytest.raises(KeyError):
            mapper.TankerTranslations.get_translation(empty_tariff, 'name', 'en')

        en_translation = 'en translation'
        FakeTankerTranslations.add_translations(empty_tariff, 'name', {'en': en_translation})
        assert mapper.TankerTranslations.get_translation(empty_tariff, 'name', 'en') == en_translation
