import math
import time
from builtins import object, range
from collections import Counter

import pytest
from mock import call

from kelvin.common.algorithms import kuhn


class TestKuhn(object):
    """Тест алгоритма Куна"""

    @pytest.mark.parametrize(
        'graph,size1,size2,max_matching_size',
        (
            # Простой пример
            (
                [[0], [0], [1, 2]],
                3,
                3,
                2,
            ),
            # Полное паросочетание
            (
                [[0, 1, 2], [3, 4], [1, 2, 4], [1, 2, 4], [0, 3, 4]],
                5,
                5,
                5,
            ),
            # Разный размер долей
            (
                [[0], [0], [0, 1], [1]],
                4,
                2,
                2,
            ),
            # Перевес долей в другую сторону
            (
                [[0, 1], [0]],
                2,
                4,
                2,
            ),
            # Для нахождения максимального паросочетания на последнем шаге
            # придется перерелаксировать всю цепь.
            (
                [[0, 1], [1, 2], [2, 3], [0]],
                4,
                4,
                4,
            ),
            # Для нахождения максимального паросочетания на последнем шаге
            # придется перерелаксировать всю цепь. Тест на глубокую рекурсию
            (
                [[x, x + 1] for x in range(99)] + [[0]],
                100,
                100,
                100,
            ),
            # Для нахождения максимального паросочетания на последнем шаге
            # придется перерелаксировать всю цепь. Тест на глубокую рекурсию.
            # В результате паросочетание не получится увеличить.
            (
                [[x, x + 1] for x in range(98)] + [[98], [0]],
                100,
                100,
                99,
            ),
            # Полное дерево и еще одна вершина которая заставит пройтись вглубь
            # по всему дереву
            (
                [list(range(99))] * 99 + [[0]],
                100,
                100,
                99,
            ),
        ),
    )
    def test_kuhn(self, graph, size1, size2, max_matching_size):
        start_time = time.time()
        return_matching_size, return_matching = kuhn(graph, size1, size2)

        assert time.time() - start_time < 1, (
            u'Алгоритм работает слишком долго')

        used = set()
        count_used_in2 = 0
        for vertex_from_1 in return_matching:
            if vertex_from_1 == -1:
                continue

            assert vertex_from_1 not in used, (
                u'Вершина из первой доли используется несколько раз')
            used.add(vertex_from_1)
            count_used_in2 += 1

        assert count_used_in2 == return_matching_size, (
            u'Реальный размер паросочетания не совпадает с возвращенным')

        assert return_matching_size == max_matching_size, (
            u'Не совпадает размер максимального паросочетания')
