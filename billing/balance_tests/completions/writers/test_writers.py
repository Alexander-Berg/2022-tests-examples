# -*- coding: utf-8 -*-
from datetime import datetime

import pytest
from sqlalchemy import text

from balance.completions_fetcher.configurable_partner_completion import GenericWriter, GenericClassWriter
from balance import mapper


@pytest.mark.parametrize('table, fields, expected', [
    [
        'bo.test', ['id', 'name', 'dt'], text('insert  into bo.test (id, name, dt) values (:id, :name, :dt)')
    ],
])
def test_writer(table, fields, expected):
    writer = GenericWriter(
        table=table, fields=fields, session=None
    )
    actual = writer.generate_sql()
    assert str(actual) == str(expected)


class TestGenericClassWriter(object):
    class ClassModuleContext(object):
        def __init__(self, cls, module):
            self.cls = cls
            self.module = module

        def __enter__(self):
            setattr(self.module, self.cls.__name__, self.cls)

        def __exit__(self, type, value, traceback):
            delattr(self.module, self.cls.__name__)

    @pytest.fixture()
    def test_class(self):
        class TestModel(object):
            def __init__(self, id=None, name=None, dt=None):
                self.id = id
                self.name = name
                self.dt = dt

            def __eq__(self, other):
                return self.id == other.id and self.name == other.name and self.dt == other.dt

        return TestModel

    @pytest.mark.parametrize('fields', [
        [
            ['id', 'name', 'dt']
        ],
    ])
    def test_correct_class(self, fields, test_class):
        with self.ClassModuleContext(cls=test_class, module=mapper):
            writer = GenericClassWriter(
                model_class=test_class.__name__, fields=fields, session=None
            )
            assert writer.model_cls == test_class

    @pytest.mark.parametrize('fields', [
        [
            ['id', 'name', 'dt']
        ],
    ])
    def test_incorrect_class(self, fields, test_class):
        with pytest.raises(ValueError):
            GenericClassWriter(
                model_class=test_class.__name__, fields=fields, session=None
            )

    @pytest.mark.parametrize('fields, data', [
        [
            ['id', 'name', 'dt'],
            [{'id': 1, 'name': 'name', 'dt': datetime.strptime("2022-05-19 13:08", "%Y-%m-%d %H:%M")}],
        ],
    ])
    def test_processing(self, fields, data, test_class):
        with self.ClassModuleContext(cls=test_class, module=mapper):
            writer = GenericClassWriter(
                model_class=test_class.__name__, fields=fields, session=None
            )
            expected = [test_class(**row) for row in data]

            assert writer.model_cls == test_class
            assert writer._get_class_objects(data) == expected
