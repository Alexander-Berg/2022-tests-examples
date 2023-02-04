import unittest
from collections import OrderedDict

from dwh.grocery.tools.diff import Diff, DiffSmall, DiffBig, get_changed_columns


class TestDiffFree(unittest.TestCase):
    """
        Проверка функций diff, не принадлежащих к классу
    """

    def test_get_changed_columns(self):
        prod_rows = [
            {
                "name": "Иван Иванов",
                "id": 1,
                "activity": "something",
                "long_long_long_column": "billing"
            },
            {
                "name": "Иван Петров",
                "id": 10,
                "activity": "equal",
                "long_long_long_column": "different"
            },
            {
                "name": "Петр Иванов",
                "id": 100,
                "activity": "OEBS",
                "long_long_long_column": "billing"
            }
        ]
        dev_rows = [
            {
                "name": "Ваня Иванов",
                "id": 1,
                "activity": "smth",
                "long_long_long_column": "billing"
            },
            {
                "name": "Александр Пушкин",
                "id": 10,
                "activity": "equal",
                "long_long_long_column": "diff"
            },
            {
                "name": "Петр Иванов",
                "id": 100,
                "activity": "OEBS",
                "long_long_long_column": "billing"
            }
        ]

        expected = OrderedDict({"name": 20, "activity": 20, "long_long_long_column": 23})
        self.assertEqual(expected, get_changed_columns(prod_rows, dev_rows))


class TestDiff(unittest.TestCase):
    """
        Проверка функций класса Diff
    """

    def test_split_by_lines(self):
        """
            Проверяем функцию split_by_lines
        """
        expected = [
            # делим на равные части
            {
                "cell": "123456",
                "length": 3,
                "result": ["123", "456"]
            },
            # делим на неравные части
            {
                "cell": "123456",
                "length": 4,
                "result": ["1234", "56"]
            },
            # делим пустую строку
            {
                "cell": "",
                "length": 4,
                "result": []
            }

        ]
        diff = Diff([], [], OrderedDict({}))
        for example in expected:
            self.assertEqual(example['result'], diff.split_by_lines(example['cell'], example['length']))


class TestDiffSmall(unittest.TestCase):
    """
        Проверка функций класса DiffSmall
    """

    prod_rows = [
        {
            "person_name": "Карлсон",
            "manager_code": "1234"
        }
    ]
    dev_rows = [
        {
            "person_name": "Карл",
            "manager_code": "1234"
        },
        {
            "person_name": "Винни Пух",
            "manager_code": "5678"
        },
        {
            "person_name": "Иван Иванович Иванов",
            "manager_code": "9876"
        }
    ]

    def test_get_first_row(self):
        """
            Проверяем функцию get_first_row (форматирование первой строки)
        """
        expected = '  PROD  |||  DEV   '
        diff = DiffSmall([], [], OrderedDict({}))
        result = diff.get_first_row(20)
        self.assertEqual(expected, result)

    def test_get_column_row(self):
        expected = "    person_name     |  manager_code   |||    person_name     |  manager_code   "
        diff = DiffSmall([], [], OrderedDict({"person_name": 20, "manager_code": 17}))
        result = diff.get_column_row()
        self.assertEqual(expected, result)

    def test_get_row(self):
        """
            Проверяем функцию get_row
        """
        diff = DiffSmall([], [], OrderedDict({"person_name": 10, "manager_code": 8}))
        left = {"person_name": ["Карлсон"], "manager_code": [12345678, 910]}
        right = {"person_name": ["Карлсон"], "manager_code": []}
        expected = [
            ' Карлсон  |12345678||| Карлсон  |        ',
            '          |  910   |||          |        '
        ]
        self.assertEqual(expected, diff.get_row(left, right))

    def test_get_rest(self):
        """
            Проверяем функцию get_rest
        """

        diff = DiffSmall(self.prod_rows, self.dev_rows, OrderedDict({"person_name": 10, "manager_code": 8}))
        diff.sep = '-' * (sum(value for key, value in diff.columns.items()) * 2 + 3 + (len(diff.columns) - 1) * 2)
        # Слева пусто, т.к. в dev-таблице больше рядов
        expected = [
            'DEV table containes more rows.',
            '-----------------------------------------',
            '          |        |||Винни Пух |  5678  ',
            '-----------------------------------------',
            '          |        |||Иван Ивано|  9876  ',
            '          |        |||вич Иванов|        ',
            '-----------------------------------------'
        ]
        self.assertEqual(expected, diff.get_rest())

    def test_get_body(self):
        """
            Проверяем основную функцию -- get_body
        """
        diff = DiffSmall(self.prod_rows, self.dev_rows, OrderedDict({"person_name": 12, "manager_code": 13}))
        diff.sep = '-' * (sum(value for key, value in diff.columns.items()) * 2 + 3 + (len(diff.columns) - 1) * 2)
        expected = [
            '           PROD           |||           DEV            ',
            '-------------------------------------------------------',
            'person_name |manager_code |||person_name |manager_code ',
            '-------------------------------------------------------',
            '  Карлсон   |    1234     |||    Карл    |    1234     ',
            '-------------------------------------------------------',
            'DEV table containes more rows.',
            '-------------------------------------------------------',
            '            |             ||| Винни Пух  |    5678     ',
            '-------------------------------------------------------',
            '            |             |||Иван Иванови|    9876     ',
            '            |             |||  ч Иванов  |             ',
            '-------------------------------------------------------'
        ]
        self.assertEqual(expected, diff.get_body())


class TestDiffBig(unittest.TestCase):
    """
        Проверка функций класса DiffBig
    """

    prod_rows = [
        {
            "person_name": "Карлсон",
            "manager_code": "1234"
        }
    ]
    dev_rows = [
        {
            "person_name": "Карл",
            "manager_code": "1234"
        },
        {
            "person_name": "Винни Пух",
            "manager_code": "5678"
        },
        {
            "person_name": "Иван Иванович Иванов",
            "manager_code": "9876"
        }
    ]

    def test_get_row(self):
        """
            Проверяем функцию  get_row
        """
        diff = DiffBig([], [], OrderedDict({"person_name": 12, "manager_code": 13, "group": 8}))
        lines = {
            "person_name": diff.split_by_lines("Очень длинное имя, состоящее из большого количества символов.", 12),
            "manager_code": diff.split_by_lines("100", 13),
            "group": diff.split_by_lines("Не очень длинное название", 8)
        }
        expected = ['prod /|Очень длинно|     100     |Не очень',
                    '      |е имя, состо|             | длинное',
                    '      |ящее из боль|             | названи',
                    '      |шого количес|             |   е    ',
                    '      |тва символов|             |        ',
                    '      |     .      |             |        ', ]
        self.assertEqual(expected, diff.get_row(lines, "prod"))
        expected = [' dev /|Очень длинно|     100     |Не очень',
                    '      |е имя, состо|             | длинное',
                    '      |ящее из боль|             | названи',
                    '      |шого количес|             |   е    ',
                    '      |тва символов|             |        ',
                    '      |     .      |             |        ', ]
        self.assertEqual(expected, diff.get_row(lines, "dev"))

    def test_get_rest(self):
        """
            Проверяем функцию get_rest
        """

        expected = [
            'DEV table containes more rows.',
            '---------------------------------',
            ' dev /| Винни Пух  |    5678     ',
            '---------------------------------',
            ' dev /|Иван Иванови|    9876     ',
            '      |  ч Иванов  |             ',
            '---------------------------------'
        ]
        diff = DiffBig(self.prod_rows, self.dev_rows, OrderedDict({"person_name": 12, "manager_code": 13}))
        width = sum(value for key, value in diff.columns.items()) + len(diff.columns) - 1 + 7
        diff.sep = '-' * width
        diff.big_sep = '_' * width
        self.assertEqual(expected, diff.get_rest())

    def test_get_body(self):
        """
            Проверяем основную функцию -- get_body
        """
        expected = [
            ' env /|person_name |manager_code ',
            '_________________________________',
            'prod /|  Карлсон   |    1234     ',
            '---------------------------------',
            ' dev /|    Карл    |    1234     ',
            '_________________________________',
            'DEV table containes more rows.',
            '---------------------------------',
            ' dev /| Винни Пух  |    5678     ',
            '---------------------------------',
            ' dev /|Иван Иванови|    9876     ',
            '      |  ч Иванов  |             ',
            '---------------------------------']
        diff = DiffBig(self.prod_rows, self.dev_rows, OrderedDict({"person_name": 12, "manager_code": 13}))
        self.assertEqual(expected, diff.get_body())
