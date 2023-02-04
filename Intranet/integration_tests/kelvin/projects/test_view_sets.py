from builtins import zip, object
import pytest

from django.core.urlresolvers import reverse

from kelvin.projects.models import Project
from kelvin.accounts.models import User, UserProject


@pytest.mark.django_db
class TestProjectSubjectViewSet(object):
    """
    Тестирование ручки /v2/project_subjects/<id>
    """

    def test_404(self, jclient):
        """
        Тест на корректную обработку 404
        """
        url = reverse('v2:project_subjects-detail', args=(-1, ))
        response = jclient.get(url)
        assert response.status_code == 404, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_unauthorized(self, jclient, full_project):
        """
        Тест на разрешение доступа неавторизованному пользователю
        """
        mathematics = full_project['subjects'][0]['subject']
        url = reverse('v2:project_subjects-detail', args=(mathematics.id, ))
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_simple(self, jclient, full_project):
        """
        Простое тестирование ручки на возврат основных полей
        """

        mathematics = full_project['subjects'][0]['subject']
        (content1, content2) = full_project['subjects'][0]['content']

        url = reverse('v2:project_subjects-detail', args=(mathematics.id, ))
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.data == {
            'id': mathematics.id,
            'slug': mathematics.slug,
            'title': mathematics.title,
            'full_title': mathematics.full_title,
            'meta': {},
            'background': mathematics.background,
            'items': [
                {
                    'id': content1.id,
                    'display_type': content1.display_type,
                    'display_options': {},
                    'order': content1.order,
                    'content': {
                        'id': content1.course_id,
                        'name': content1.course.name,
                    }
                },
                {
                    'id': content2.id,
                    'display_type': content2.display_type,
                    'display_options': {},
                    'order': content2.order,
                    'content': {
                        'id': content2.course_id,
                        'name': content2.course.name,
                        'clessons': [],
                    }
                }
            ],
        }, u'Неправильный ответ'

    def test_clessons(self, jclient, full_project):
        """
        Тест на выдачу clessons
        """

        english = full_project['subjects'][1]['subject']
        (content3, ) = full_project['subjects'][1]['content']
        course = full_project['subjects'][1]['courses'][0]

        url = reverse('v2:project_subjects-detail', args=(english.id, ))
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.data == {
            'id': english.id,
            'slug': english.slug,
            'title': english.title,
            'full_title': english.full_title,
            'meta': {},
            'background': english.background,
            'items': [
                {
                    'id': content3.id,
                    'display_type': content3.display_type,
                    'display_options': {},
                    'order': content3.order,
                    'content': {
                        'id': content3.course_id,
                        'name': content3.course.name,
                        'clessons': [
                            {
                                'id': clesson.id,
                                'order': clesson.order,
                                'name': clesson.lesson.name,
                                'date_assignment_passed': date_passed,
                            }
                            for clesson, date_passed
                            in zip(course.courselessonlink_set.all(),
                                   [False, True, False])
                        ]
                    }
                },
            ],
        }, u'Неправильный ответ'


@pytest.mark.django_db
class TestProjectViewSet(object):
    """
    Тестирование ручки /v2/projects/<project_slug>/[?expand_subject=<slug>]
    """
    def test_auto_add_user_to_project(self, jclient):
        project = Project(
            slug="generic_prject",
            title="Generic project"
        )
        project.save()
        user = User(
            username="generic student"
        )
        user.save()
        url = reverse('auto_add_to_project')
        jclient.login(user=user)
        response = jclient.post(url, [{'nda_accepted': True, 'add_code': project.add_code}])
        assert response.status_code == 201
        user_projects = UserProject.objects.filter(user=user, project=project)
        assert len(user_projects) == 1

    def test_404(self, jclient):
        """
        Тест на корректную обработку 404
        """
        url = reverse('v2:projects-detail', args=(-1, ))
        response = jclient.get(url)
        assert response.status_code == 404, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_unauthorized(self, jclient, full_project):
        """
        Тест на разрешение доступа неавторизованному пользователю
        """
        ege = full_project['project']
        url = reverse('v2:projects-detail', args=(ege.slug, ))
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))

    def test_extended(self, jclient, full_project):
        """
        Простое тестирование ручки на возврат основных полей с раскрытием
        списка (без дефолтного ПроектоПредмета)
        """

        ege = full_project['project']
        mathematics = full_project['subjects'][0]['subject']
        english = full_project['subjects'][1]['subject']
        (content3, ) = full_project['subjects'][1]['content']
        course = full_project['subjects'][1]['courses'][0]

        url = ('/api/v2/projects/{slug}/?expand_subject={expand_subject}'
               .format(slug=ege.slug, expand_subject=english.slug))
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.data == {
            'id': ege.id,
            'slug': ege.slug,
            'add_code': Project.gen_digest('Project {}'.format(ege.slug)),
            'nda': False,
            'nda_resource': None,
            'title': ege.title,
            'default_project_subject_id': ege.default_project_subject_id,
            'project_subjects': [
                {
                    'id': mathematics.id,
                    'slug': mathematics.slug,
                    'title': mathematics.title,
                    'full_title': mathematics.full_title,
                    'meta': {},
                    'background': mathematics.background,
                },
                {
                    'id': english.id,
                    'slug': english.slug,
                    'title': english.title,
                    'full_title': english.full_title,
                    'meta': {},
                    'background': english.background,
                    'items': [
                        {
                            'id': content3.id,
                            'order': content3.order,
                            'display_type': content3.display_type,
                            'display_options': {},
                            'content': {
                                'id': content3.course_id,
                                'name': content3.course.name,
                                'clessons': [
                                    {
                                        'id': clesson.id,
                                        'order': clesson.order,
                                        'name': clesson.lesson.name,
                                        'date_assignment_passed': date_passed,
                                    }
                                    for clesson, date_passed
                                    in zip(course.courselessonlink_set.all(),
                                           [False, True, False])
                                ]
                            }
                        },
                    ],
                }
            ],
        }, u'Неправильный ответ'

    def test_default_extended_without_link(self, jclient, full_project):
        """
        Тест на дефолтное раскрытие ПроектоПредмета, если такого нет
        """

        ege = full_project['project']
        mathematics = full_project['subjects'][0]['subject']
        english = full_project['subjects'][1]['subject']

        url = reverse('v2:projects-detail', args=(ege.slug,))
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.data == {
            'id': ege.id,
            'slug': ege.slug,
            'add_code': Project.gen_digest('Project {}'.format(ege.slug)),
            'nda': False,
            'nda_resource': None,
            'title': ege.title,
            'default_project_subject_id': ege.default_project_subject_id,
            'project_subjects': [
                {
                    'id': mathematics.id,
                    'slug': mathematics.slug,
                    'title': mathematics.title,
                    'full_title': mathematics.full_title,
                    'meta': {},
                    'background': mathematics.background,
                },
                {
                    'id': english.id,
                    'slug': english.slug,
                    'title': english.title,
                    'full_title': english.full_title,
                    'meta': {},
                    'background': english.background
                }
            ],
        }, u'Неправильный ответ'

    def test_default_extended_with_link(self, jclient, full_project):
        """
        Тест на дефолтное раскрытие ПроектоПредмета, если он есть
        """

        ege = full_project['project']
        mathematics = full_project['subjects'][0]['subject']
        english = full_project['subjects'][1]['subject']
        (content3,) = full_project['subjects'][1]['content']
        course = full_project['subjects'][1]['courses'][0]

        ege.default_project_subject = english
        ege.save()

        url = reverse('v2:projects-detail', args=(ege.slug,))
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.data == {
            'id': ege.id,
            'slug': ege.slug,
            'add_code': Project.gen_digest('Project {}'.format(ege.slug)),
            'nda': False,
            'nda_resource': None,
            'title': ege.title,
            'default_project_subject_id': ege.default_project_subject_id,
            'project_subjects': [
                {
                    'id': mathematics.id,
                    'slug': mathematics.slug,
                    'title': mathematics.title,
                    'full_title': mathematics.full_title,
                    'meta': {},
                    'background': mathematics.background,
                },
                {
                    'id': english.id,
                    'slug': english.slug,
                    'title': english.title,
                    'full_title': english.full_title,
                    'meta': {},
                    'background': english.background,
                    'items': [
                        {
                            'id': content3.id,
                            'order': content3.order,
                            'display_type': content3.display_type,
                            'display_options': {},
                            'content': {
                                'id': content3.course_id,
                                'name': content3.course.name,
                                'clessons': [
                                    {
                                        'id': clesson.id,
                                        'order': clesson.order,
                                        'name': clesson.lesson.name,
                                        'date_assignment_passed': date_passed,
                                    }
                                    for clesson, date_passed
                                    in zip(course.courselessonlink_set.all(),
                                           [False, True, False])
                                ]
                            }
                        },
                    ]
                }
            ],
        }, u'Неправильный ответ'

    def test_extended_with_default(self, jclient, full_project):
        """
        Тестирование ручки на возврат основных полей с раскрытием
        списка (у проекта проставлен ПроектоПредмет по умолчанию,
        но мы запрашиваем другой)
        """

        ege = full_project['project']
        mathematics = full_project['subjects'][0]['subject']
        english = full_project['subjects'][1]['subject']
        (content3, ) = full_project['subjects'][1]['content']
        course = full_project['subjects'][1]['courses'][0]

        ege.default_project_subject = mathematics
        ege.save()

        url = ('/api/v2/projects/{slug}/?expand_subject={expand_subject}'
               .format(slug=ege.slug, expand_subject=english.slug))
        response = jclient.get(url)
        assert response.status_code == 200, (
            u'Неправильный статус ответа, ответ: {0}'.format(response.content))
        assert response.data == {
            'id': ege.id,
            'slug': ege.slug,
            'add_code': Project.gen_digest('Project {}'.format(ege.slug)),
            'nda': False,
            'nda_resource': None,
            'title': ege.title,
            'default_project_subject_id': ege.default_project_subject_id,
            'project_subjects': [
                {
                    'id': mathematics.id,
                    'slug': mathematics.slug,
                    'title': mathematics.title,
                    'full_title': mathematics.full_title,
                    'meta': {},
                    'background': mathematics.background,
                },
                {
                    'id': english.id,
                    'slug': english.slug,
                    'title': english.title,
                    'full_title': english.full_title,
                    'meta': {},
                    'background': english.background,
                    'items': [
                        {
                            'id': content3.id,
                            'order': content3.order,
                            'display_type': content3.display_type,
                            'display_options': {},
                            'content': {
                                'id': content3.course_id,
                                'name': content3.course.name,
                                'clessons': [
                                    {
                                        'id': clesson.id,
                                        'order': clesson.order,
                                        'name': clesson.lesson.name,
                                        'date_assignment_passed': date_passed,
                                    }
                                    for clesson, date_passed
                                    in zip(course.courselessonlink_set.all(),
                                           [False, True, False])
                                ]
                            }
                        },
                    ],
                }
            ],
        }, u'Неправильный ответ'
