from builtins import object

from mock import MagicMock, call

from kelvin.accounts.admin import TeacherProfileAdmin, UnapprovedTeacherProfileAdmin


class TestUnapprovedTeacherProfileAdmin(object):
    """
    Тесты админки неподтвержденных профилей учителей
    """
    def test_get_queryset(self, mocker):
        """
        Проверяем, что показываем только неподтвержденные профили
        """
        mocked = mocker.patch.object(TeacherProfileAdmin, 'get_queryset')
        admin = UnapprovedTeacherProfileAdmin(MagicMock(), MagicMock())
        request = MagicMock()
        admin.get_queryset(request)

        assert mocked.mock_calls == [
            call(request), call().filter(teacher__is_teacher=False)], (
            'Должен быть вызван родительский метод `get_queryset` и должна '
            'быть фильтрация возвращенного объекта по полю учителя '
            '`is_teacher`'
        )
