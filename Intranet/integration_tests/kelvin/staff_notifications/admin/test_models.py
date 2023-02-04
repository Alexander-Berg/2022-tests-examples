from builtins import object
import pytest

from kelvin.staff_notifications.models import ActivationCode


@pytest.mark.django_db
class TestActivationCode(object):
    """
    Тесты модели кода активации
    """
    def test_batch_create(self, action1, action2):
        """
        Проверка генерации набора кодов
        """
        created_ids = ActivationCode.batch_create(3, [action1, action2])

        codes = (
            ActivationCode.objects.all()
            .order_by('id')
            .prefetch_related('actions')
        )

        assert [code.pk for code in codes] == created_ids
        for code in codes:
            assert list(code.actions.all()) == [action1, action2]
