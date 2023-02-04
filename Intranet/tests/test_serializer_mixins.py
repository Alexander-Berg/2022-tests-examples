from builtins import object

import pytest
from mock import MagicMock

from django.db import models
from django.db.models.fields.related_descriptors import ReverseManyToOneDescriptor

from rest_framework import fields, serializers

from kelvin.common.serializer_fields import MicrosecondsDateTimeField, UnixTimeField
from kelvin.common.serializer_mixins import (
    DateUpdatedFieldMixin, ExcludeForStudentMixin, PreciseDateUpdatedFieldMixin, SerializerForeignKeyMixin,
    SerializerManyToManyMixin, SetFieldsMixin, SkipNullsMixin,
)
from kelvin.common.tests.utils import build_model_class_name, get_model_class


class TestSetFieldsMixin(object):
    """
    Тест миксина `SetFieldsMixin`
    """

    def test_init(self):
        """
        Проверяем, что можем инициализировать сериализатор с подмножеством
        полей сериализатора
        """
        cases = [
            {
                'model_fields': ['id', 'name', 'address'],
                'need_fields': None,
                'expected_fields': ['id', 'name', 'address'],
            },
            {
                'model_fields': ['id', 'name', 'address'],
                'need_fields': [],
                'expected_fields': ['id', 'name', 'address'],
            },
            {
                'model_fields': ['id', 'name', 'address'],
                'need_fields': ['id', 'name'],
                'expected_fields': ['id', 'name'],
            },
            {
                'model_fields': ['id', 'name'],
                'need_fields': ['id', 'url'],
                'expected_fields': ['id'],
            }
        ]

        for case in cases:
            MyModel = get_model_class(
                {
                    field_name: models.Field()
                    for field_name in case['model_fields']
                }
            )

            class MyModelSerializer(SetFieldsMixin,
                                    serializers.ModelSerializer):
                class Meta(object):
                    model = MyModel
                    fields = '__all__'

            # инициализируем сериализатор
            if case['need_fields'] is None:
                serializer = MyModelSerializer()
            else:
                serializer = MyModelSerializer(fields=case['need_fields'])

            # проверяем набор полей
            assert (set(serializer.fields.keys()) ==
                    set(case['expected_fields'])), (
                u'Неправильный набор сериализуемых полей')


class TestSkipNullsMixin(object):
    """
    Тесты миксина `SkipNullsMixin`
    """

    def test_to_representation(self):
        """
        Проверяем, что `to_representation` опускает незначимые поля
        """
        # определяем свой базовый класс для проверки
        class CustomBaseSerializer(object):
            called = False

            def to_representation(self, instance):
                self.called = True
                return instance

        class CustomSerializer(SkipNullsMixin, CustomBaseSerializer):
            pass

        cases = (
            {
                'initial': {
                    'id': 1,
                    'a': u'данные',
                    'b': None,
                    'c': False,
                    'd': [],
                    'e': 0,
                },
                'expected': {
                    'id': 1,
                    'a': u'данные',
                    'c': False,
                    'd': [],
                    'e': 0,
                },
            },
            {
                'initial': {
                    'id': 2,
                    'a': None,
                    'b': None,
                },
                'expected': {
                    'id': 2,
                },
            },
            {
                'initial': {
                    'id': None,
                    'a': None,
                    'b': None,
                },
                'expected': {},
            },
        )

        for case in cases:
            serializer = CustomSerializer()
            serialized_data = serializer.to_representation(case['initial'])

            assert serialized_data == case['expected'], (
                u'Неправильно удалены ключи с `None`')
            assert serializer.called, (
                u'Не вызван родительский метод `to_representation`')


class TestDateUpdatedFieldMixin(object):
    """
    Тест миксина для представления времени обновления в unixtime
    """

    def test_build_field(self, mocker):
        """
        Тест создания поля. Проверяет, что класс поля, созданный родительским
        методом, заменяется на UnixTimeField только в случае, когда имя поля -
        `date_updated`
        """
        # Создаем псевдокласс-сериализатор псевдомодели с миксином и моки
        class MySerializer(DateUpdatedFieldMixin, serializers.ModelSerializer):
            class Meta(object):
                model = get_model_class()
                fields = '__all__'

        serializer = MySerializer()

        mocked_build_field = mocker.patch.object(serializers.ModelSerializer,
                                                 'build_field')
        mocked_build_field.return_value = (
            serializers.DateTimeField,
            {},
        )

        # сценарий первый - генерация произвольного поля идет без изменений
        field_class, field_kwargs = serializer.build_field(
            'fld', 1, 2, 3)
        assert field_class != UnixTimeField, (
            u"Произошла замена класса на UnixTimeField у поля с названием fld")
        assert mocked_build_field.called, (
            u"Не было вызова родительского build_field")

        # сценарий второй - замена у поля с названием date_updated
        mocked_build_field.reset_mock()
        field_class, field_kwargs = serializer.build_field(
            'date_updated', 1, 2, 3)
        assert field_class == UnixTimeField, (
            u"Класс поля с названием date_updated должен быть UnixTimeField")
        assert mocked_build_field.called, (
            u"Не было вызова родительского build_field")


class TestPreciseDateUpdatedFieldMixin(object):
    """
    Тест миксина для представления времени обновления в микросекундах
    """

    def test_build_field(self, mocker):
        """
        Тест создания поля. Проверяет, что класс поля, созданный родительским
        методом, заменяется на `MicrosecondsDateTimeField` только в случае,
        когда имя поля - `date_updated`
        """
        class MySerializer(PreciseDateUpdatedFieldMixin,
                           serializers.ModelSerializer):
            class Meta(object):
                model = get_model_class()
                fields = '__all__'

        serializer = MySerializer()

        mocked_build_field = mocker.patch.object(serializers.ModelSerializer,
                                                 'build_field')
        mocked_build_field.return_value = (
            serializers.DateTimeField,
            {},
        )

        # сценарий первый - генерация произвольного поля идет без изменений
        field_class, field_kwargs = serializer.build_field(
            'fld', 1, 2, 3)
        assert field_class != MicrosecondsDateTimeField, (
            u"Произошла замена класса на UnixTimeField у поля с названием fld")
        assert mocked_build_field.called, (
            u"Не было вызова родительского build_field")

        # сценарий второй - замена у поля с названием date_updated
        mocked_build_field.reset_mock()
        field_class, field_kwargs = serializer.build_field(
            'date_updated', 1, 2, 3)
        assert field_class == MicrosecondsDateTimeField, (
            u"Класс поля с названием date_updated должен быть UnixTimeField")
        assert mocked_build_field.called, (
            u"Не было вызова родительского build_field")


class TestSerializerForeignKeyMixin(object):
    """
    Тесты миксина `SerializerForeignKeyMixin`
    """
    @pytest.fixture
    def serializer_fixture(self):
        """
        Создает два класса сериализаторов двух моделей, один вложен в другой.
        К основному добавлен `SerializerForeignKeyMixin`, в `fk_update_fields`
        указано поле вложенной модели . Возвращает классы моделей и инстансы их
        сериализаторов
        """
        ModelA = get_model_class({'number': models.IntegerField()})
        ModelB = get_model_class({
            'notnumber': models.CharField(),
            'model_a': models.ForeignKey(ModelA, null=True)
        })

        class NestedSerializer(serializers.ModelSerializer):

            class Meta(object):
                model = ModelA
                fields = '__all__'

        class PrimarySerializer(SerializerForeignKeyMixin,
                                serializers.ModelSerializer):
            model_a = NestedSerializer()

            fk_update_fields = ['model_a', 'name_without_field']

            class Meta(object):
                model = ModelB
                fields = '__all__'

        return ModelA, ModelB, NestedSerializer(), PrimarySerializer()

    def test_get_fields(self, serializer_fixture):
        """
        Тест проставления `partial` в дочернем поле
        """
        modelA, modelB, nested, primary = serializer_fixture

        class GoodSerializer(SerializerForeignKeyMixin,
                             serializers.ModelSerializer):
            fk = nested
            fk_update_fields = ['fk']

            class Meta(object):
                model = modelA
                fields = '__all__'

        class WrongFieldSerializer(GoodSerializer):
            fk = fields.IntegerField()

        class NoFkListSerializer(GoodSerializer):
            fk_update_fields = ['not_fk']

        try:
            good = GoodSerializer()
        except AssertionError as e:
            pytest.fail(u"Возникла ошибка при создании валидного "
                        u"сериализатора: {0}".format(e.message))

        assert good.fields['fk'].partial is False

        try:
            good = GoodSerializer(partial=True)
        except AssertionError as e:
            pytest.fail(u"Возникла ошибка при создании валидного "
                        u"сериализатора: {0}".format(e.message))

        assert good.fields['fk'].partial is True

        # отсутствующие поля игнорируются (поля могут меняться от запроса к
        # запросу)
        try:
            NoFkListSerializer().fields
        except AssertionError as e:
            pytest.fail(u"Возникла ошибка при создании валидного "
                        u"сериализатора: {0}".format(e.message))

        with pytest.raises(AssertionError) as excinfo:
            WrongFieldSerializer().fields

        print(excinfo.value)
        assert str(excinfo.value) == ("Cannot use `ForeignKeyMixin` with field "
                                   "'fk', which is not an instance of "
                                   "`ModelSerializer`")

    def test_init(self, serializer_fixture):
        """
        Тест инициализации
        """
        modelA, modelB, nested, primary = serializer_fixture

        class GoodSerializer(SerializerForeignKeyMixin,
                             serializers.ModelSerializer):
            fk = nested
            fk_update_fields = ['fk']

            class Meta(object):
                model = modelA
                fields = '__all__'

        class NoFkUpdateSerializer(GoodSerializer):
            fk_update_fields = []

        class NoFkListSerializer(GoodSerializer):
            fk_update_fields = ['not_fk']

        class NotModelSerializer(SerializerForeignKeyMixin):
            pass

        # отсутствующие поля игнорируются (поля могут меняться от запроса к
        # запросу)
        try:
            NoFkListSerializer()
        except AssertionError as e:
            pytest.fail(u"Возникла ошибка при создании валидного "
                        u"сериализатора: {0}".format(e.message))

        with pytest.raises(AssertionError) as excinfo:
            NoFkUpdateSerializer()
        assert str(excinfo.value) == ("`fk_update_fields` was not set in "
                                      "`NoFkUpdateSerializer`")

        with pytest.raises(AssertionError) as excinfo:
            NotModelSerializer()
        assert str(excinfo.value) == ("SerializerForeignKeyMixin must be used "
                                      "with a class inheriting from "
                                      "`ModelSerializer`")

    def test_save(self, mocker, serializer_fixture):
        """
        Различные сценарии работы `save`
        """
        mocked_atomic = mocker.patch(
            'kelvin.common.serializer_mixins.transaction')
        # Получаем сериализатор, мокаем сохранения вложенного и родительского
        # сериализаторов
        ModelA, ModelB, nested, primary = serializer_fixture
        primary._errors = {}
        mocked_nested_save = mocker.patch.object(
            primary.fields['model_a'], 'save')
        mocked_parent_save = mocker.patch.object(serializers.ModelSerializer,
                                                 'save')
        mocked_model_a_get = mocker.patch.object(ModelA.objects, 'get')
        validated_data = {'notnumber': 'not really a number'}

        primary._validated_data = validated_data

        # Случай объекта модели в валидированных данных
        validated_data['model_a'] = ModelA(number=1)

        primary.save()

        assert not mocked_nested_save.called, (
            u"При наличии объекта модели в данных не должно быть сохранения"
            u"вложенного сериализатора")
        assert mocked_parent_save.called, (
            u"Не было вызова родительского `save`"
        )

        # Случай создания объекта во вложенном сериализаторе
        mocked_nested_save.reset_mock()
        mocked_parent_save.reset_mock()
        validated_data['model_a'] = {'number': 4}
        mocked_nested_save.return_value = ModelA(id=1, number=4)

        primary.save()

        assert mocked_nested_save.called, (
            u"Не было вызова сохранения вложенного сериализатора")
        assert mocked_parent_save.called, (
            u"Не было вызова родительского `save`"
        )
        assert validated_data['model_a'] == mocked_nested_save.return_value, (
            u"`validated_data['model_a']` должно поменяться на значение, "
            u"возвращаемое вложенным сериализатором"
        )

        # Случай апдейта существующего объекта поля
        mocked_parent_save.reset_mock()
        mocked_nested_save.reset_mock()
        validated_data['model_a'] = {'id': 1, 'number': 2}
        primary.instance = ModelB(id=1, model_a=ModelA(id=1, number=1))
        mocked_nested_save.return_value = ModelA(id=1, number=2)
        primary.save()

        assert mocked_nested_save.called, (
            u"Не было вызова сохранения вложенного сериализатора")
        assert mocked_parent_save.called, (
            u"Не было вызова родительского `save`")
        assert primary.fields['model_a'].instance, (
            u"Нет инстанса у вложенного сериализатора"
        )
        assert primary.fields['model_a']._validated_data, (
            u"Нет валидированных данных у вложенного сериализатора")
        assert not mocked_model_a_get.called, (
            u"При обновлении с тем же id не должно быть запроса в базу ")
        assert validated_data['model_a'] == mocked_nested_save.return_value, (
            u"`validated_data['model_a']` должно поменяться на значение, "
            u"возвращаемое вложенным сериализатором"
        )

        # Случай апдейта существующего объекта поля при создании основного
        mocked_parent_save.reset_mock()
        mocked_nested_save.reset_mock()
        validated_data['model_a'] = {'id': 1, 'number': 2}
        primary.instance = None
        mocked_nested_save.return_value = ModelA(id=1, number=2)
        primary.save()

        assert mocked_nested_save.called, (
            u"Не было вызова сохранения вложенного сериализатора")
        assert mocked_parent_save.called, (
            u"Не было вызова родительского `save`")
        assert primary.fields['model_a'].instance, (
            u"Нет инстанса у вложенного сериализатора"
        )
        assert primary.fields['model_a']._validated_data, (
            u"Нет валидированных данных у вложенного сериализатора")
        assert mocked_model_a_get.called, (
            u"Должен быть запрос в базу за вложенным объектом")
        assert validated_data['model_a'] == mocked_nested_save.return_value, (
            u"`validated_data['model_a']` должно поменяться на значение, "
            u"возвращаемое вложенным сериализатором"
        )

        # Случай замены объекта в поле и одновременного его апдейта
        mocked_parent_save.reset_mock()
        mocked_nested_save.reset_mock()
        mocked_model_a_get.reset_mock()
        primary.instance = ModelB(id=1, model_a=ModelA(id=1, number=1))
        validated_data['model_a'] = {'id': 2, 'number': 3}
        mocked_model_a_get.return_value = ModelA(id=2, number=1)
        mocked_nested_save.return_value = ModelA(id=2, number=3)

        primary.save()

        assert mocked_parent_save.called, (
            u"Не было вызова родительского `save`")
        assert mocked_nested_save.called, (
            u"Не было вызова сохранения вложенного сериализатора")
        assert primary.fields['model_a'].instance, (
            u"Нет инстанса у вложенного сериализатора"
        )
        assert primary.fields['model_a']._validated_data, (
            u"Нет валидированных данных у вложенного сериализатора")
        assert mocked_model_a_get.called, (
            u"При обновлении с другим id нужно получить объект с этим id")
        assert validated_data['model_a'] == mocked_nested_save.return_value, (
            u"`validated_data['model_a']` должно поменяться на значение, "
            u"возвращаемое вложенным сериализатором"
        )

        # Случай назначаения объекта в поле и одновременного его апдейта
        mocked_parent_save.reset_mock()
        mocked_nested_save.reset_mock()
        mocked_model_a_get.reset_mock()
        primary.instance = ModelB(id=1)
        validated_data['model_a'] = {'id': 2, 'number': 3}
        mocked_model_a_get.return_value = ModelA(id=2, number=1)
        mocked_nested_save.return_value = ModelA(id=2, number=3)

        primary.save()

        assert mocked_parent_save.called, (
            u"Не было вызова родительского `save`")
        assert mocked_nested_save.called, (
            u"Не было вызова сохранения вложенного сериализатора")
        assert primary.fields['model_a'].instance, (
            u"Нет инстанса у вложенного сериализатора"
        )
        assert primary.fields['model_a']._validated_data, (
            u"Нет валидированных данных у вложенного сериализатора")
        assert mocked_model_a_get.called, (
            u"При обновлении с другим id нужно получить объект с этим id")
        assert validated_data['model_a'] == mocked_nested_save.return_value, (
            u"`validated_data['model_a']` должно поменяться на значение, "
            u"возвращаемое вложенным сериализатором"
        )

        # случай отсутствия поля в данных - не делаем ничего нестандартного
        mocked_parent_save.reset_mock()
        mocked_nested_save.reset_mock()
        mocked_model_a_get.reset_mock()
        validated_data.pop('model_a')

        primary.save()

        assert mocked_parent_save.called, (
            u"Не было вызова родительского `save`")
        assert not mocked_nested_save.called, (
            u"Не должно производиться действий с полем, если нет данных")
        assert not mocked_model_a_get.called, (
            u"Не должно быть запросов в базу, если нет данных")

    to_internal_value_default_cases = (
        [],
        {'model_c'},
        {'model_a': {}},
        {'model_a': {'nonempty': 'dict'}},
    )
    to_internal_value_customized_cases = (
        {'model_a': 1},
        {'model_a': '1'},
        {'model_a': []},
        {'model_a': 0},
    )

    @pytest.mark.parametrize("case", to_internal_value_default_cases)
    def test_to_internal_value_default(self, mocker, serializer_fixture, case):
        """
        Тесты `to_internal_value` - случаи, когда поведение не отличается от
        родительского метода
        """
        ModelA, ModelB, nested, primary = serializer_fixture
        mocked_super_to_internal_value = mocker.patch.object(
            serializers.ModelSerializer, 'to_internal_value')
        mocked_build_relational_field = mocker.patch.object(
            serializers.ModelSerializer, 'build_relational_field'
        )

        primary.to_internal_value(case)

        assert not mocked_build_relational_field.called, (
            u"Не должно быть вызова `build_relational_field`")
        assert mocked_super_to_internal_value.called, (
            u"Не было вызова родительского `save`")
        assert isinstance(primary.fields['model_a'], nested.__class__), (
            u"Не должно происходить замены поля `model_a`")

    @pytest.mark.parametrize("case", to_internal_value_customized_cases)
    def test_to_internal_value_customized(self, mocker,
                                          serializer_fixture, case):
        """
        Тесты `to_internal_value` - случаи, когда поведение отличается от
        родительского метода
        """
        ModelA, ModelB, nested, primary = serializer_fixture
        mocked_super_to_internal_value = mocker.patch.object(
            serializers.ModelSerializer, 'to_internal_value')
        mocked_build_relational_field = mocker.patch.object(
            serializers.ModelSerializer, 'build_relational_field'
        )
        mocked_build_relational_field.return_value = MagicMock(), MagicMock()

        primary.to_internal_value(case)

        assert mocked_build_relational_field.called, (
            u"Не было вызова `build_relational_field`")
        assert mocked_super_to_internal_value.called, (
            u"Не было вызова родительского `save`")
        assert isinstance(primary.fields['model_a'], MagicMock), (
            u"Должна произойти замена поля `model_a`")


@pytest.fixture
def example_models():
    """
    Модели и сериализаторы для тестирования `SerializerManyToManyMixin`
    """
    ModelA = get_model_class({
        'name': models.CharField(max_length=100),
    })
    modelb_name = build_model_class_name()
    ModelAB = get_model_class({
        'model_a': models.ForeignKey(ModelA),
        'model_b': models.ForeignKey(modelb_name),
        'addition_field': models.CharField(max_length=100),
    })

    ModelB = get_model_class({
        'name': models.CharField(max_length=100),
        'models_a': models.ManyToManyField(ModelA, through=ModelAB)
    }, class_name=modelb_name)

    class CustomField(serializers.ModelSerializer):
        pass

    class MySerializer(SerializerManyToManyMixin,
                       serializers.ModelSerializer):

        m2m_update_fields = {'models_a': 'models_a'}
        models_a = CustomField(many=True, source='modelab_set')

        class Meta(object):
            model = ModelB
            fields = ('models_a', 'name')

    return ModelA, ModelAB, ModelB, MySerializer, CustomField


class TestSerializerManyToManyMixin(object):
    """
    Тесты класса `SerializerManyToManyMixin`
    """

    def test_not_implemented(self):
        """
        Тест на валидацию параметра класса
        """
        class MySerializer(SerializerManyToManyMixin):
            pass

        with pytest.raises(AssertionError) as excinfo:
            MySerializer()

        assert str(excinfo.value) == 'Field m2m_update_fields is not declared'

    def test_save_without_instance(self, mocker, example_models):
        """
        Тест метода `save`, создание инстанса
        """
        mocked_atomic = mocker.patch(
            'kelvin.common.serializer_mixins.transaction')
        ModelA, ModelAB, ModelB, MySerializer, CustomField = example_models
        mocker.patch.object(ReverseManyToOneDescriptor, '__set__')

        mocked_field = mocker.patch.object(serializers.ListSerializer, 'save')
        mocked_field.return_value = [ModelAB()]

        mocked_save = mocker.patch.object(serializers.ModelSerializer, 'save')
        mocked_save.return_value = ModelB(id=1)

        my_serializer = MySerializer()
        my_serializer._validated_data = {'modelab_set': [
            {'addition_field': u'Имя', 'model_a': 1},
            {'addition_field': u'Имя', 'model_a': 2}]}
        my_serializer.save()

        assert mocked_save.called
        assert mocked_field.called

    def test_save_with_instance(self, mocker, example_models):
        """
        Тест метода `save`, обновление инстанса
        """
        mocked_atomic = mocker.patch(
            'kelvin.common.serializer_mixins.transaction')
        ModelA, ModelAB, ModelB, MySerializer, CustomField = example_models
        mocker.patch.object(ReverseManyToOneDescriptor, '__set__')

        mocked_field = mocker.patch.object(serializers.ListSerializer, 'save')
        mocked_field.return_value = [ModelAB()]

        mocked_save = mocker.patch.object(serializers.ModelSerializer, 'save')
        mocked_save.return_value = ModelB(id=1)

        instance = MagicMock()
        instance.id = 1
        instance.modelab_set = MagicMock()
        instance.modelab_set.all.return_value = [ModelAB(id=1), ModelAB(id=2)]

        my_serializer = MySerializer(instance=instance)
        my_serializer._validated_data = {'modelab_set': [
            {'addition_field': u'Имя1', 'model_a': 1},
            {'addition_field': u'Имя2', 'model_a': 2}]}
        my_serializer.save()

        assert mocked_save.called
        assert mocked_field.called, u'Не был вызвал save поля сериализатора'
        assert instance.modelab_set.all.called, (
            u'Не был получен список существующих связей')

    def test_save_partly_update(self, mocker, example_models):
        """
        Тест метода `save`, нужного поля нет в валидных данных
        """
        mocked_atomic = mocker.patch(
            'kelvin.common.serializer_mixins.transaction')
        ModelA, ModelAB, ModelB, MySerializer, CustomField = example_models
        mocker.patch.object(ReverseManyToOneDescriptor, '__set__')

        mocked_field = mocker.patch.object(serializers.ListSerializer, 'save')
        mocked_field.return_value = [ModelAB()]

        mocked_save = mocker.patch.object(serializers.ModelSerializer, 'save')
        mocked_save.return_value = ModelB(id=1)

        instance = MagicMock()
        instance.id = 1
        instance.modelab_set = MagicMock()
        instance.modelab_set.all.return_value = [ModelAB(id=1), ModelAB(id=2)]

        my_serializer = MySerializer(instance=instance)
        my_serializer._validated_data = {'name': u'Имя'}
        my_serializer.save()

        assert mocked_save.called
        assert not mocked_field.called, (
            u'Метод save поля сериализатора не должен быть вызван')


class TestExcludeForStudentMixin(object):
    """
    Тест исключения полей для ученика
    """

    def test_get_field_names(self):
        class MySerializer(ExcludeForStudentMixin,
                           serializers.ModelSerializer):
            class Meta(object):
                exclude_for_student = (
                    'field_for_teacher',
                )
                fields = (
                    'common_field',
                    'field_for_teacher'
                )

        # убираем поля для ученика
        serializer = MySerializer(context={'for_student': True})
        assert serializer.get_field_names([], {}) == ['common_field']

        # возвращаем все поля для остальных
        serializer = MySerializer(context={})
        assert serializer.get_field_names([], {}) == (
            'common_field', 'field_for_teacher')
