from builtins import object
import pytest

from django.core.urlresolvers import reverse

from kelvin.subjects.models import Subject


@pytest.mark.xfail
@pytest.mark.django_db
class TestSubjectListViewSet(object):
    """Тесты `SubjectListViewSet`"""
    def test_non_public_subjects(self, jclient):
        """
        Проверяет, что в списке отдаются только доступные предметы
        """
        Subject.objects.all().delete()  # FIXME где-то создается предмет с id=1
        subject1 = Subject.objects.create(
            name=u'Математика',
            slug='math',
        )
        subject2 = Subject.objects.create(
            name=u'Программирование',
            slug='proga',
            public=False,
        )
        subject3 = Subject.objects.create(
            name=u'Русский язык',
            slug='russian',
        )

        list_url = reverse('v2:subject-list')
        expected_response = [
            {
                'id': subject1.id,
                'name': u'Математика',
                'slug': 'math',
            },
            {
                'id': subject3.id,
                'name': u'Русский язык',
                'slug': 'russian',
            },
        ]
        response = jclient.get(list_url)

        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.json() == expected_response, (
            u'Неверный формат списка предметов')
