from builtins import object
import re

import pytest
from swissknife.assertions import AssertDictsAreEqual
from django.core.urlresolvers import reverse
from kelvin.problems.models import Resource
from django.conf import settings

POST_RESOURCE_DATA = (
    {},
)


@pytest.mark.django_db
class TestResourceViewSet(object):
    """
    Тесты рест-интерфейса ресурсов
    """
    @pytest.mark.parametrize('version', ['v2'])
    @pytest.mark.parametrize('post_data', POST_RESOURCE_DATA)
    def test_create_resource(self, jclient, post_data, content_manager, version):
        """
        Тест создания ресурса

        :param post_data: словарь с ресурсом, который надо создать
        """
        current_resources_count = Resource.objects.count()

        create_url = reverse('{0}:resource-list'.format(version))

        # неавторизованный юзер не может создавать ресурсы
        response = jclient.post(create_url, post_data)
        assert response.status_code == 401

        # контент-менеджер — может
        jclient.login(user=content_manager)
        response = jclient.post(create_url, post_data)
        assert response.status_code == 201

        answer = response.json()
        assert 'id' in answer, u'Нет идентификатора ресурса в ответе'
        assert 'date_updated' in answer, u'Нет даты обновления в ответе'
        assert 'nda' in answer, u'Нет признака NDA в ответе'
        assert 'shortened_file_url' in answer, u'Нет короткой ссылки на ресурс'

        id_ = answer.pop('id')
        answer.pop('date_updated')

        GUID_RE = settings.SWITCHMAN_URL + r'\/\w{8}-\w{4}-\w{4}-\w{4}-\w{12}'
        result_oracle = {
            'type': lambda x: x == '',
            'name': lambda x: x == '',
            'nda': lambda x: x is False,
            'shortened_file_url': lambda x: bool(re.match(GUID_RE, x))
        }
        AssertDictsAreEqual(answer, result_oracle)

        assert Resource.objects.count() - current_resources_count == 1

    @pytest.mark.parametrize('version', ['v2'])
    def test_resource_detail_permissions(self, jclient, version, student,
                                         parent, teacher, content_manager):
        """
        Тест доступов на detail-ручке ресурсов
        """
        resource = Resource.objects.create()
        detail_url = reverse('{0}:resource-detail'.format(version),
                             args=(resource.id,))

        # для доступа к detail-ручке ресурсов нужно быть авторизованным
        response = jclient.get(detail_url)
        assert response.status_code == 401

        # Everybody authorized can use get
        for user in (student, parent, teacher):
            if user:
                jclient.login(user=user)
            response = jclient.get(detail_url)
            assert response.status_code == 200

        # контент-менеджеру доступна detail-ручка, даже если он не учитель
        jclient.login(user=content_manager)
        response = jclient.get(detail_url)
        assert not content_manager.is_teacher and response.status_code == 200
