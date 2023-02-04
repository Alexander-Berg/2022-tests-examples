# -*- coding: utf-8 -*-

import pytest

from balance.mapper import Exportable
from tests.object_builder import PassportBuilder, ContractBuilder

TEST_QUEUES = {
    PassportBuilder._class: 'TAKEOUT',
    ContractBuilder._class: 'BY_FILE',
}


def get_test_queue_of_instance(instance):
    return TEST_QUEUES[type(instance)]


@pytest.fixture(
    scope='session',
    params=[
        ContractBuilder,
        PassportBuilder,
    ],
)
def exportable_class_builder(request):
    assert issubclass(request.param._class, Exportable), \
        "%s does not build a subclass of Exportable" % request.param.__name__
    return request.param


@pytest.fixture
def exportable_instance(exportable_class_builder, session):
    instance = exportable_class_builder.construct(session)

    # В бд название очереди - это partition key
    # Поэтому надо эскпортировать в существующую очередь
    # чтобы попать наверняка - будем эскпортировать в правильную очередь для типа
    test_queue_name = get_test_queue_of_instance(instance)
    assert test_queue_name in instance.get_export_types(), \
        'Queue "%s" is not valid for instances of %s' % (test_queue_name, type(instance))

    # очистить экспорты от созданных заранее записей для чистого эксперимента
    instance.exports = {}
    return instance


@pytest.fixture
def selected_exportable_instance(exportable_instance, session):
    session.flush()
    session.expire_all()
    return exportable_instance


@pytest.fixture
def selected_instance_with_export_entry(exportable_instance, session):
    test_queue_name = get_test_queue_of_instance(exportable_instance)
    exportable_instance.enqueue(test_queue_name)
    session.flush()
    session.expire_all()
    return exportable_instance
