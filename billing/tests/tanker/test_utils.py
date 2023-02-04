import pytest

from billing.apikeys.apikeys.mapper.translation import NotExistingFieldError
from billing.apikeys.apikeys.tanker.utils import TranslationUploader


def test_translation_uploader_getattr(mongomock, simple_service, balance_config):
    # пустое поле первого уровня (пустое строковое значение ничем не заменяется)
    simple_service.token = ''
    assert TranslationUploader._getattr(simple_service, 'token') == simple_service.token

    # несуществующее для модели поле (первый уровень)
    with pytest.raises(NotExistingFieldError):
        TranslationUploader._getattr(simple_service, 'not_existing_field')

    # несуществующее для модели поле (второй уровень)
    with pytest.raises(NotExistingFieldError):
        TranslationUploader._getattr(simple_service, 'balance_config.not_existing_field')

    # существующее для модели, но не существующее для объекта поле
    assert TranslationUploader._getattr(simple_service, 'balance_config.contract_config') is None

    # заполненное поле первого уровня
    assert TranslationUploader._getattr(simple_service, 'name') == simple_service.name

    # заполненное поле третьего уровня
    simple_service.balance_config = balance_config
    assert TranslationUploader._getattr(simple_service, 'balance_config.contract_config.firm_id') == 1

    # несуществующее поле третьего уровня
    with pytest.raises(NotExistingFieldError):
        TranslationUploader._getattr(simple_service, 'balance_config.contract_config.not_existing_field')
