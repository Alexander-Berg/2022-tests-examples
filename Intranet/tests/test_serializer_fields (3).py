from builtins import object

from mock import call

from kelvin.problems.serializer_fields import ExpandableProblemField, ExpandableTextResourceField, NestedForeignKeyField


class TestExpandableProblemField(object):
    """
    Тесты сериализатора задач в занятии
    """

    def test_to_representation(self, mocker):
        """
        Проверка сериализации
        """
        mocked_to_representation = mocker.patch.object(
            NestedForeignKeyField, 'to_representation')
        mocked_short_serializer = mocker.patch(
            'kelvin.problems.serializer_fields.ProblemIdDateSerializer')

        # сериализация без разворачивания задач
        field = ExpandableProblemField()
        field.to_representation(123)
        assert mocked_to_representation.mock_calls == [], (
            u'Метод не должен вызываться')
        assert mocked_short_serializer.mock_calls == [
            call(), call().to_representation(123)], (
            u'Должен быть вызван сериализатор')

        # сериализация с разворачиванием задач
        mocked_short_serializer.reset_mock()
        mocked_to_representation.reset_mock()
        field = ExpandableProblemField()
        field._context = {'expand_problems': True}
        field.to_representation(123)

        assert mocked_to_representation.mock_calls == [call(123)], (
            u'Должен быть вызван метод')
        assert mocked_short_serializer.mock_calls == [], (
            u'Сериализатор не должен вызываться')


class TestExpandableTextResourceField(object):
    """
    Тесты сериализатора текстовых ресурсов в занятии
    """

    def test_to_representation(self, mocker):
        """
        Проверка сериализации
        """
        mocked_to_representation = mocker.patch.object(
            NestedForeignKeyField, 'to_representation')
        mocked_short_serializer = mocker.patch(
            'kelvin.problems.serializer_fields.TextResourceIdDateSerializer')

        # сериализация без разворачивания ресурсов
        field = ExpandableTextResourceField()
        field.to_representation(123)
        assert mocked_to_representation.mock_calls == [], (
            u'Метод не должен вызываться')
        assert mocked_short_serializer.mock_calls == [
            call(), call().to_representation(123)], (
            u'Должен быть вызван сериализатор')

        # сериализация с разворачиванием ресурсов
        mocked_short_serializer.reset_mock()
        mocked_to_representation.reset_mock()
        field = ExpandableTextResourceField()
        field._context = {'expand_problems': True}
        field.to_representation(123)

        assert mocked_to_representation.mock_calls == [call(123)], (
            u'Должен быть вызван метод')
        assert mocked_short_serializer.mock_calls == [], (
            u'Сериализатор не должен вызываться')
