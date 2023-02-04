from unittest import mock
import pytest
from marshmallow import ValidationError
from startrek_client import Startrek

from billing.apikeys.apikeys.service_config import OAuthServiceConfig
from billing.apikeys.apikeys.startrek_wrapper.exceptions import (
    ApikeysStartrekServerError,
    ApikeysStartrekClientError,
)
from billing.apikeys.apikeys.startrek_wrapper.startrek import (
    CustomStartrek,
    ServiceStartrekClient,
    StartrekCollectionElementCheckerMixin,
)


@pytest.fixture
def service_with_startrek_config(service_fabric, st_config_user_observer):
    service = service_fabric()
    service.startrek_config = st_config_user_observer
    service.save()
    return service


def test_custom_startrek_create(mongomock):
    """Создание экземпляра CustomStartrek через OAuthServiceConfig."""
    connect_config = OAuthServiceConfig('apikeys_service', url='url', token='token')
    cs = CustomStartrek(connect_config)
    assert isinstance(cs, Startrek)


def test_service_startrek_client_create_with_empty_st_config(mongomock, simple_service, st_client):
    """У сервиса не заполнен startrek_config."""
    with pytest.raises(ApikeysStartrekClientError, match='Service is not startrekable'):
        ServiceStartrekClient(simple_service, startrek_client=st_client)


def test_service_startrek_client_create_with_invalid_st_config(mongomock, service_with_startrek_config, st_client):
    """Невалидный startrek_config."""
    def fake_validate_config(self, client, config):
        raise ApikeysStartrekServerError('Bad service configuration')

    with mock.patch.object(ServiceStartrekClient, '_validate_st_config', fake_validate_config):
        with pytest.raises(ApikeysStartrekServerError, match='Bad service configuration'):
            ServiceStartrekClient(service_with_startrek_config, startrek_client=st_client)


def test_service_startrek_client_create_with_valid_st_config(mongomock, service_with_startrek_config, st_client):
    """Успешное создание ServiceStartrekClient."""
    with mock.patch.object(ServiceStartrekClient, '_validate_st_config', lambda *args: None):
        ssc = ServiceStartrekClient(service_with_startrek_config, startrek_client=st_client)
    assert ssc._queue_name == service_with_startrek_config.startrek_config.queue \
           and ssc._observer == service_with_startrek_config.startrek_config.observer \
           and ssc._service_cc == service_with_startrek_config.cc


def test_service_startrek_client_is_user_observer_false(mongomock, simple_service, st_client, st_config_maillist_observer):
    """Наблюдатель - не пользователь."""
    simple_service.startrek_config = st_config_maillist_observer
    simple_service.save()

    with mock.patch.object(ServiceStartrekClient, '_validate_st_config', lambda *args: None):
        ssc = ServiceStartrekClient(simple_service, startrek_client=st_client)

    assert ssc._is_user_observer is False


def test_service_startrek_client_is_user_observer_true(mongomock, service_with_startrek_config, st_client):
    """Наблюдатель - пользователь."""
    st_client.users.add(service_with_startrek_config.startrek_config.observer)

    with mock.patch.object(ServiceStartrekClient, '_validate_st_config', lambda *args: None):
        ssc = ServiceStartrekClient(service_with_startrek_config, startrek_client=st_client)

    assert ssc._is_user_observer and '@' not in ssc._observer


def test_element_checker_mixin_check_element_not_found(mongomock, st_client):
    """Элемента нет в коллекции."""
    assert StartrekCollectionElementCheckerMixin._check_element(st_client, 'QUEUE', 'queues') is False


def test_element_checker_mixin_check_element_found(mongomock, st_client):
    """Элемент присутствует в коллекции."""
    queue_key = 'QUEUE'
    st_client.queues.add(queue_key)
    assert StartrekCollectionElementCheckerMixin._check_element(st_client, queue_key, 'queues') is True


def test_validate_st_config_with_invalid_queue(mongomock, st_client, st_config_user_observer):
    """Очереди нет в Стартреке."""
    with pytest.raises(ValidationError, match='Queue not found'):
        ServiceStartrekClient._validate_st_config(st_client, st_config_user_observer)


def test_validate_st_config_with_valid_queue_invalid_user_observer(mongomock, st_client, st_config_user_observer):
    """Наблюдателя (пользователя) нет в Стартреке."""
    st_client.queues.add(st_config_user_observer.queue, None)

    with pytest.raises(ValidationError, match='User not found'):
        ServiceStartrekClient._validate_st_config(st_client, st_config_user_observer)


def test_validate_st_config_with_valid_queue_valid_user_observer(mongomock, st_client, st_config_user_observer):
    """Очередь и наблюдатель (пользователь) есть в Стартреке."""
    st_client.queues.add(st_config_user_observer.queue, None)
    st_client.users.add(st_config_user_observer.observer, None)

    ServiceStartrekClient._validate_st_config(st_client, st_config_user_observer)


def test_validate_st_config_with_valid_queue_invalid_maillist_observer(mongomock, st_client,
                                                                       st_config_maillist_observer):
    """Наблюдателя (рассылки) нет в Стартреке."""
    st_client.queues.add(st_config_maillist_observer.queue, None)

    with pytest.raises(ValidationError, match='Maillist not found'):
        ServiceStartrekClient._validate_st_config(st_client, st_config_maillist_observer)


def test_validate_st_config_with_valid_queue_valid_maillist_observer(mongomock, st_client,
                                                                     st_config_maillist_observer):
    """Очередь и наблюдатель (рассылка) есть в Стартреке."""
    st_client.queues.add(st_config_maillist_observer.queue, None)
    st_client.maillists.add(st_config_maillist_observer.observer, None)

    ServiceStartrekClient._validate_st_config(st_client, st_config_maillist_observer)
