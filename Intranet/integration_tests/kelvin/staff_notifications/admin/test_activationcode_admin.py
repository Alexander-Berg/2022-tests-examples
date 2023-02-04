from builtins import str, object
import pytest

from django.core.urlresolvers import reverse

from kelvin.staff_notifications.models import ActivationCode


@pytest.mark.skip()
@pytest.mark.django_db
class TestActivationCodeAdmin(object):
    """
    Тесты админки кодов активации
    """
    def test_add_view(self, jclient, action1, action2):
        """
        Проверка генерации набора кодов
        """
        create_url = reverse('admin:staff_notifications_activationcode_add')
        jclient.login(is_superuser=True)

        # создаем 2 кода без привязки к действиям
        response = jclient.post(create_url, data={
            'count': 2,
            'actions': []
        })

        codes = (
            ActivationCode.objects.all()
            .order_by('id')
            .prefetch_related('actions')
        )

        assert len(codes) == 2
        for code in codes:
            assert len(code.actions.all()) == 0

        assert response.status_code == 302, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.url == (
            reverse('admin:staff_notifications_activationcode_changelist') +
            '?id__in={}'.format(','.join([str(code.pk) for code in codes]))
        )

        ActivationCode.objects.all().delete()

        # создаем 2 кода, привязанные к действиям
        response = jclient.post(create_url, data={
            'count': 2,
            'actions': [action1.pk, action2.pk]
        })

        codes = (
            ActivationCode.objects.all()
            .order_by('id')
            .prefetch_related('actions')
        )

        assert len(codes) == 2
        for code in codes:
            assert list(code.actions.all()) == [action1, action2]

        assert response.status_code == 302, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.url == (
            reverse('admin:staff_notifications_activationcode_changelist') +
            '?id__in={}'.format(','.join([str(code.pk) for code in codes]))
        )
