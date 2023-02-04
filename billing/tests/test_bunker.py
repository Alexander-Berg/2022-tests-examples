# -*- coding: utf-8 -*-
import mock
import hamcrest as hm

from balance.utils.ya_bunker import BunkerRepository, BunkerClient

TEST_OBJECT = {
    'title': 'Стандартный шаблон',
    'fields': [
        {
            'name': 'client_id',
            'type': 'integer',
            'title': 'ID_Client',
            'required': True,
            'values': []
        }
    ]
}

ACTIVE_REF = '/active'

TEST_REFS = [
    # не отображается, т.к в ответе игнорируем не json
    {'name': 'ignored-1',
     'mime': 'application/schema+json; charset=utf-8; schema=bunker',
     'fullName': '/ignored-1'},
    # не отображается, т.к отсутствует mime - как например у узла
    {'name': 'ignored-2',
     'fullName': '/ignored-2'},
    {'name': 'active',
     'mime': 'application/json; charset=utf-8; schema=\"bunker:/template#\"',
     'fullName': ACTIVE_REF},
]


class MockBunkerClient(object):
    @staticmethod
    def cat(*args, **kwargs):
        return TEST_OBJECT

    @staticmethod
    def ls(*args, **kwargs):
        return TEST_REFS


def test_bunker_repository_get():
    """
    Простой тест что метод работает
    """
    mock.patch.object(BunkerRepository, '_get_bunker_client', return_value=MockBunkerClient()).start()
    repository = BunkerRepository(app=None)
    res = repository.get('/test')
    hm.assert_that(res, hm.equal_to(TEST_OBJECT))


def test_bunker_repository_list():
    """
    Тест, проверяющий работу методов, возвращающих списки, с игнорированием "необъектов" (папок, удаленных и т.п)
    """
    mock.patch.object(BunkerRepository, '_get_bunker_client', return_value=MockBunkerClient()).start()
    repository = BunkerRepository(app=None)
    objects, refs = repository.list_with_references('/test')
    hm.assert_that(objects, hm.equal_to([TEST_OBJECT]))
    hm.assert_that(refs, hm.has_length(1))
    hm.assert_that(refs[0], hm.has_entry('fullName', ACTIVE_REF))

    objects = repository.list('/test')
    hm.assert_that(objects, hm.equal_to([TEST_OBJECT]))
