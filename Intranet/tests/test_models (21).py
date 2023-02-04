from builtins import object

import pytest

from django.core.exceptions import ValidationError

from kelvin.courses.models import Course
from kelvin.projects.models import Project, ProjectSubjectItem


@pytest.mark.django_db
class TestProject(object):
    def test_add_code_generation(self):
        """
        Проверяем автогенерацию кода для самодобавления пользователя в проект
        """
        SLUG = 'any_project'
        project = Project(
            slug=SLUG,
            title='any project title'
        )
        project.save()
        import hashlib
        gen = hashlib.md5()
        gen.update(str.encode("Project {}" . format(SLUG)))
        assert(project.add_code == gen.hexdigest())


@pytest.mark.django_db
class TestProjectSubjectItem(object):
    """
    Тесты модели ПроектоПредмета
    """
    project_subject_item_neg_test_data = (
        (ProjectSubjectItem.ItemType.COURSE, None, 'http://test.tld'),
        (ProjectSubjectItem.ItemType.LINK, 1, ''),
        (ProjectSubjectItem.ItemType.LINK, 1, None),
        (ProjectSubjectItem.ItemType.COURSE, None, ''),
        (ProjectSubjectItem.ItemType.COURSE, None, None),
        (ProjectSubjectItem.ItemType.LINK, None, ''),
        (ProjectSubjectItem.ItemType.LINK, None, None),
    )

    @pytest.mark.parametrize('item_type,course,link',
                             project_subject_item_neg_test_data)
    def test_clean_neg(self, item_type, course, link):
        """
        Тест соответствия типа ПроектоПредмета и значений его полей
        """
        with pytest.raises(ValidationError):
            ProjectSubjectItem(
                item_type=item_type,
                course_id=course,
                link=link,
            ).clean()

    project_subject_item_pos_test_data = (
        (
            {
                'id': 1,
                'item_type': ProjectSubjectItem.ItemType.COURSE,
                'course': Course(id=10),
                'link': '',
            },
            u'1: course-10',
        ),
        (
            {
                'id': 11,
                'item_type': ProjectSubjectItem.ItemType.COURSE,
                'course': Course(id=101),
                'link': None,
            },
            u'11: course-101',
        ),
        (
            {
                'id': 2,
                'item_type': ProjectSubjectItem.ItemType.COURSE,
                'course': Course(id=20),
                'link': 'http://test.tld',
            },
            u'2: course-20',
        ),
        (
            {
                'id': 3,
                'item_type': ProjectSubjectItem.ItemType.LINK,
                'course': None,
                'link': 'http://test.tld',
            },
            u'3: link-http://test.tld',
        ),
        (
            {
                'id': 4,
                'item_type': ProjectSubjectItem.ItemType.LINK,
                'course': Course(id=40),
                'link': 'http://test.tld',
            },
            u'4: link-http://test.tld',
        ),
    )

    @pytest.mark.parametrize('create_data,expected_string',
                             project_subject_item_pos_test_data)
    def test_clean_pos(self, create_data, expected_string, mocker):
        """
        Тест соответствия типа ПроектоПредмета и значений его полей
        """
        project_subject_item = ProjectSubjectItem(**create_data)
        project_subject_item.clean()

        assert project_subject_item.__str__() == expected_string, (
            u'Неправильное название проектопредмета')

        if project_subject_item.item_type == ProjectSubjectItem.ItemType.COURSE:
            assert project_subject_item.link is None

        if project_subject_item.item_type == ProjectSubjectItem.ItemType.LINK:
            assert project_subject_item.course is None
