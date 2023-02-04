from builtins import map, object
import pytest

from itertools import chain

from django.contrib.auth import get_user_model
from django.core.urlresolvers import reverse
from mixer.backend.django import Mixer

from kelvin.problems.models import TextResourceContentType
from kelvin.problems.serializers import ContentTypeSerializer


User = get_user_model()
mixer = Mixer(locale='ru')


@pytest.fixture
def content_types():
    return mixer.cycle(9).blend(
        TextResourceContentType,
        resource__file=mixer.sequence(u'test{0}.jpg'),
        name=mixer.sequence(u'Тип контента №{0}')
    )


@pytest.fixture
def user():
    """Пользователь, у которого есть доступы к типам контента"""
    return mixer.blend(
        User,
        is_teacher=True,
        is_content_manager=True,
    )


@pytest.mark.django_db
class TestContentTypeViewSet(object):
    """
    Тесты на получение расширенных типов контента
    """

    def test_list(self, content_type, content_types, jclient, user):
        jclient.login(user=user)

        content_type_list_url = reverse('v2:content_types-list')
        response = jclient.get(content_type_list_url)
        response_dict = response.json()

        # мы сгенерили N объектов и один был создан с помощью фикстуры
        # content_type
        serialized_list = ContentTypeSerializer(
            chain([content_type.instance], content_types), many=True
        ).data

        assert response_dict['count'] == len(content_types) + 1
        assert response_dict['results'] == list(map(dict, serialized_list))

    def test_detail(self, content_type, jclient, user):
        jclient.login(user=user)

        content_type_list_detail = reverse(
            'v2:content_types-detail', args=(content_type.instance.id, )
        )
        response = jclient.get(content_type_list_detail)
        response_dict = response.json()

        serialized_content_type = dict(
            ContentTypeSerializer(content_type.instance).data
        )

        assert response_dict == serialized_content_type

    @pytest.mark.parametrize(
        'user_with_roles, status_code',
        [
            (('is_staff', ), 200),
            (('is_content_manager', 'is_teacher'), 200),
            (('is_parent', ), 403),
            (('is_content_manager', ), 200),
            (('is_teacher', ), 200),
            ((), 403),
            (('is_parent', 'is_teacher'), 200),
        ],
        ids=str,
        indirect=('user_with_roles',),
    )
    def test_resource_access(self, user_with_roles, content_type, status_code,
                             jclient):

        jclient.login(user=user_with_roles)

        content_type_list_detail = reverse(
            'v2:content_types-detail', args=(content_type.instance.id, )
        )
        response = jclient.get(content_type_list_detail)
        assert response.status_code == status_code
