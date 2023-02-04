# coding: utf-8
import pytest

import btestlib.utils as utils

'''Управление именами параметризованных тестов.
Для этого я переопределил стандратное поведение ids параметра в parametrize.
Теперь можно создавать id на основании всех параметров а не для каждого отдельно'''


@pytest.mark.parametrize('a, b', [(1, 10), (2, 20)],
                         ids=lambda a, b: 'a={}-b={}'.format(a, b))
def test_ids(a, b):
    """ Для каждого набора параметров будет применена переданная функция. Результирующая строка - id теста
    test_ids[a=1-b=10]
    test_ids[a=2-b=20]
    """
    print (a, b)


# закомментировано, т.к. ломает сборку. Примеры валидные
# @pytest.mark.parametrize('a, b, c', [(1, 10, 100), (2, 20, 200)],
#                          ids=lambda a, b: 'a={}-b={}'.format(a, b))
# def test_ids_less_args(a, b, c):
#     """Если в лямбде было задано неверное число аргументов, при коллекте будет ошибка
#     TestsError: Parametrize ids function has wrong number of arguments (2) for set: a, b, c"""
#     print (a, b, c)
#
#
# @pytest.mark.parametrize('a, b', [(1, 10), (2, 20)],
#                          ids=lambda a, b, c: 'a={}-b={}'.format(a, b))
# def test_ids_more_args(a, b):
#     """TestsError: Parametrize ids function has wrong number of arguments (3) for set: a, b"""
#     print (a, b)

# @pytest.mark.parametrize('a, b', [(1, 10), (2, 20)],
#                          ids=lambda a, b: None)
# def test_ids_not_string(a, b):
#     """Если переданная функция возвращает не строку будет ошибка на этапе сборки.
#     С юникод строками тоже работу не далал, т.к. вроде не надо и не факт что заработает.
#     TestsError: Parametrize ids function does not return str for set: a, b
#     """
#     print (a, b)


@pytest.mark.parametrize('a, b', [(1, 10), (2, 20)],
                         ids=lambda a, b: 'a={}-b={}'.format(a, b))
@pytest.mark.parametrize('c, d', [(100, 1000)],
                         ids=lambda c, d: 'c={}-d={}'.format(c, d))
def test_ids_several_parametrize(a, b, c, d):
    """Для случая нескольких наборов параметров ids создастся для каждого, а потом объединятся.
    Разделитель будет стандартный путестовский '-'
    test_ids_several_parametrize[a=1-b=10-c=100-d=1000]
    test_ids_several_parametrize[a=2-b=20-c=100-d=1000]
    """
    print (a, b, c, d)


@pytest.mark.parametrize('a, b', [(1, 10), (2, 20)],
                         ids=lambda a, b: 'a={}-b={}'.format(a, b))
@pytest.mark.parametrize('c, d', [(100, 1000)],
                         ids=lambda c, d: 'c={}-d={}'.format(c, d))
def test_ids_several_parametrize2(c, d, a, b):
    """Строки объединяются в том порядке в каком заданы в сигнатуре тестового метода
    test_ids_several_parametrize2[c=100-d=1000-a=1-b=10]
    test_ids_several_parametrize2[c=100-d=1000-a=2-b=20]
    """
    print (a, b, c, d)


@pytest.mark.parametrize('a, b', [(1, 10), (2, 20)],
                         ids=lambda a, b: 'a={}-b={}'.format(a, b))
@pytest.mark.parametrize('c, d', [(100, 1000)],
                         ids=lambda c, d: 'c={}-d={}'.format(c, d))
def test_ids_several_parametrize3(a, c, b, d):
    """Если в сигнатуре параметры перемешаны, строки будут объединяться в порядке обратном тому как определены
    test_ids_several_parametrize2[c=100-d=1000-a=1-b=10]
    test_ids_several_parametrize2[c=100-d=1000-a=2-b=20]
    """
    print (a, b, c, d)


@pytest.mark.parametrize('a, b', [(1, 10), (2, 20)],
                         ids=lambda x: 'x={}'.format(x))
def test_ids_old(a, b):
    """Если вы задаете лямбду с одним параметром то поведение будет стандартное путестовское
    Даже если параметров в тесте несколько ошибки не будет. Ибо обратная совместимость
    test_ids_old[x=1-x=10]
    test_ids_old[x=2-x=20]
    """
    print (a, b)


# По сгенеренным id можно запустить тест с конкретным набором параметров из параметризации
# py.test scripts/examples/pytest_examples.py::test_ids[a=2-b=20] -v -s
# Напоминаю что если использовать с тексте имени хитрые символы или пробелы то путь в команде нужно взять в кавычки


'''
utils.Pytest.combine - из 2х наборов значений для двух параметров сформирует такой набор параметров что каждое значение
каждого параметра используется хотябы раз.
Если не хочется использовать все стандартные перестановки, как делает стандартно путест если задать два parametrize
'''


@pytest.mark.parametrize(*utils.Pytest.combine(
    utils.Pytest.ParamsSet(names='letter',
                           values=['a', 'b']),
    utils.Pytest.ParamsSet(names='number',
                           values=[1, 2])),
                         ids=lambda letter, number: '{}-{}'.format(letter, number))
def combine_usage1(letter, number):
    """
    Параметризация будет аналогична вызову
    @pytest.mark.parametrize('letter, number', [('a', 1), ('b', 2)])
    Использование ids тоже по аналогии с таким вызовом параметрайз
    """
    pass


@pytest.mark.parametrize(*utils.Pytest.combine(
    utils.Pytest.ParamsSet(names='letter',
                           values=['a', 'b', 'c', 'd', 'e']),
    utils.Pytest.ParamsSet(names='number',
                           values=[1, 2])))
def combine_usage2(letter, number):
    """
    Если набор значения для одного параметра больше чем для другого - меньший будет дополнен до длины большего.
    Для этого будут использованы его собственные значения по порядку.

    @pytest.mark.parametrize('letter, number', [('a', 1), ('b', 2), ('c', 1), ('d', 2), ('e', 1)])

    В целом ради этого юзкейса метод и был написан. И в целом такой набор параметров не сложно составить и руками.
     Но есть преймущества:
       проще исключить одно значение для параметра - закомментировать только в одном месте
       проще маркировать конкретное значение - тоже только в одном месте
       проще копипастить и в целом комбинировать разные наборы
       можем управлять внутри функции как формируется набор параметров
       я не люблю позиционное соответствие
       проще понять как формируется набор параметров (вроде)
       меньше писать, но не всегда
    """
    pass


@pytest.mark.parametrize(*utils.Pytest.combine(
    utils.Pytest.ParamsSet(names='letter',
                           values=[
                               pytest.mark.xfail('a'),
                               'b',
                               'c'
                           ]),
    utils.Pytest.ParamsSet(names='number',
                           values=[
                               pytest.mark.slow(1),
                               2
                           ])))
def combine_usage3(letter, number):
    """
    Можно использовать марк как всегда. Если марк был применен к значениям обоих параметров - к паре будут применены оба
    @pytest.mark.parametrize('letter, number', [
                                    pytest.mark.xfail(pytest.mark.slow(('a', 1))),
                                    ('b', 2),
                                    pytest.mark.slow('c', 1)])
    """
    pass


@pytest.mark.parametrize(*utils.Pytest.combine(
    utils.Pytest.ParamsSet(names='letter1, letter2',
                           values=[('a', 'x'),
                                   ('b', 'y')
                                   ]),
    utils.Pytest.ParamsSet(names='number1, number2',
                           values=[(1, 10),
                                   (2, 20)
                                   ])))
def combine_usage4(letter, number):
    """
    Каждый из наборов может быть содержать значения для нескольких параметров
    @pytest.mark.parametrize('letter1, letter2, number1, number2', [
                                    ('a', 'x', 1, 10))),
                                    ('b', 'y', 2, 20)
                                    ])
    """
    pass


@pytest.mark.parametrize(*utils.Pytest.combine(('letter', ['a', 'b']),
                                               ('number', [1, 2])))
def combine_usage5(letter, number):
    """
    Для краткости можно использовать в качестве параметров и обычные туплы
    """
    pass


'''
utils.Pytest.combine_set - служит аналогом ситуации, когда в кейс передаются параметры в нескольких
parametrize, но при этом требуется, чтобы некоторые из них не пересекались. Проще говоря, когда в наличие два параметра,
pytest создает количество тестов, равное их декартовому произведению. Данный метод генерирует такое количество тестов,
что у первого ParamsSet первый список декартово перемножается с первым списком второго ParamsSet. Второй список со
вторым, n-ный список с n-ным.
Количество ParamsSet любое больше двух.
Количество values в каждом ParamsSet любое больше двух (можно и одно, но зачем?).
Единственное требование - количество values в каждом ParamsSet должно быть одинаковым.

'''


@pytest.mark.parametrize(*utils.Pytest.combine_set(
    utils.Pytest.ParamsSet(names='letter',
                           values=[['a', 'x'],
                                   ['b', 'y']
                                   ]),
    utils.Pytest.ParamsSet(names='number',
                           values=[[1, 10],
                                   [2, 20]
                                   ])))
def combine_set_usage1(letter, number):
    """
    Простой пример применения. Результат будет аналогичен:
    @pytest.mark.parametrize('letter, number',
                                [('a', '1'),
                                ('a', '10'),
                                ('x', '1'),
                                ('x', '10'),
                                ('b', '2'),
                                ('b', '20'),
                                ('y', '2'),
                                ('y', '20')
                                ])
    """
    pass


'''ВАЖНО. params_format устарел и был выпилен. Более его использовать нельзя. Пользуйтесь parametrize(..., ids=...)'''
