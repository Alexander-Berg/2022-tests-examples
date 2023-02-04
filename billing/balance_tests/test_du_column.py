# -*- coding: utf-8 -*-
import pytest

from butils.decimal_unit import DecimalUnit, UnitsError, Units

from tests import object_builder
from tests import base


class ItemForTests(object):
    u"""
    Класс объекта, элемент данных для теста.
    """
    def __init__(self, mapper_object, du_column):
        u"""
        :param mapper_object: Объект меппер, которому принадлежит колонка.
        :param du_column: Колонка типа DecimalUnit из меппер объекта.
        """
        self.mapper_object = mapper_object
        self.du_column = du_column

        self.expected_units = None
        self.du_attribute = None
        self.write_check = None

        if mapper_object is not None and du_column is not None:
            unum, uden = self.du_column.get_units(self.mapper_object)
            self.expected_units = Units(unum, uden)
            self.du_attribute = getattr(self.mapper_object, self.du_column.name)
            self.write_check = self.du_column.write_check


@pytest.fixture(scope="module")
def get_test_item(modular_session):
    u"""
    Возвращает функцию, создающую элемент данных для тестов
    из пути к колонке типа DecimalUnit.
    """
    source_object = object_builder.InvoiceBuilder().build(modular_session).obj

    def find_mapper_object(mapper_path):
        mapper_object = source_object
        for path_element in mapper_path:
            if hasattr(mapper_object, "__getitem__") and path_element.isdigit():
                index = int(path_element)
                if index < len(mapper_object):
                    mapper_object = mapper_object[index]
                else:
                    mapper_object = None
            elif hasattr(mapper_object, path_element):
                mapper_object = getattr(mapper_object, path_element)
            else:
                mapper_object = None
            if mapper_object is None:
                break
        return mapper_object

    def wrapped(du_column_path):
        path_elements = list(filter(len, du_column_path.split(".")))
        mapper_path = path_elements[:-1]
        column_name = path_elements[-1]
        mapper_object = find_mapper_object(mapper_path)
        du_column = None
        if mapper_object is not None:
            du_column = mapper_object.__table__.columns[column_name]
        return ItemForTests(mapper_object, du_column)

    return wrapped


du_column_paths = [
    ".total_sum",
    ".receipt_sum_1c",
    ".paysys.nds_pct",
    ".effective_sum",
    ".request.agency_discount_pct",
    ".invoice_orders.0.order.consume_qty",
]


class TestDUColumn(object):
    @pytest.mark.parametrize('du_column_path', du_column_paths)
    def test_return_du(self, get_test_item, du_column_path):
        u"""
        Проверяем, что тип возвращаемого объекта из атрибута DUColumn: DecimalUnit.
        """
        test_item = get_test_item(du_column_path)
        assert isinstance(test_item.du_attribute, DecimalUnit)

    @pytest.mark.parametrize('du_column_path', du_column_paths)
    def test_correct_du_units(self, get_test_item, du_column_path):
        u"""
        Проверяем, что у возвращаемого объекта верные единицы измерения.
        """
        test_item = get_test_item(du_column_path)
        assert test_item.expected_units.matched_units(test_item.du_attribute)

    @pytest.mark.parametrize('du_column_path', du_column_paths)
    def test_write_check_true(self, get_test_item, du_column_path):
        u"""
        Проверяем, что если у колонки атрибут write_check = True то нельзя записать значения в других единицах измерения.
        При попытке записать значение с другими единицами измерения должно выбрасываться исключение UnitsError.
        Если у колонки атрибут write_check = False, при записи значения единиц не проверяется.
        """
        test_item = get_test_item(du_column_path)
        test_du_value = DecimalUnit(696, 'SomeRareUnit')
        if test_item.write_check:
            # Не смогли записать значения, выкинули исключение
            with pytest.raises(UnitsError):
                setattr(test_item.mapper_object, test_item.du_column.name, test_du_value)
                pytest.fail(u"Operation with wrong units, UnitsError exception expect.")
        else:
            # Записали значения, проигнорировав единицы измерения, значения совпадают
            setattr(test_item.mapper_object, test_item.du_column.name, test_du_value)
            assert test_item.du_attribute.as_decimal() == test_du_value.as_decimal()
