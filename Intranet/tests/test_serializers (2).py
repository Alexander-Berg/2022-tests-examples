from builtins import object, range
from collections import OrderedDict

from mock import MagicMock, call

from django.db import models

from rest_framework import serializers

from kelvin.common.serializers import DictSerializer, ManyToManyListSerializer


class TestDictSerializer(object):
    """
    Тесты класса DictSerializer
    """

    def test_to_representation(self):
        """
        Проверяем работу метода to_representation
        """
        child_mock = MagicMock()

        def to_repr(obj):
            return {'id': obj.pk}

        serializer = DictSerializer(child=child_mock)
        child_mock.to_representation = MagicMock(side_effect=to_repr)

        OBJECTS_COUNT = 5

        objects = []
        for i in range(OBJECTS_COUNT):
            obj = MagicMock()
            obj.pk = i
            objects.append(obj)

        serialized = serializer.to_representation(objects)
        assert isinstance(serialized, OrderedDict)

        for obj in objects:
            assert obj.pk in serialized, (
                u"не сериализован объект с id {0}".format(obj.pk))
            assert serialized[obj.pk] == {'id': obj.pk}, (
                u"неверно сериализован объект с id {0}".format(obj.pk))

        assert len(objects) == len(serialized), (
            u"неверное число сериализованных объектов")

    def test_dealing_with_querysets(self, mocker):
        """
        Проверяем, что если набор объектов для сериализации --
        Manager или QuerySet, то перед сериализацией из него запрашиваются
        объекты вызовом all
        """
        serializer = DictSerializer(child=MagicMock())

        # у любых других объектов all не вызывается
        some_other_objects = MagicMock()
        some_other_objects.__iter__.return_value = []
        serializer.to_representation(some_other_objects)
        assert not some_other_objects.all.called

        mocked = mocker.patch('kelvin.common.serializers.models')
        mocked.Manager = MagicMock
        mocked.query.QuerySet = MagicMock

        # у менеджеров и queryset'ов — вызывается
        some_manager = MagicMock(spec=mocked.Manager())
        some_manager.mock_add_spec(['all'])
        some_manager.all.return_value = []

        serializer.to_representation(some_manager)
        some_manager.all.assert_called_once_with()

        some_qs = MagicMock(spec=mocked.query.QuerySet())
        some_qs.mock_add_spec(['all'])
        some_qs.all.return_value = []

        serializer.to_representation(some_qs)
        some_qs.all.assert_called_once_with()


class TestManyToManyListSerializer(object):
    """
    Тесты для класса `ManyToManyListSerializer`
    """

    def test_update(self, mocker):
        """
        Тесты метода `update`
        """
        class MyModelA(models.Model):
            name = models.CharField(u'Name', max_length=100)

        class MyModelB(models.Model):
            title = models.CharField(u'Name', max_length=100)
            models_a = models.ManyToManyField(MyModelA, through='MyModelAB')

        class MyModelAB(models.Model):
            model_a = models.ForeignKey(MyModelA)
            model_b = models.ForeignKey(MyModelB)
            additional_field = models.CharField(u'Additional', max_length=100)

            class Meta(object):
                unique_together = ('model_a', 'model_b')

        class MySerializer(serializers.ModelSerializer):
            class Meta(object):
                model = MyModelAB
                fields = ('model_a', 'additional_field')
                list_serializer_class = ManyToManyListSerializer

        model_a_instances = [MyModelA(id=i) for i in range(1, 8)]

        # с 1 по 5 включительно, связи есть
        instances = [MyModelAB(id=i, model_a=model_a_instances[i - 1])
                     for i in range(1, 6)]

        # c 1 по 2 включительно, должен удалить, с 3 по 5 - изменить,
        # с 6 по 7 создать
        # по ключу `model_a` будут лежать инстансы, а не id, т.к. это поле
        # ссылается на поле типа `ForeignKey` модели и внутри ModelSerializer
        # после валидации ему будет присвоен инстанс модели
        validated_data = [
            {
                'additional_field': u'{0}'.format(i),
                'model_a': model_a_instances[i - 1],
            }
            for i in range(3, 8)
        ]
        validated_data[0]['id'] = 3
        validated_data[1]['id'] = 4
        validated_data[2]['id'] = 5

        mocked_create = mocker.patch.object(MySerializer, 'create')
        mocked_update = mocker.patch.object(MySerializer, 'update')
        mocked_delete = mocker.patch.object(MyModelAB, 'delete')
        mocker.patch('django.db.transaction.get_connection')

        list_serializer = MySerializer(many=True)
        list_serializer.update(instances, validated_data)

        assert mocked_create.called, u'Create method wasn\'t called'
        assert mocked_create.call_count == 2
        calls = [call(data) for data in validated_data[3:]]
        mocked_create.assert_has_calls(calls, any_order=True)

        assert mocked_update.called, u'Update method wasn\'t called'
        assert mocked_update.call_count == 3
        calls = [call(instances[i - 1], validated_data[i - 3])
                 for i in range(3, 6)]
        mocked_update.assert_has_calls(calls, any_order=True)

        assert mocked_delete.called, u'Delete method wasn\'t called'
        assert mocked_delete.call_count == 2

        mocked_create.reset_mock()
        mocked_update.reset_mock()
        mocked_delete.reset_mock()
        # удалить все связи
        list_serializer = MySerializer(many=True)

        list_serializer.update(instances, {})
        assert mocked_delete.called, u'Delete method wasn\'t called'
        assert mocked_delete.call_count == 5

        assert not mocked_create.called
        assert not mocked_update.called
