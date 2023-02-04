from builtins import range, object
import pytest

from django.core.urlresolvers import reverse

from kelvin.group_levels.models import GroupLevel
from kelvin.problem_meta.models import (
    Skill, Exam, ProblemMetaExam, ProblemMeta,
)
from kelvin.subjects.models import Theme, Subject


POST_PROBLEM_META_DATA = (
    (
        {
            'difficulty': 1, 'tags': ['new', 'math'], 'main_theme': 1,
            'skills': [1, 2],
        },
        {
            'difficulty': 1, 'tags': ['new', 'math'], 'main_theme': 1,
            'skills': [1, 2], 'additional_themes': [],
            'exams': [], 'group_levels': [],
        },
    ),
    (
        {'difficulty': 1, 'tags': ['new', 'math'], 'main_theme': 1,
         'group_levels': [1, 2], 'skills': [1, 3]},
        {
            'difficulty': 1, 'tags': ['new', 'math'], 'main_theme': 1,
            'group_levels': [
                {'id': 1, 'name': u'1 класс', 'baselevel': 2, 'slug': 'slug1'},
                {'id': 2, 'name': u'2 класс', 'baselevel': 1, 'slug': 'slug2'},
            ],
            'skills': [1, 3], 'additional_themes': [],
            'exams': [],
        },
    ),
    (
        {'difficulty': 1, 'tags': ['new', 'math'], 'main_theme': 1,
         'group_levels': [1, 3], 'skills': [1, 2],
         'additional_themes': [1, 3]},
        {
            'difficulty': 1, 'tags': ['new', 'math'], 'main_theme': 1,
            'skills': [1, 2], 'additional_themes': [1, 3],
            'exams': [],
            'group_levels': [
                {'id': 1, 'name': u'1 класс', 'baselevel': 2, 'slug': 'slug1'},
                {'id': 3, 'name': u'3 класс', 'baselevel': 2, 'slug': 'slug3'},
            ],
        },
    ),
    (
        {
            'difficulty': 1, 'tags': ['new', 'math'], 'main_theme': 1,
            'skills': [1, 2], 'additional_themes': [1, 3],
            'classes': [
                {'id': 1, 'name': u'1 класс'},
                {'id': 2},
                {'id': 3, 'name': u'3 класс', 'baselevel': 2},
            ],
        },
        {
            'difficulty': 1, 'tags': ['new', 'math'], 'main_theme': 1,
            'skills': [1, 2], 'additional_themes': [1, 3],
            'exams': [],
            'group_levels': [
                {'id': 1, 'name': u'1 класс', 'baselevel': 2, 'slug': 'slug1'},
                {'id': 2, 'name': u'2 класс', 'baselevel': 1, 'slug': 'slug2'},
                {'id': 3, 'name': u'3 класс', 'baselevel': 2, 'slug': 'slug3'},
            ],
        },
    ),
    (
        {
            'difficulty': 1, 'tags': ['new', 'math'], 'main_theme': 1,
        },
        {
            'difficulty': 1, 'tags': ['new', 'math'], 'main_theme': 1,
            'skills': [], 'additional_themes': [],
            'exams': [], 'group_levels': [],
        },
    ),
)

@pytest.fixture
def models_data():
    """
    Создает предмет, темы для него, навыки и книги для тестирования
    метаинформации задачи
    :return: словарь со списками объектов или объектом
    """
    subject = Subject.objects.create(name=u'Математика')
    themes = []
    skills = []
    group_levels = []
    exams = []
    for i in range(1, 4):
        themes.append(
            Theme.objects.create(name=u'Арифметика', subject=subject))
        skills.append(
            Skill.objects.create(name=u'Сложение', subject=subject))
        group_levels.append(
            GroupLevel.objects.create(name=u'{0} класс'.format(i),
                                      baselevel=i % 2 + 1, slug='slug123'))
        exams.append(Exam.objects.create(name=u'ЕГЭ',
                                         year=u'{0}'.format(i)))
    return {
        'subject': subject,
        'group_levels': group_levels,
        'themes': themes,
        'skills': skills,
        'exams': exams,
    }


@pytest.mark.django_db
class FIXTestProblemMetaViewSet(object):
    # TODO переписать все тесты EDU-569
    """
    Тесты рест-интерфейса метаинформации
    """
    @pytest.mark.parametrize('version', ['v1', 'v2'])
    @pytest.mark.parametrize('post_data,expected', POST_PROBLEM_META_DATA)
    def test_create_problem_meta(self, jclient, post_data, expected,
                                 models_data, version):
        """
        Тест создания метаинформации

        :param post_data: словарь с метаинформацией, которую надо создать
        :param models_data: содержит инстансы необходимых моделей
        """
        exams_data = [{'exam': exam.id,
                       'number': u'B.{0}'.format(i)}
                      for i, exam in enumerate(models_data['exams'])]
        post_data.update({'exams': exams_data})
        expected.update({'exams': exams_data})

        create_url = reverse('{0}:problem_meta-list'.format(version))
        response = jclient.post(create_url, post_data)
        assert response.status_code == 201, (
            u'Неверный статус ответа, содержимое ответа: {0}'.format(
                response.json()))
        answer = response.json()
        assert 'id' in answer, u'Нет идентификатора метаинформации в ответе'
        assert 'date_updated' in answer, u'Нет даты обновления в ответе'
        answer.pop('id')
        answer.pop('date_updated')
        for exam in answer['exams']:
            assert exam.pop('id')

        # в ответ получаем то, что посылали
        assert answer == expected
        assert ProblemMeta.objects.all().count() == 1

    @pytest.mark.parametrize('version', ['v1', 'v2'])
    def test_update_problem_meta(self, jclient, models_data, version):
        """
        Проверка обновления мета-информации
        """
        data = {'difficulty': 1, 'tags': [u'Tag'],
                'main_theme': models_data['themes'][0],
                }
        meta = ProblemMeta.objects.create(**data)
        meta.group_levels = models_data['group_levels']
        meta.additional_themes = models_data['themes']
        meta.skills = models_data['skills']
        meta.save()

        problem_meta_exams = []
        for i, exam in enumerate(models_data['exams'][2:], 1):
            problem_meta_exams.append(ProblemMetaExam.objects.create(
                problem_meta=meta, exam=exam, number=u'A.{0}'.format(i)))

        new_data = {
            'difficulty': 2, 'tags': [u'New tag', u'ЕГЭ'],
            'main_theme': models_data['themes'][1].id,
            'group_levels': [models_data['group_levels'][0].id],
            'additional_themes': [theme.id
                                  for theme in models_data['themes'][:2]],
            'skills': [models_data['skills'][0].id],
            # 2 удалятся, 2 добавятся, 1 изменятся
            'exams': [
                {
                    'number': u'B.0',
                    'exam': models_data['exams'][0].id,
                },
                {
                    'number': u'B.1',
                    'exam': models_data['exams'][0].id,
                },
                {
                    'id': problem_meta_exams[0].id,
                    'number': u'B.2',
                    'exam': models_data['exams'][0].id,
                },
            ],
        }

        object_url = reverse('{0}:problem_meta-detail'.format(version),
                             args=(meta.id,))
        response = jclient.put(object_url, new_data)

        assert response.status_code == 200, response.json()
        json_data = response.json()
        json_data.pop('id')
        json_data.pop('date_updated')
        assert len(json_data['exams']) == 3
        assert json_data['exams'][0].pop('id')
        assert json_data['exams'][1].pop('id')
        problem_meta = ProblemMeta.objects.get(id=meta.id)
        new_data.update({'group_levels': [
            {'baselevel': 2, 'id': 1, 'slug': 'slug1',
             'name': u'1 класс'}]})
        assert json_data == new_data
        assert problem_meta.exams.count() == len(new_data['exams'])
        assert problem_meta.skills.count() == 1
        assert problem_meta.skills.all()[0].id == models_data['skills'][0].id

    @pytest.mark.parametrize('version', ['v1', 'v2'])
    def test_get_problem_meta(self, jclient, models_data, version):
        """
        Проверка получения данных о мета-информации
        """
        meta = ProblemMeta.objects.create(
            difficulty=1, tags=[u'Ghfhfh'],
            main_theme=models_data['themes'][0],
        )
        meta.group_levels = models_data['group_levels']
        meta.additional_themes = models_data['themes']
        meta.skills = models_data['skills']
        meta.save()
        problem_meta_exams = []
        for i, exam in enumerate(models_data['exams'], 1):
            problem_meta_exams.append(ProblemMetaExam.objects.create(
                exam=exam, problem_meta=meta, number=u'{0}'.format(i)))
        object_url = reverse('{0}:problem_meta-detail'.format(version),
                             args=(meta.id,))
        response = jclient.get(object_url)

        assert response.status_code == 200
        json_data = response.json()
        assert 'date_updated' in json_data
        json_data.pop('id')
        json_data.pop('date_updated')

        assert json_data == {
            'difficulty': 1,
            'tags': [u'Ghfhfh'],
            'main_theme': models_data['themes'][0].id,
            'group_levels': [
                {'id': class_.id, 'name': class_.name,
                 'slug': 'slug', 'baselevel': class_.baselevel}
                for class_ in models_data['group_levels']
            ],
            'additional_themes': [item.id for item in models_data['themes']],
            'skills': [item.id for item in models_data['skills']],
            'exams': [
                {
                    'id': item.id,
                    'exam': item.exam.id,
                    'number': item.number,
                }
                for item in problem_meta_exams
            ],
        }
