from smarttv.alice.tv_proxy.proxy.management.commands.delete_outdated_documents import get_outdated

from collections import namedtuple
import random
from time import time

test_url = namedtuple('test_url', ['uri', 'delete'])
random.seed(time())


def generate_sample_table(urls):
    return [{'key': '000000', 'subkey': url.uri, 'value': ''} for url in urls]


def generate_answer(urls):
    return [url.uri for url in urls if url.delete]


dirty_urls = [
    # Урлы страшные и непонятные
    test_url(uri='du://aa?aaaaa&device_id=1&aaaaa&ts=15&bbb', delete=False),  # device_id=1
    test_url(uri='du://a/a?ts=14&device_id=1&dd', delete=True),
    test_url(uri='du://aa/aa/aaa?y=22&ts=14&device_id=1&x=1&id=2', delete=True),
    test_url(uri='du://a/b/c/d?device_id=1&dev_id=11&ts=5&junk', delete=True),

    test_url(uri='du://aa?ts=11&ddd=fff&device_id=2', delete=False),  # другие device_id

    test_url(uri='du://aaa?device_id=d2&ts=10&tsnot=100', delete=False)
]

few_deletions = [
    # Удалить нужно немного записей
    test_url(uri='fd://fd?device_id=x&ts=14013', delete=False),  # device_id=x
    test_url(uri='fd://fd?device_id=x&ts=1', delete=True),

    test_url(uri='fd://fd?device_id=a&ts=102', delete=False),  # device_id=a
    test_url(uri='fd://fd?&device_id=a&ts=101', delete=True),
    test_url(uri='fd://fd?ts=100&device_id=a', delete=True),

    test_url(uri='fd://fd?ts=1010910910901', delete=False),  # пропущено device_id

    test_url(uri='fd://fd?device_id=dvffv', delete=False),  # пропущено ts

    test_url(uri='fd://fd?device_id=11&ts=11', delete=False)  # device_id=11
]

many_deletions = [
    # Удалить нужно много записей
    test_url(uri='md://a?device_id=d1&ts=15', delete=False),  # device_id=d1
    test_url(uri='md://a?device_id=d1&ts=15&id=2', delete=False),  # две одинаковых ts
    test_url(uri='md://a?ts=14&device_id=d1', delete=True),
    test_url(uri='md://a?ts=14&device_id=d1&id=2', delete=True),
    test_url(uri='md://a?device_id=d1&ts=0', delete=True),
    test_url(uri='md://a?device_id=d1&ts=0&id=2', delete=True),

    test_url(uri='md://a?ts=11&device_id=d2', delete=False),  # device_id=d2
    test_url(uri='md://a?device_id=d2&ts=5', delete=True),
    test_url(uri='md://a?ts=10&device_id=d2', delete=True),
]

unique_device_ids = [
    # Все device_id разные
    test_url(uri='udi://path?device_id=a&ts=1', delete=False),

    test_url(uri='udi://path?ts=2&device_id=b', delete=False),

    test_url(uri='udi://path?device_id=c&ts=3', delete=False),

    test_url(uri='udi://path?ts=4&device_id=d', delete=False)
]

equal_timestamps = [
    # Много одинаковых ts у одинаковых device_id
    test_url(uri='ets://e?ts=101&device_id=a635ed', delete=False),  # device_id=a635ed
    test_url(uri='ets://e?ts=101&device_id=a635ed&id=2', delete=False),  # две одинаковые ts
    test_url(uri='ets://e?ts=100&device_id=a635ed', delete=True),

    test_url(uri='ets://e?device_id=a635e&ts=100', delete=False),  # device_id=a635e
    test_url(uri='ets://e?ts=100&id=2&device_id=a635e', delete=False),  # две одинаковые ts
    test_url(uri='ets://e?device_id=a635e&ts=99', delete=True),

    test_url(uri='ets://e?ts=99&device_id=a635', delete=False)  # device_id=a635
]

all_urls_skipped = [
    # Во всех урлах чего-нибудь не хватает или неправильный формат
    test_url(uri='aus://path?completely_nothing', delete=False),  # Ничего нет

    test_url(uri='aus://path?device_id=0&t_s=1', delete=False),  # Нет ts

    test_url(uri='aus://path?ts=111&deviceid=e5r', delete=False),  # Нет device_id

    test_url(uri='aus://path?devce_id=1&ts=15', delete=False),  # Нет device_id

    test_url(uri='aus://path?device_id=10a&ts=ddd', delete=False)  # ts не число
]

different_arguments_order_1 = [
    # Аргументы в урлах сильно перемешаны
    test_url(uri='dao://path?ts=50&device_id=asd', delete=False),  # device_id=asd
    test_url(uri='dao://path?device_id=asd&ts=40', delete=True),
    test_url(uri='dao://path?device_id=asd&ts=10', delete=True),
    test_url(uri='dao://path?ts=5&device_id=asd', delete=True),
    test_url(uri='dao://path?device_id=asd&ts=1', delete=True)
]

different_arguments_order_2 = [
    # То же самое, что и в different_arguments_order_1, но аргументы перемешаны в другом порядке
    test_url(uri='dao://path?device_id=asd&ts=50', delete=False),  # device_id=asd
    test_url(uri='dao://path?ts=40&device_id=asd', delete=True),
    test_url(uri='dao://path?device_id=asd&ts=10', delete=True),
    test_url(uri='dao://path?device_id=asd&ts=5', delete=True),
    test_url(uri='dao://path?ts=1&device_id=asd', delete=True),
]


def check(data):
    random.shuffle(data)

    prepared_data = generate_sample_table(data)

    program_answer = get_outdated(prepared_data)
    correct_answer = generate_answer(data)

    assert program_answer == correct_answer, '''Test data is: {};\n
                                             Program answer is {};\n
                                             Correct answer is {}'''.format(data, program_answer, correct_answer)


class TestOutdated(object):
    def test_few_deletions(self):
        check(few_deletions)

    def test_many_deletions(self):
        check(many_deletions)

    def test_empty_list(self):
        check([])

    def test_unique_urls(self):
        check(unique_device_ids)

    def test_equal_timestamps(self):
        check(equal_timestamps)

    def test_all_urls_skipped(self):
        check(all_urls_skipped)

    def test_different_arguments_order(self):
        check(different_arguments_order_1)
        check(different_arguments_order_2)

    def test_dirty_urls(self):
        check(dirty_urls)
