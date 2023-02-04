import datetime
import json
import time
from builtins import range
from collections import Counter
from string import hexdigits

import pytest
import pytz
from mock import MagicMock

from django.core.exceptions import ValidationError

from kelvin.common.utils import (
    ALPHABET, COLOR_CREATION_ATTEMPTS, DEFAULT_COLOR, EmptyTrueTuple, OrderedCounter, client_is_android,
    dt_from_microseconds, dt_to_microseconds, generate_code, generate_color_from_set, generate_graph_from_counters,
    generate_random_color, generate_unique_color, group_level_sorted_key, make_timestamp, normalize_name, safe_filename,
    takewhile_with_first_fail, transliterate,
)
from kelvin.common.validators import validate_color
from kelvin.group_levels.models import GroupLevel
from kelvin.problems.checkers import DragChecker, InlineChecker

TEST_COLOR_GENERATION_ATTEMPTS = 3  # к-во попыток случайной генерации

GROUP_LEVEL_SORTED_KEY_CASES = (
    (
        GroupLevel(id=2, baselevel=1, slug='slug1'),
        GroupLevel(id=1, baselevel=2, slug='slug2'),
    ),
    (
        GroupLevel(id=1, baselevel=1, name=u'Младшая группа', slug='slug1'),
        GroupLevel(id=2, baselevel=1, name=u'Старшая группа', slug='slug2'),
    ),
    (
        GroupLevel(id=1, baselevel=1, name=u'1 класс', slug='slug1'),
        GroupLevel(id=2, baselevel=1, name=u'2 класс', slug='slug2'),
    ),
    (
        GroupLevel(id=1, baselevel=1, name=u'9класс', slug='slug1'),
        GroupLevel(id=2, baselevel=1, name=u'10класс', slug='slug2'),
    ),
    (
        GroupLevel(id=1, baselevel=1, name=u'10класс', slug='slug1'),
        GroupLevel(id=2, baselevel=2, name=u'9класс', slug='slug2'),
    ),
    (
        GroupLevel(id=1, baselevel=1, name=u'9 класс 3 дан', slug='slug1'),
        GroupLevel(id=2, baselevel=1, name=u'9 класс 12 дан', slug='slug2'),
    ),
    (
        GroupLevel(id=1, baselevel=1, name=u'8 класс 12 дан', slug='slug1'),
        GroupLevel(id=2, baselevel=1, name=u'9 класс 3 дан', slug='slug2'),
    ),
)

TRANSLITERATE_CASES = (
    (u'Ёж', 'Ezh'),
    (u'Ъпячка', 'pyachka'),
    (u'Йод', 'Yod'),
    (u'Ядовитая Ющерица', 'Yadovitaya Yushcheritsa'),
)


def test_generate_code():
    """
    Проверяем, что генерируется код заданной длины с символами из алфавита,
    заданного в модуле `common.utils`
    """
    for code_length in [5, 6, 12]:
        code = generate_code(code_length)
        assert len(code) == code_length, (
            u'Неправильная длина сгенерированного кода')
        for sign in code:
            assert sign in ALPHABET, (
                u'Все символы кода должны быть в алфавите `{0}`'
                .format(ALPHABET)
            )

    code = generate_code(5, alphabet='abc')
    for letter in code:
        assert letter in 'abc'


@pytest.mark.parametrize('initial', ([], [0], [False, 2]))
def test_empty_true_tuple(initial):
    """
    Проверяем, что `EmptyTrueTuple` имеет истинное логическое значение
    """
    assert bool(EmptyTrueTuple(initial)) is True


def test_generate_color():
    """
    Проверяем, что `generate_random_color` возвращает корректный цвет
    """
    for __ in range(TEST_COLOR_GENERATION_ATTEMPTS):
        color = generate_random_color()
        assert len(color) == 7, (
            u'Неправильная длина hex-представления цвета')
        assert color[0] == '#', (
            u'Первым символом цвета должен быть знак `#`')
        for digit in color[1:]:
            assert digit in hexdigits, (
                u'В представлении цвета {0} встречаются символы не из '
                u'hex-диапазона'.format(color)
            )


def test_generate_unique_color(mocker):
    """
    Тесты `generate_unique_color`
    """
    # если список использованных цветов пустой
    generated_color = generate_unique_color([])

    # Если был сгенерироан неправильный цвет, то здесь будет эксепшен
    try:
        validate_color(generated_color)
    except ValidationError:
        pytest.fail(u'Был сгенерирован неправильный цвет {0}'
                    .format(generated_color))

    # проверяем, что возвращается код по умолчанию и логируем,
    # если не получилось создать цвет (при всех попытках генерации он занят)
    mocked_generate_color = mocker.patch(
        'kelvin.common.utils.generate_random_color')
    mocked_generate_color.return_value = '#111111'

    mocked_qs = ['#111111']
    mocked_logger = mocker.patch('kelvin.common.utils.logger')

    assert generate_unique_color(mocked_qs) == DEFAULT_COLOR, (
        u'Если не получилось сгенерировать незанятый цвет, то он '
        u'выставляется в дефолтный'
    )
    assert (mocked_generate_color.call_count == COLOR_CREATION_ATTEMPTS), (
        u'Попыток генерации цвета должно быть как минимум {0}'
        .format(COLOR_CREATION_ATTEMPTS)
    )
    mocked_logger.warning.assert_called_once_with(
        'Had to assign default color %s', '')

    mocked_logger.reset_mock()
    mocked_generate_color.reset_mock()

    assert generate_unique_color(mocked_qs, default_color='abc',
                                 additional_log_string='for test') == 'abc', (
        u'Если не получилось сгенерировать незанятый цвет, то он'
        u'выставляется в дефолтный, переданный в параметрах'.format()
    )
    assert (mocked_generate_color.call_count == COLOR_CREATION_ATTEMPTS), (
        u'Попыток генерации цвета должно быть как минимум {0}'
        .format(COLOR_CREATION_ATTEMPTS)
    )
    mocked_logger.warning.assert_called_once_with(
        'Had to assign default color %s', 'for test')


cases_color_from_set = (
    (['#aaaaaa', '#bbbbbb'], None, ['#aaaaaa', '#bbbbbb']),
    (['#aaaaaa', '#bbbbbb'], ['#aaaaaa'], ['#bbbbbb']),
    (['#aaaaaa', '#bbbbbb'], ['#aaaaaa', '#bbbbbb', '#bbbbbb',
                              '#aaaaaa', '#aaaaaa'],
     ['#bbbbbb']),
    (['#aaaaaa', '#bbbbbb'], ['#cccccc'],
     ['#bbbbbb', '#aaaaaa']),
    (['#aaaaaa', '#bbbbbb'], ['#aaaaaa', '#cccccc', '#bbbbbb',
                              '#aaaaaa', '#aaaaaa', '#bbbbbb'],
     ['#bbbbbb']),
)


@pytest.mark.parametrize('colors_limit,used_colors,expected',
                         cases_color_from_set)
def test_generate_color_from_set(colors_limit, used_colors, expected):
    """
    Тестирование генерации цвета из ограниченного набора
    """
    assert generate_color_from_set(colors_limit, used_colors) in expected


@pytest.mark.parametrize('smaller,bigger', GROUP_LEVEL_SORTED_KEY_CASES)
def test_group_level_sorted_key(smaller, bigger):
    assert group_level_sorted_key(smaller) < group_level_sorted_key(bigger), (
        u'Уровень группы {0} должен предшествовать уровню группы {1}'
        .format(smaller, bigger)
    )


@pytest.mark.parametrize('case, expected', TRANSLITERATE_CASES)
def test_transliterate(case, expected):
    """
    Тест транслитерации
    """
    assert transliterate(case) == expected, u'Неправильная транслитерация'


TIMESTAMP_CASES = (
    (datetime.datetime(1970, 1, 1, 0, 2, 2), 122),
    (datetime.datetime(1980, 2, 1, 0, 1), 318211260),
    (datetime.datetime(1960, 2, 1, 0, 1), -312940740),
    (datetime.datetime(2015, 6, 15, 2, 3), 1434333780),
)


@pytest.mark.parametrize('case,expected', TIMESTAMP_CASES)
def test_make_timestamp(case, expected):
    """
    Тест создания численного таймстампа по объекту datetime
    """
    assert make_timestamp(case) == expected, u'Неправильный таймстамп'


SAFE_FILENAME_CASES = (
    (u'Скриншот 2010.png', '', u'Skrinshot_2010_<timestamp>.png'),
    (u'Имя файла.ppt', '', u'Imia_faila_<timestamp>.ppt'),
    (u'Чего?!.jpg', '', u'Chego_<timestamp>.jpg'),
    (u'Математика: Д\'Артаньян', '', u'Matematika_DArtanian_<timestamp>'),
    (u'badname\'\\/:*?\'<>|!#$+%`&*{}“=@.txt', '', u'badname_<timestamp>.txt'),
    (
        u'badname_badname_badname_badname\'\\/:*?\'<>|!#$+%`&*{}“=@.txt',
        u'overly/long/upload/path/test/var/lib/pelican/utils/dangerous_utils/',
        u'overly/long/upload/path/test/var/lib/pelican/utils/dangerous_utils/'
        u'badname_badname_b_<timestamp>.txt'
    ),
    # без слеша в пути
    (
        u'badname_badname_badname_badname\'\\/:*?\'<>|!#$+%`&*{}“=@.txt',
        u'overly/long/upload/path/test/var/lib/pelican/utils/dangerous_utils',
        u'overly/long/upload/path/test/var/lib/pelican/utils/dangerous_utils/'
        u'badname_badname_b_<timestamp>.txt'
    ),
)


@pytest.mark.parametrize('case,upload_dir,expected', SAFE_FILENAME_CASES)
def test_safe_filename(mocker, case, upload_dir, expected):
    """
    Тест создания безопасного уникального имени файла
    """
    mocked_make_timestamp = mocker.patch('kelvin.common.utils.make_timestamp')
    mocked_make_timestamp.return_value = u'<timestamp>'
    assert safe_filename(case, upload_dir) == expected, (
        u'Неправильное преобразование имени файла')
    assert mocked_make_timestamp.called, u'Не было создания таймстампа'


def test_dt_from_microseconds():
    """
    Тест конвертации `datetime` в микросекунды
    """
    assert (dt_from_microseconds(1444747581017227) == datetime.datetime(
        2015, 10, 13, 14, 46, 21, 17227, tzinfo=pytz.utc,
    ))


def test_dt_to_microseconds():
    """
    Тест конвертации микросекунд в `datetime`
    """
    assert dt_to_microseconds(datetime.datetime(
        2015, 10, 13, 14, 46, 21, 17227, tzinfo=pytz.utc)) == 1444747581017227


NORMALIZE_NAME_CASES = (
    (u'петров', u'Петров'),
    (u' васИлий', u'Василий'),
    (u'петроВ   водкин  ', u'Петров Водкин'),
    (u'  Мамин-сибиряк  ', u'Мамин-Сибиряк'),
    (u' ALLCAPS ВСЕКАПСОВ', u'Allcaps Всекапсов'),
)


@pytest.mark.parametrize('case,expected', NORMALIZE_NAME_CASES)
def test_normalize_name(case, expected):
    """
    Тест нормализации имен
    """
    assert normalize_name(case) == expected, u'Неправильная нормализация'


HEX_COLOR_TO_TUPLE_CASES = (
    (u'#00FF00', (0, 255, 0)),
    (u'#0000ff', (0, 0, 255)),
    (u'#e2cfec', (226, 207, 236)),
    (u'#e2CfeC', (226, 207, 236)),
    (u'#ABCDEF', (171, 205, 239)),
)


CLIENT_IS_ANDROID_CASES = (
    ({}, {}, False),
    ({'some': 'data'}, {}, False),
    ({'some': 'data', 'request': MagicMock()}, {}, False),
    (
        {'some': 'data', 'request': MagicMock()},
        {'client_application': 'notAndroid'},
        False
    ),
    (
        {'some': 'data', 'request': MagicMock()},
        {'client_application': 'Android'},
        True
    ),
)


@pytest.mark.parametrize('context,meta,expected', CLIENT_IS_ANDROID_CASES)
def test_client_is_android(context, meta, expected):
    """Тест определения андроид-клиентов"""
    if 'request' in context:
        context['request'].META = meta
    assert client_is_android(context) == expected, (
        u'Клиент определен неправильно')


TAKEWHILE_WITH_FIRST_FAIL_CASES = (
    ([], lambda x: x < 5, []),
    ([6], lambda x: x < 5, [6]),
    ([5, 6], lambda x: x < 5, [5]),
    ([1, 4, 6, 4, 1], lambda x: x < 5, [1, 4, 6]),
    ([True, True, False, True, False], lambda x: x, [True, True, False]),
    (
        [{'mistakes': 2}, {'mistakes': 2}, {'mistakes': 0}, {'mistakes': 2}],
        lambda x: x['mistakes'],
        [{'mistakes': 2}, {'mistakes': 2}, {'mistakes': 0}],
    ),
)


@pytest.mark.parametrize('iterable,predicate,expected_list',
                         TAKEWHILE_WITH_FIRST_FAIL_CASES)
def test_takewhile_with_first_fail(iterable, predicate, expected_list):
    """Тесты генератора-аналога `itertools.takewhile`"""
    print('predicate=', predicate)
    print('iterable=', iterable)
    print(expected_list)
    assert expected_list == list(
        takewhile_with_first_fail(predicate, iterable),
    )


@pytest.mark.parametrize('data,answer', (
    # Проверяем автоматическое приведение к OrderedCounter, далее будем
    # явно использовать OrderedCounter, чтобы рулить порядком элементов
    (
        (
            Counter([1]),
            Counter([
                frozenset([1]),
            ]),
        ),
        [
            [0],
        ],
    ),
    (
        (
            Counter([1, 2]),
            Counter([
                frozenset([1]),
            ]),
        ),
        False,
    ),
    (
        (
            OrderedCounter([2, 4, 3]),
            OrderedCounter([
                frozenset([1, 2, 3]),
                frozenset([2, 4]),
                frozenset([2, 3]),
            ]),
        ),
        [
            [0, 1, 2],
            [1],
            [0, 2],
        ],
    ),
    # Проверка на одинаковые значения. Из-за Counter они сольются в
    # один и индексы у них будут рядом с первым вхождением.
    # Пример: [1, 4, 1, 3, 4] --> [1, 1, 4, 4, 3]
    (
        (
            OrderedCounter([2, '2', 4, 3, 4]),  # [2, '2', 4, 4, 3]
            OrderedCounter([
                frozenset([1, 2, 3]),       # 0
                frozenset([2, 4]),          # 1
                frozenset(['2', 3]),        # 3
                frozenset([3, 4, '2']),     # 4
                frozenset([2, 4]),          # 2
            ]),
        ),
        [
            [0, 1, 2],
            [3, 4],
            [1, 2, 4],
            [1, 2, 4],
            [0, 3, 4],
        ],
    ),
    # Проверка на генерацию большого (100 вершин в каждой доле) графа
    (
        (
            Counter({0: 100}),
            OrderedCounter([frozenset(list(range(10)))] * 100),
        ),
        [list(range(100))] * 100,
    ),
))
def test_generate_graph(data, answer):
    time_start = time.time()
    assert generate_graph_from_counters(*data) == answer, (
        u'Граф сгенерировался плохо')
    assert time.time() - time_start < 1, (
        u'Граф генерировался слишком долго')
