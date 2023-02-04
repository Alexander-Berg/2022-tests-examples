import pytest

from kelvin.projects.models import (
    Project, ProjectSubject, ProjectSubjectItem,
)
from kelvin.accounts.models import UserProject


@pytest.fixture
def full_project(course, course2, course_with_lesson_variations):
    """
    Создает и возвращает структуру
    Проект (ege)
        ПроектоПредмет (mathematics)
            ПроектоПредметоОбъект (CARD_NORMAL_HALF) - course
            ПроектоПредметоОбъект (CARD_EXTENDED_FOURTH) - course2
        ПроектоПредмет (english)
            ПроектоПредметоОбъект (CARD_NORMAL_FOURTH)
                course_with_lesson_variations
    """

    project = Project.objects.create(
        slug='ege',
        title='ЕГЭ',
        default_project_subject=None,
    )

    mathematics = ProjectSubject.objects.create(
        project=project,
        slug='mathematics',
        title='Математика',
        order=0,
        background='red',
    )
    english = ProjectSubject.objects.create(
        project=project,
        slug='english',
        title='Английский',
        order=1,
        background='green',
    )

    content1 = ProjectSubjectItem.objects.create(
        project_subject=mathematics,
        display_type=ProjectSubjectItem.DisplayType.CARD_NORMAL_HALF,
        item_type=ProjectSubjectItem.ItemType.COURSE,
        course=course,
        order=0,
    )
    content2 = ProjectSubjectItem.objects.create(
        project_subject=mathematics,
        display_type=ProjectSubjectItem.DisplayType.CARD_EXTENDED_FOURTH,
        item_type=ProjectSubjectItem.ItemType.COURSE,
        course=course2,
        order=1,
    )
    course3 = course_with_lesson_variations['course']
    content3 = ProjectSubjectItem.objects.create(
        project_subject=english,
        display_type=ProjectSubjectItem.DisplayType.CARD_EXTENDED_HALF,
        item_type=ProjectSubjectItem.ItemType.COURSE,
        course=course3,
        order=0,
    )

    return {
        'project': project,
        'subjects': [
            {
                'subject': mathematics,
                'content': [content1, content2],
                'courses': [course, course2],
            },
            {
                'subject': english,
                'content': [content3],
                'courses': [course3],
            },
        ]
    }


@pytest.fixture
def default_project():
    project = Project.objects.create(
        title='DEFAULT',
        slug='DEFAULT',
    )

    return project


def get_default_project():
    return Project.objects.get_or_create(
        title='DEFAULT',
        defaults={'title': 'DEFAULT', 'nda': False}
    )[0]


def add_user_to_default_project(user):
    default_project = get_default_project()
    UserProject.objects.update_or_create(
        user=user,
        project=default_project,
        defaults={"user": user, "project": default_project}
    )


def add_course_to_default_project(course):
    default_project = get_default_project()
    if course.project != default_project:
        course.project = default_project
        course.save()
