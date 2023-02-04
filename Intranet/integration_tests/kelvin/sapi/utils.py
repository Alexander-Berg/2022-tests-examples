import collections
import logging

from django.db import models

log = logging.getLogger('utils')


def assert_contains_value(sequence, value, key):
    """
    Ассерт на то что последовательность содержит значение в указанном ключе
    """
    assert any(item.get(key) == value for item in sequence)


def assert_not_contains_value(sequence, value, key):
    """
    Ассерт на то что последовательность не содержит значение в указанном ключе
    """
    assert not any(item.get(key) == value for item in sequence)


def assert_sirius_pagination(obj):
    """
    Результат содержит данные пагинации
    """
    assert 'count' in obj
    assert 'limit' in obj
    assert 'page' in obj
    assert 'results' in obj


def get_response(client, url):
    """
    Делает GET-запрос на переданный url и возвращает
    ответ и json тело ответа
    """
    response = client.get(url)
    log.debug('GET - %s - "%s" - %s',
              response.status_code, url, len(response.content))
    return response, response.json()


def obj_factory(model, data):
    """
    Генерирует объекты из модели и словаря или списка с данными
    """
    if not issubclass(model, models.Model):
        raise ValueError('model must be models.Model instance')

    log.debug('Making "%s" object', model.__name__)

    if isinstance(data, dict):
        return model.objects.create(**data)

    elif isinstance(data, collections.Iterable):
        objects = []

        for item in data:
            objects.append(obj_factory(model, item))

        return objects

    else:
        raise ValueError('data must be sequence or dict')
