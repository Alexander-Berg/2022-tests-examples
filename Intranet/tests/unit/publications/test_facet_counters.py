import pytest

from intranet.femida.src.publications.choices import PUBLICATION_FACETS
from intranet.femida.src.publications.counters import FacetCounter

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db

FACETS = list(PUBLICATION_FACETS._db_values)


def _create_publication_facet(facet_id, value, pub_ids, facets=FACETS):
    f.PublicationFacetFactory(
        facet=facets[facet_id],
        value=str(value),
        publication_ids=pub_ids,
    )


@pytest.mark.parametrize('initial_data, correct_result', (
    (  # не выбрана ни одна галочка, должно вернуть длины списков
        {}, {FACETS[0]: {'0': 1, '1': 3}},
    ),
    (  # аналогично если список внутри фасета пришёл пустой (кривой случай)
        {FACETS[0]: []}, {FACETS[0]: {'0': 1, '1': 3}},
    ),
    (  # выбор несуществующего фасета, должен быть проигнорирован
        {FACETS[1]: ['0']}, {FACETS[0]: {'0': 1, '1': 3}},
    ),
    (  # выбор несуществующего значения существующего фасета, должен быть проигнорирован
        {FACETS[0]: ['2']}, {FACETS[0]: {'0': 1, '1': 3}},
    ),
    (  # выбрана только нулевая галочка
        {FACETS[0]: ['0']}, {FACETS[0]: {'0': 1, '1': 3}},
    ),
    (  # выбрана только первая галочка
        {FACETS[0]: ['1']}, {FACETS[0]: {'0': 1, '1': 3}},
    ),
    (  # выбраны обе галочки
        {FACETS[0]: ['0', '1']}, {FACETS[0]: {'0': 1, '1': 3}},
    ),
    (  # выбрана существующая галочка вместе с несуществующими
        {FACETS[0]: ['0', '2'], FACETS[1]: ['0']}, {FACETS[0]: {'0': 1, '1': 3}},
    ),
))
def test_single_facet(initial_data, correct_result):
    _create_publication_facet(0, 0, [0])
    _create_publication_facet(0, 1, [0, 1, 2])

    counters = FacetCounter(initial_data).compute()

    assert counters == correct_result


@pytest.mark.parametrize('initial_data, correct_result', (
    ({FACETS[0]: ['0']}, {FACETS[0]: {'0': 2, '1': 2}, FACETS[1]: {'0': 2, '1': 1}}),
    ({FACETS[0]: ['1']}, {FACETS[0]: {'0': 2, '1': 2}, FACETS[1]: {'0': 1, '1': 2}}),
    ({FACETS[0]: ['0', '1']}, {FACETS[0]: {'0': 2, '1': 2}, FACETS[1]: {'0': 3, '1': 3}}),
))
def test_one_vs_one_facet(initial_data, correct_result):
    _create_publication_facet(0, 0, [0, 1])
    _create_publication_facet(0, 1, [2, 3])

    _create_publication_facet(1, 0, [0, 1, 2])
    _create_publication_facet(1, 1, [1, 2, 3])

    counters = FacetCounter(initial_data).compute()

    assert counters == correct_result


@pytest.mark.parametrize('initial_data, correct_result', (
    (
        {FACETS[0]: ['0'], FACETS[1]: ['0'], FACETS[2]: ['0']},
        {
            FACETS[0]: {'0': 1, '1': 0, '2': 2},
            FACETS[1]: {'0': 1, '1': 2, '2': 2},
            FACETS[2]: {'0': 1, '1': 1, '2': 0},
        },
    ),
    (
        {FACETS[0]: ['0', '1'], FACETS[1]: ['0', '1'], FACETS[2]: ['0', '1']},
        {
            FACETS[0]: {'0': 4, '1': 2, '2': 2},
            FACETS[1]: {'0': 2, '1': 2, '2': 2},
            FACETS[2]: {'0': 3, '1': 3, '2': 3},
        },
    ),
))
def test_three_facets(initial_data, correct_result):
    _create_publication_facet(0, 0, [0, 1, 3, 4])
    _create_publication_facet(0, 1, [1, 4, 5])
    _create_publication_facet(0, 2, [2, 5, 0])

    _create_publication_facet(1, 0, [0, 1, 2])
    _create_publication_facet(1, 1, [3, 4, 5])
    _create_publication_facet(1, 2, [0, 2, 3, 5])

    _create_publication_facet(2, 0, [0, 2, 3, 4])
    _create_publication_facet(2, 1, [1, 2, 3, 4])
    _create_publication_facet(2, 2, [2, 3, 4, 5])

    counters = FacetCounter(initial_data).compute()

    assert counters == correct_result


@pytest.mark.parametrize('initial_data, correct_result', (
    (
        {FACETS[0]: ['0'], FACETS[1]: ['0']},
        {
            FACETS[0]: {'0': 3, '1': 4},
            FACETS[1]: {'0': 3, '1': 3},
            FACETS[2]: {'0': 2, '1': 0},
            FACETS[3]: {'0': 0, '1': 0},
        },
    ),
    (
        {FACETS[0]: ['1'], FACETS[1]: ['0']},
        {
            FACETS[0]: {'0': 3, '1': 4},
            FACETS[1]: {'0': 4, '1': 5},
            FACETS[2]: {'0': 3, '1': 1},
            FACETS[3]: {'0': 0, '1': 2},
        },
    ),
    (
        {FACETS[0]: ['0'], FACETS[1]: ['1']},
        {
            FACETS[0]: {'0': 3, '1': 5},
            FACETS[1]: {'0': 3, '1': 3},
            FACETS[2]: {'0': 0, '1': 1},
            FACETS[3]: {'0': 2, '1': 1},
        },
    ),
    (
        {FACETS[0]: ['1'], FACETS[1]: ['1']},
        {
            FACETS[0]: {'0': 3, '1': 5},
            FACETS[1]: {'0': 4, '1': 5},
            FACETS[2]: {'0': 1, '1': 3},
            FACETS[3]: {'0': 3, '1': 2},
        },
    ),
))
def test_four_facets(initial_data, correct_result):
    _create_publication_facet(0, 0, [0, 2, 4, 6, 8])
    _create_publication_facet(0, 1, [1, 2, 3, 4, 5, 6, 7])

    _create_publication_facet(1, 0, [0, 1, 2, 3, 4])
    _create_publication_facet(1, 1, [3, 4, 5, 6, 7, 8, 9])

    _create_publication_facet(2, 0, [0, 1, 2, 3])
    _create_publication_facet(2, 1, [3, 6, 7])

    _create_publication_facet(3, 0, [5, 6, 7, 8])
    _create_publication_facet(3, 1, [1, 3, 6])

    counters = FacetCounter(initial_data).compute()

    assert counters == correct_result


@pytest.mark.parametrize('initial_data, available_publication_ids, correct_result', (
    ({FACETS[0]: ['0']}, None, {FACETS[0]: {'0': 6, '1': 6}, FACETS[1]: {'0': 4, '1': 2}}),
    ({FACETS[0]: ['1']}, None, {FACETS[0]: {'0': 6, '1': 6}, FACETS[1]: {'0': 0, '1': 4}}),
    ({FACETS[0]: ['0']}, set(range(10)), {FACETS[0]: {'0': 6, '1': 6}, FACETS[1]: {'0': 4, '1': 2}}),
    ({FACETS[0]: ['1']}, set(range(10)), {FACETS[0]: {'0': 6, '1': 6}, FACETS[1]: {'0': 0, '1': 4}}),
    ({FACETS[0]: ['0']}, {0, 1, 2, 3, 4}, {FACETS[0]: {'0': 3, '1': 3}, FACETS[1]: {'0': 2, '1': 1}}),
    ({FACETS[0]: ['1']}, {0, 1, 2, 3, 4}, {FACETS[0]: {'0': 3, '1': 3}, FACETS[1]: {'0': 0, '1': 2}}),
    ({FACETS[0]: ['0']}, {0, 2, 4, 6, 8}, {FACETS[0]: {'0': 3, '1': 3}, FACETS[1]: {'0': 2, '1': 1}}),
    ({FACETS[0]: ['1']}, {0, 2, 4, 6, 8}, {FACETS[0]: {'0': 3, '1': 3}, FACETS[1]: {'0': 0, '1': 2}}),
))
def test_available_publication_ids(initial_data, available_publication_ids, correct_result):
    _create_publication_facet(0, 0, [0, 1, 2, 5, 6, 7])
    _create_publication_facet(0, 1, [2, 3, 4, 7, 8, 9])

    _create_publication_facet(1, 0, [0, 1, 5, 6])
    _create_publication_facet(1, 1, [2, 3, 7, 8])

    counters = FacetCounter(initial_data, available_publication_ids).compute()

    assert counters == correct_result
