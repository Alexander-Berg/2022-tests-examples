import pytest

from wiki.pages.constants import PageOrderPosition
from wiki.pages.logic.rank import MAX_PREFIX, MIN_PREFIX, MAX_STRING_LEN, calculate_page_rank, next_rank

pytestmark = [pytest.mark.django_db]


def test_next_rank():
    assert next_rank(MAX_PREFIX, MIN_PREFIX) == '00000000000000000001'
    assert next_rank(MIN_PREFIX, MAX_PREFIX) == '00000000000000000001'
    assert next_rank('00000000000000000001', '99999999999999999999') == '00000000000000000002'
    assert next_rank('00000000000000000001ABC', '00000000000000000001ABE') == '00000000000000000001ABD'
    assert next_rank('00000000000000000001ABC', '00000000000000000003ABE') == '00000000000000000002'
    assert next_rank('00000000000000012345', '00000000000000012347') == '00000000000000012346'
    assert next_rank('00000000000000012345', '00000000000000012346') == '00000000000000012345E'
    assert next_rank('00000000000000000000', '00000000000000000001') == '00000000000000000000E'
    assert next_rank('00000000000000000000', '00000000000000000000E') == '00000000000000000000A'
    assert next_rank('00000000000000000000', '00000000000000000002') == '00000000000000000001'

    assert next_rank('00000000000000000002E', '000000000000000000030E') == '00000000000000000002O'
    assert next_rank('00000000000000000002Z', '000000000000000000030') == '00000000000000000002Z0'

    # Некорректный формат ранга, нечисловой префикс, будут использованы значения по умолчанию
    assert next_rank('A', 'B') == '00000000000000000001'

    # Не выходим за пределы максимальной длинны ранга
    long_rank = '9' * 19 + '8' + 'Y' * MAX_STRING_LEN
    assert next_rank(long_rank, MAX_PREFIX) == long_rank


def test_calculate_page_rank(page_cluster, test_baseorg):
    # Выше рута ничего нет
    assert calculate_page_rank(PageOrderPosition.BEFORE, 'root', test_baseorg) == '00000000000000000002'

    # После root/b стоит root/a и места между префиксами нет
    assert calculate_page_rank(PageOrderPosition.AFTER, 'root/b', test_baseorg) == '00000000000000000001E'

    # Перед root/c ничего нет
    assert calculate_page_rank(PageOrderPosition.BEFORE, 'root/c', test_baseorg) == '00000000000000000004'
