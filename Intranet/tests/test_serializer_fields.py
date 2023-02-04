import datetime
from builtins import object

import pytest
import pytz
from mock import MagicMock, call

from django.db import models

from rest_framework import serializers
from rest_framework.exceptions import ValidationError

from kelvin.common.serializer_fields import (
    JSONField, MicrosecondsDateTimeField, NestedForeignKeyField, NestedManyToManyField, UnixTimeField,
)
from kelvin.common.tests.utils import get_model_class


class TestNestedForeignKeyField(object):
    """
    Тесты поля для сериализации "foreign key"-модели
    """

    def test_to_representation(self):
        """Тест сериализации"""
        serializer_class = MagicMock()
        expected = MagicMock()
        serializer_class.return_value.to_representation.return_value = expected

        # наследуем поле
        class CustomForeignKeyField(NestedForeignKeyField):
            class Meta(object):
                serializer = serializer_class
                model = MagicMock()

        serialized_data = CustomForeignKeyField().to_representation('data')
        assert serializer_class.mock_calls == [
            call(context={}), call().to_representation('data')], (
            u'Сериализация проходит через указанный сериализатор')
        assert serialized_data == expected, (
            u'Возвращается значение из сериализатора')

    def test_to_internal_value(self):
        """Тест десериализации"""
        model_class = MagicMock()

        # наследуем поле
        class CustomForeignKeyField(NestedForeignKeyField):
            class Meta(object):
                serializer = MagicMock()
                model = model_class

        # объект есть в базе и данные правильные
        model_class.objects.get.return_value = 'internal_value'
        assert (CustomForeignKeyField().to_internal_value('data') ==
                'internal_value'), u'Должен вернуться объект из `get`'
        assert model_class.mock_calls == [call.objects.get(id='data')], (
            u'Должен быть запрос за объектом')

        model_class.reset_mock()
        assert (CustomForeignKeyField().to_internal_value({'id': 'data'}) ==
                'internal_value'), u'Должен вернуться объект из `get`'
        assert model_class.mock_calls == [call.objects.get(id='data')], (
            u'Должен быть запрос за объектом')

        # нет идентификатора в словаре
        model_class.reset_mock()
        with pytest.raises(ValidationError) as excinfo:
            CustomForeignKeyField().to_internal_value({'key': 'data'})
        assert excinfo.value.detail == [
            "ID attribute wasn't found for None in {'key': 'data'}"], (
            u'Неправильное сообщение об ошибке')
        assert model_class.mock_calls == [], u'Не должно быть запроса'

        # неправильный идентификатор
        model_class.reset_mock()
        model_class.objects.get.side_effect = ValueError
        with pytest.raises(ValidationError) as excinfo:
            CustomForeignKeyField().to_internal_value('data')
        assert excinfo.value.detail == ['bad value in None data: data'], (
            u'Неправильное сообщение об ошибке')
        assert model_class.mock_calls == [call.objects.get(id='data')], (
            u'Должен быть запрос за объектом')

        # несуществующий объект
        model_class.reset_mock()
        model_class.DoesNotExist = Exception
        model_class.objects.get.side_effect = model_class.DoesNotExist
        with pytest.raises(ValidationError) as excinfo:
            CustomForeignKeyField().to_internal_value('data')
        assert excinfo.value.detail == [
            "Some objects for None doesn't exist: data"], (
            u'Неправильное сообщение об ошибке')
        assert model_class.mock_calls == [call.objects.get(id='data')], (
            u'Должен быть запрос за объектом')


class TestNestedManyToManyField(object):
    """
    Тест поля группы `TestNestedManyToManyField`
    """
    to_representation_cases = [
        (
            ('name',),
            [{'id': 1, 'name': u'Имя'}],
            [{'id': 1, 'name': u'Имя'}],
        ),
        (
            ('name',),
            [{'name': 'Name'}],
            [{'id': None, 'name': 'Name'}],
        ),
        (
            ('name', 'additional'),
            [
                {'id': 3, 'name': u'Имя', 'additional': 'info'},
                {'id': 4, 'name': u'Имя2'},
            ],
            [
                {'id': 3, 'additional': 'info', 'name': u'Имя'},
                {'id': 4, 'name': u'Имя2', 'additional': ''},
            ],
        ),
        (
            ('name', 'additional'),
            [],
            [],
        ),
    ]
    to_representation_cases_with_meta_fields = [
        (
            ('name', 'additional'),
            ['name', 'additional'],
            [
                {'id': 3, 'name': u'Имя', 'additional': 'info'},
                {'id': 4, 'name': u'Имя2'},
            ],
            [
                {'additional': 'info', 'name': u'Имя'},
                {'name': u'Имя2', 'additional': ''},
            ],
        ),
        (
            ('name', 'additional'),
            ['additional'],
            [
                {'id': 3, 'name': u'Имя', 'additional': 'info'},
                {'id': 4, 'name': u'Имя2'},
            ],
            [
                {'additional': 'info'},
                {'additional': ''},
            ],
        ),
        (
            ('name', 'additional'),
            ['additional'],
            [],
            [],
        ),
    ]

    @pytest.mark.parametrize("fields,params,expected",
                             to_representation_cases)
    def test_to_representation(self, fields, params, expected):
        """
        Тест сериализации поля, когда конкретные поля для сериализации
        не указаны в сериализаторе, указанном в `Meta`
        """
        MyModel = get_model_class(
            {field_name: models.Field() for field_name in fields},
        )

        # определяем класс сериализатора
        class CustomSerializer(serializers.ModelSerializer):
            class Meta(object):
                model = MyModel
                fields = '__all__'

        # наследуем поле
        class CustomManyToManyField(NestedManyToManyField):
            class Meta(object):
                serializer = CustomSerializer
                model = MyModel

        instances = []
        for param in params:
            instances.append(MyModel(**param))

        serialized_data = CustomManyToManyField().to_representation(
            instances)
        assert [dict(data) for data in serialized_data] == expected

    @pytest.mark.parametrize("fields,meta_fields,params,expected",
                             to_representation_cases_with_meta_fields)
    def test_to_representation_with_meta_fields(
            self, fields, meta_fields, params, expected):
        """
        Тест сериализации поля, когда конкретные поля для сериализации
        указаны в сериализаторе, указанном в `Meta`
        """
        # определяем класс модели с заданными полями
        attrs = {'__module__': '.'.join(__name__.split('.')[0:2])}
        MyModel = get_model_class(
            {field_name: models.Field() for field_name in fields}
        )

        # определеяем класс сериализатора
        class CustomSerializer(serializers.ModelSerializer):
            class Meta(object):
                model = MyModel
                fields = meta_fields

        # наследуем поле
        class CustomManyToManyField(NestedManyToManyField):
            class Meta(object):
                serializer = CustomSerializer
                model = MyModel

        instances = []
        for param in params:
            instances.append(MyModel(**param))

        serialized_data = CustomManyToManyField().to_representation(
            instances)
        assert [dict(data) for data in serialized_data] == expected

    to_internal_value_positive = [
        ((), [1], [1]),
        (
            ('name',),
            [{'id': 1, 'name': 'Test'}],
            [1],
        ),
        (
            ('name',),
            [{'id': 1, 'additional': 'info'}],
            [1],
        ),
        (
            ('name', 'additional'),
            [1, 2],
            [1, 2],
        ),
    ]

    to_internal_value_negative = [
        (
            [2],
            u'Some objects for custom doesn\'t exist: {2}'),
        (
            [{'id': 2, 'name': 'Test'}],
            u'Some objects for custom doesn\'t exist: {2}',
        ),
        (
            [{'additional': 'info'}],
            u'ID attribute wasn\'t found for custom in {\'additional\': '
            u'\'info\'}',
        ),
        (
            ['not_id'],
            u'bad value in custom data: not_id',
        ),
        (
            {'id': 1},
            u'custom field requires list value',
        ),
    ]

    @pytest.mark.parametrize("fields,value,expected",
                             to_internal_value_positive)
    def test_to_internal_value_positive(self, mocker, fields, value,
                                        expected):
        """
        Тест десериализации поля, положительные случаи
        """
        # определяем класс модели с заданными полями
        attrs = {'__module__': '.'.join(__name__.split('.')[0:2])}
        MyModel = get_model_class(
            {field_name: models.Field() for field_name in fields},
        )

        # определеяем класс сериализатора
        class CustomSerializer(serializers.ModelSerializer):
            class Meta(object):
                model = MyModel

        # наследуем поле
        class CustomManyToManyField(NestedManyToManyField):
            class Meta(object):
                serializer = CustomSerializer
                model = MyModel

        mocked = mocker.patch.object(MyModel, 'objects')
        mocked.filter.return_value.values_list.return_value = expected

        deserialized = CustomManyToManyField().to_internal_value(value)
        assert expected == deserialized
        assert mocked.method_calls == [call.filter(id__in=expected)]

    @pytest.mark.parametrize("value,errmsg", to_internal_value_negative)
    def test_to_internal_value_negative(self, mocker, value, errmsg):
        """
        Тест десерализации поля, отрицательные случаи
        """
        MyModel = get_model_class()

        # определеяем класс сериализатора
        class CustomSerializer(serializers.ModelSerializer):
            class Meta(object):
                model = MyModel

        # наследуем поле
        class CustomManyToManyField(NestedManyToManyField):
            class Meta(object):
                serializer = CustomSerializer
                model = MyModel

        mocked = mocker.patch.object(MyModel, 'objects')
        mocked.filter.return_value.values_list.return_value = [1]

        with pytest.raises(serializers.ValidationError) as excinfo:
            CustomManyToManyField(label=u'custom').to_internal_value(value)
        print("detail=", excinfo.value.detail)
        assert excinfo.value.detail == [errmsg]

    def test_wrong_field_usage(self):
        """
        Тест на неверное использование поля
        """
        # не указана модель
        class MyField(NestedManyToManyField):
            class Meta(object):
                serializer = serializers.Field

        with pytest.raises(AssertionError) as excinfo:
            MyField()
        print(excinfo.value)
        assert str(excinfo.value) == 'Model must be specified in Meta'

        class MyField(NestedManyToManyField):
            class Meta(object):
                serializer = serializers.Field
                model = None

        with pytest.raises(AssertionError) as excinfo:
            MyField()
        assert str(excinfo.value) == 'Model must be specified in Meta'

        # не указан сериализатор
        class MyField(NestedManyToManyField):
            class Meta(object):
                model = models.Model

        with pytest.raises(AssertionError) as excinfo:
            MyField()

        assert str(excinfo.value) == 'Serializer class must be specified in Meta'

        class MyField(NestedManyToManyField):
            class Meta(object):
                model = models.Model
                serializer = None

        with pytest.raises(AssertionError) as excinfo:
            MyField()

        assert str(excinfo.value) == u'Serializer class must be specified in Meta'


class TestUnixTimeField(object):
    """
    Тесты `UnixTimeField`
    """
    to_representation_cases = (
        (datetime.datetime(1970, 1, 1, 0, 2, 2), 122),
        (datetime.datetime(1980, 2, 1, 0, 1), 318211260),
        (datetime.datetime(1960, 2, 1, 0, 1), -312940740),
        (datetime.datetime(2015, 6, 15, 2, 3), 1434333780),
    )

    @pytest.mark.parametrize("to_represent, expected", to_representation_cases)
    def test_to_representation(self, to_represent, expected):
        """
        Тесты представления времени в виде unixtime
        """
        represented = UnixTimeField().to_representation(to_represent)

        assert represented == expected, (
            u"Неверное представление даты-времени. Ожидалось {0}, получено {1}"
            .format(expected, represented)
        )

    to_internal_value_positive_cases = (
        (1, datetime.datetime(1970, 1, 1, 0, 0, 1)),
        (-1, datetime.datetime(1969, 12, 31, 23, 59, 59)),
        (3142856, datetime.datetime(1970, 2, 6, 9, 0, 56)),
    )
    to_internal_value_negative_cases = (
        'string',
        {'di': 'ct'},
        ['list'],
        -900000000000,
    )

    @pytest.mark.parametrize("to_internal,expected",
                             to_internal_value_positive_cases)
    def test_to_internal_value_positive(self, to_internal, expected):
        """
        Тесты десериализации времени, положительные случаи
        """
        internal = UnixTimeField().to_internal_value(to_internal)

        assert internal == expected, (
            u"Неверное представление даты-времени. Ожидалось {0}, получено {1}"
            .format(expected, internal)
        )

    @pytest.mark.parametrize("to_internal", to_internal_value_negative_cases)
    def test_to_internal_value_negative(self, to_internal):
        """
        Тесты десериализации времени, отрицательные случаи
        """
        with pytest.raises(serializers.ValidationError) as excinfo:
            UnixTimeField().to_internal_value(to_internal)
        assert excinfo.value.detail == [
            "Incorrect unixtime: {0}".format(to_internal)], (
            u"Неверное сообщение об ошибке"
        )


class TestMicrosecondsDateTimeField(object):
    """
    Тесты `MicrosecondsDateTimeField`
    """
    to_representation_cases = (
        (
            datetime.datetime(1970, 1, 1, 0, 2, 2, 1, tzinfo=pytz.utc),
            122000001,
        ),
        (
            datetime.datetime(1969, 12, 31, 23, 59, 59, 999999,
                              tzinfo=pytz.utc),
            -1,
        ),
        (
            datetime.datetime(1970, 2, 6, 9, 0, 56, 333943, tzinfo=pytz.utc),
            3142856333943,
        ),
    )

    @pytest.mark.parametrize("to_represent, expected", to_representation_cases)
    def test_to_representation(self, to_represent, expected):
        """
        Тесты представления времени в виде микросекунд
        """
        represented = MicrosecondsDateTimeField().to_representation(
            to_represent)

        assert represented == expected, (
            u"Неверное представление даты-времени. Ожидалось {0}, получено {1}"
            .format(expected, represented)
        )

    to_internal_value_positive_cases = (
        (
            1000001,
            datetime.datetime(1970, 1, 1, 0, 0, 1, 1, tzinfo=pytz.utc),
        ),
        (
            -1,
            datetime.datetime(1969, 12, 31, 23, 59, 59, 999999,
                              tzinfo=pytz.utc),
        ),
        (
            3142856333943,
            datetime.datetime(1970, 2, 6, 9, 0, 56, 333943, tzinfo=pytz.utc),
        ),
    )
    to_internal_value_negative_cases = (
        'string',
        {'di': 'ct'},
        ['list'],
        -900000000000000000,
    )

    @pytest.mark.parametrize("to_internal,expected",
                             to_internal_value_positive_cases)
    def test_to_internal_value_positive(self, to_internal, expected):
        """
        Тесты десериализации времени, положительные случаи
        """
        internal = MicrosecondsDateTimeField().to_internal_value(to_internal)

        assert internal == expected, (
            u"Неверное представление даты-времени. Ожидалось {0}, получено {1}"
            .format(expected, internal)
        )

    @pytest.mark.parametrize("to_internal", to_internal_value_negative_cases)
    def test_to_internal_value_negative(self, to_internal):
        """
        Тесты десериализации времени, отрицательные случаи
        """
        with pytest.raises(serializers.ValidationError) as excinfo:
            MicrosecondsDateTimeField().to_internal_value(to_internal)
        assert excinfo.value.detail == [
            "Incorrect microseconds: {0}".format(to_internal)], (
            u"Неверное сообщение об ошибке"
        )


class TestJSONField(object):
    """
    Тесты поля разметки задачи
    """
    init_cases = (
        ({'schema': {'type': 'object'}}, {'type': 'object'}),
        ({'read_only': True}, None),
        ({'allow_null': False}, None),
        ({'allow_null': True}, None),
        ({'schema': {'type': 'object'}, 'write_only': False},
         {'type': 'object'}),
    )

    @pytest.mark.parametrize('init,expected_schema', init_cases)
    def test__init(self, mocker, init, expected_schema):
        """
        Тест инициализации класса
        """
        mocked_init = mocker.patch.object(serializers.Field, '__init__')
        mocked_init.return_value = True

        field = JSONField(**init)

        assert field.json_schema == expected_schema, u'Неправильная схема'
        init.pop('schema', None)
        if 'allow_null' in init:
            assert mocked_init.mock_calls == [
                call(**init),
            ], u'При указании явного `allow_null` нужно использовать его'
        else:
            assert mocked_init.mock_calls == [
                call(allow_null=True, **init),
            ], u'По умолчанию нужно принимать null как валидный json'

    def test__init__subclass(self):
        """
        Тест инициализации при наследовании
        """
        class MyField(JSONField):
            json_schema = {'type': 'object'}

        field = MyField()
        assert field.json_schema == {'type': 'object'}, (
            u'При наследовании не переопределилось поле json_schema')

        field = MyField(schema={'type': 'string'})
        assert field.json_schema == {'type': 'string'}, (
            u'Не проставилось значение, заданное при инициализции')

    markup_to_representation = (
        ({}, {}),
        ({'markers': {"a": "ddd"}}, {'markers': {"a": "ddd"}}),
        ({'hit_areas': [{"a": "ddd"}, 3]}, {'hit_areas': [{"a": "ddd"}, 3]}),
        (
            {
                'task_answers': '[2, 3, 3.5]',
                'description': u'Задача',
            },
            {
                'task_answers': '[2, 3, 3.5]',
                'description': u'Задача',
            },
        ),
    )

    @pytest.mark.parametrize('markup,expected', markup_to_representation)
    def test_to_representation(self, markup, expected):
        """
        Тесты сериализации поля разметки задачи
        """
        serialized_markup = JSONField().to_representation(markup)
        assert serialized_markup == expected

    markup_to_internal_value = (
        ({}, {}),
        ({'a': 5}, {'a': 5}),
        (
            {
                'markers': ['b', 'c', 5],
                'hit_areas': [[0, 1], [2, 5.5]],
            },
            {
                'markers': ['b', 'c', 5],
                'hit_areas': [[0, 1], [2, 5.5]],
            },
        ),
        (
            {
                'description': (
                    u'Вставьте пропущенные буквы и знаки препинания'),
                'task_answers': ['b', ['3', 2], 5],
                'markers': [
                    {
                        "id": "1",
                        "answer": "о",
                        "type": "field",
                        "params": {
                            "answer_display": "старичок",
                        },
                        "type_display": "inline",
                    },
                    {
                        "id": "2",
                        "answer": "е",
                        "type": "field",
                        "params": {
                            "answer_display": "мундире",
                        },
                        "type_display": "inline",
                    },
                ],
            },
            {
                'description': (
                    u'Вставьте пропущенные буквы и знаки препинания'),
                'task_answers': ['b', ['3', 2], 5],
                'markers': [
                    {
                        "id": "1",
                        "answer": "о",
                        "type": "field",
                        "params": {
                            "answer_display": "старичок",
                        },
                        "type_display": "inline",
                    },
                    {
                        "id": "2",
                        "answer": "е",
                        "type": "field",
                        "params": {
                            "answer_display": "мундире",
                        },
                        "type_display": "inline",
                    },
                ],
            },
        ),
    )

    @pytest.mark.parametrize('initial,expected', markup_to_representation)
    def test_to_internal_value(self, initial, expected):
        """
        Тест десериализации поля разметки задачи, схема не указана
        """
        markup = JSONField().to_internal_value(initial)
        assert markup == expected

    def test_to_internal_value_with_schema(self):
        """
        Тест десериализации поля с указанием json-схемы
        """
        data = {
            'type': 'content',
            'owner': 1,
            'data': [10, 'c', 30],
        }

        # данные валидные
        markup = JSONField(schema={'type': 'object'}).to_internal_value(data)
        assert markup == data

        # данные не валидные
        with pytest.raises(serializers.ValidationError) as exinfo:
            JSONField(schema={'type': 'object'}).to_internal_value([10, 20])

        assert exinfo.value.detail == ["[10, 20] is not of type 'object'"]
