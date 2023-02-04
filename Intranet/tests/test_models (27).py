import random
import time
from builtins import object, str

import pytest

from django.contrib.contenttypes.models import ContentType

from kelvin.accounts.models import User, UserProject
from kelvin.accounts.utils import get_user_courses
from kelvin.common.utils_for_tests import CaptureQueriesContext
from kelvin.courses.models import AssignmentRule, Course, CourseStudent
from kelvin.projects.models import Project
from kelvin.tags.models import (
    Tag, TaggedObject, TagTypeAdapter, TagTypeChiefDuration, TagTypeCity, TagTypeCourse, TagTypeGenericInteger,
    TagTypeGenericString, TagTypeIsChief, TagTypeProject, TagTypeStaffCity, TagTypeStaffGroup, TagTypeStaffOffice,
    TagTypeStaffStartDate, TagTypeStaffTimeFromStartDate, TagTypeStudyGroup, TagTypeUser, TagTypeUserNativeLang,
)
from kelvin.tags.utils import get_user_staff_groups


def _rand():
    random.seed(time.time())
    return str(random.randrange(1, 32767))


def _set_from_query_set(queryset=None):
    """
    Converts Django QuerySet to native python set
    :param queryset: input parameter considered to be a django QuerySet instance
    :return: queryset converted to native python set ( to be able to use in test comparisions )
    """
    result = set([])
    if queryset is None:
        return result

    for row in queryset:
        result.add(row)

    return result


class TestTagTypeAdapterInterface(object):
    def test_get_tag_type_choices(self):
        choices = TagTypeAdapter.get_tag_type_choices()
        assert choices == [
            ['GNRSTR', u'GenericString'],
            ['GNRINT', u'GenericInteger'],
            ['CTY', u'City'],
            ['MOEGRP', u'MoebiusGroup'],
            ['PRJ', u'Project'],
            ['CRS', u'Course'],
            ['USR', u'User'],
            ['STGRP', u'StaffGroup'],
            ['STDT', u'StaffStartDate'],
            ['STFSD', u'StaffTimeFromStartDate'],
            ['ISCHF', u'IsChief'],
            ['CHFDRTN', u'ChiefDuration'],
            ['USRNTLANG', u'UserNativeLang'],
            ['STCTY', u'StaffCity'],
            ['STOFF', u'StaffOffice'],
            ['STHRBP', 'StaffHRBP'],
            ['STCHF', 'StaffChief'],
        ]

    def test_get_tag_db_types(self):
        db_types = TagTypeAdapter.get_tag_db_types()
        assert db_types == [
            'GNRSTR', 'GNRINT', 'CTY', 'MOEGRP', 'PRJ', 'CRS', 'USR', 'STGRP', 'STDT', 'STFSD', 'ISCHF',
            'CHFDRTN', 'USRNTLANG', 'STCTY', 'STOFF', 'STHRBP', 'STCHF',
        ]

    def test_get_language_tag_type_for_db_type(self):

        expected_results_map = {
            'GNRSTR': 'string',
            'GNRINT': 'integer',
            'CTY': 'string',
            'PRJ': 'integer',
            'CRS': 'integer',
            'USR': 'integer',
            'STGRP': 'string',
            'STDT': 'date',
            'STFSD': 'integer',
            'ISCHF': 'bool',
            'CHFDRTN': 'integer',
            'MOEGRP': 'string',
            'USRNTLANG': 'string',
            'STOFF': 'string',
            'STCTY': 'string',
        }

        for tag_db_type, tag_language_type_expected in expected_results_map.items():
            assert TagTypeAdapter.get_language_tag_type_for_db_type(tag_db_type) == tag_language_type_expected

    def test_get_tag_type_for_db_type(self):
        expected_results_map = {
            'GNRSTR': TagTypeGenericString,
            'GNRINT': TagTypeGenericInteger,
            'CTY': TagTypeCity,
            'PRJ': TagTypeProject,
            'CRS': TagTypeCourse,
            'USR': TagTypeUser,
            'STGRP': TagTypeStaffGroup,
            'STDT': TagTypeStaffStartDate,
            'STFSD': TagTypeStaffTimeFromStartDate,
            'ISCHF': TagTypeIsChief,
            'CHFDRTN': TagTypeChiefDuration,
            'MOEGRP': TagTypeStudyGroup,
            'USRNTLANG': TagTypeUserNativeLang,
            'STOFF': TagTypeStaffOffice,
            'STCTY': TagTypeStaffCity,
        }
        for tag_db_type, tag_type_expected in expected_results_map.items():
            assert TagTypeAdapter.get_tag_type_for_db_type(tag_db_type) == tag_type_expected

    def test_get_tag_type_for_semantic_type(self):
        expected_results_map = {
            'GenericString': TagTypeGenericString,
            'GenericInteger': TagTypeGenericInteger,
            'City': TagTypeCity,
            'Project': TagTypeProject,
            'Course': TagTypeCourse,
            'User': TagTypeUser,
            'StaffGroup': TagTypeStaffGroup,
            'StaffStartDate': TagTypeStaffStartDate,
            'StaffTimeFromStartDate': TagTypeStaffTimeFromStartDate,
            'IsChief': TagTypeIsChief,
            'ChiefDuration': TagTypeChiefDuration,
            'MoebiusGroup': TagTypeStudyGroup,
            'UserNativeLang': TagTypeUserNativeLang,
            'StaffOffice': TagTypeStaffOffice,
            'StaffCity': TagTypeStaffCity,
        }
        for tag_semantic_type, tag_type_expected in expected_results_map.items():
            assert TagTypeAdapter.get_tag_type_for_semantic_type(tag_semantic_type) == tag_type_expected

    def test_get_dynamic_tag_types(self):
        expected_result = [
            TagTypeStaffStartDate, TagTypeStaffTimeFromStartDate,
            TagTypeIsChief, TagTypeChiefDuration,
        ]
        assert TagTypeAdapter.get_dynamic_tag_types() == expected_result

    def test_visible_returns_bool(self):
        for tag_type in TagTypeAdapter.get_available_tag_types():
            assert type(tag_type.visible()) is bool


class TestTagTypeInterfaces(object):
    """
    Тесты интерфейсов иерархии классов типов тегов
    """

    def test_get_operations(self):
        """
        Проверяем, что все поддерживамые типы тегов адекватно реагируют на get_operations
        """
        available_tag_types = TagTypeAdapter.get_available_tag_types()
        for available_tag_type in available_tag_types:
            operations = available_tag_type.get_operations()
            assert type(operations) == list
            assert len(operations) > 0

    def test_get_operations(self):
        """
        Проверяем, что get_source_values_... всегд возвращает list
        TODO: добавить фикстуру пользователя со всеми типами тегов и дернуть тестируемую ф-ю для каждого типа тегов
        """
        pass


class TestGetStaffGroupsOrdered(object):
    @pytest.mark.django_db
    def test_get_staff_groups_positive(self, simple_user, simple_project):
        """
        Тегируем пользователя тремя группами разных уровней.
        Вызываем тестируемую функцию 'get_user_staff_groups' и
        проверяем, что отданы все три группы, упорядоченные по 'level'
        """
        tags = Tag.objects.bulk_create([
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                project=simple_project,
                value="group1",
                data={
                    "level": "level1"
                }
            ),
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                project=simple_project,
                value="group3",
                data={
                    "level": "level3"
                }
            ),
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                project=simple_project,
                value="group2",
                data={
                    "level": "level2"
                }
            )
        ])

        user_content_type = ContentType.objects.get_for_model(User)
        TaggedObject.objects.bulk_create([
            TaggedObject(
                object_id=simple_user.id,
                tag_id=tags[0].id,
                content_type=user_content_type
            ),
            TaggedObject(
                object_id=simple_user.id,
                tag_id=tags[1].id,
                content_type=user_content_type
            ),
            TaggedObject(
                object_id=simple_user.id,
                tag_id=tags[2].id,
                content_type=user_content_type
            ),
        ])

        staff_groups = get_user_staff_groups(simple_user.id)

        assert staff_groups == ['group1', 'group2', 'group3']

    @pytest.mark.django_db
    def test_get_staff_groups_no_level_partial(self, simple_user, simple_project):
        """
        Проверяем случай, когда в одном из тегов групп нет level.
        Особенность postgres в том, что если пытаемся сортировать по несуществующему ключу, то
        исключения не возникает - сначала отдаются все записи, где ключ есть в отсортированном виде,
        а потом к ним добавляются записи, у которых ключа нет
        """
        tags = Tag.objects.bulk_create([
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                project=simple_project,
                value="group1",
                data={
                    "_level_": 1
                }
            ),
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                project=simple_project,
                value="group3",
                data={
                    "level": 3
                }
            ),
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                project=simple_project,
                value="group2",
                data={
                    "level": 2
                }
            ),
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                project=simple_project,
                value="group4",
                data={
                    "level": 5
                }
            ),

        ])

        user_content_type = ContentType.objects.get_for_model(User)
        TaggedObject.objects.bulk_create([
            TaggedObject(
                object_id=simple_user.id,
                tag_id=tags[0].id,
                content_type=user_content_type
            ),
            TaggedObject(
                object_id=simple_user.id,
                tag_id=tags[1].id,
                content_type=user_content_type
            ),
            TaggedObject(
                object_id=simple_user.id,
                tag_id=tags[3].id,
                content_type=user_content_type
            ),
            TaggedObject(
                object_id=simple_user.id,
                tag_id=tags[2].id,
                content_type=user_content_type
            ),
        ])

        staff_groups = get_user_staff_groups(simple_user.id)

        assert staff_groups == ['group2', 'group3', 'group4', 'group1']

    @pytest.mark.django_db
    def test_get_staff_groups_no_level_at_all(self, simple_user, simple_project):
        """
        Проверяем случай, когда у всей выборки из тегов групп нет level.
        Данные могут отдаваться в рандомном порядке, поэтому мы не проверяем отсортированность,
        а проверяем лишь эквивалентность множеств и их мощность.
        Исключений возникать не должно.
        """
        tags = Tag.objects.bulk_create([
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                project=simple_project,
                value="group1",
                data={
                    "_level_": 1
                }
            ),
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                project=simple_project,
                value="group3",
                data={
                    "_level_": 3
                }
            ),
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                project=simple_project,
                value="group2",
                data={
                    "_level_": 2
                }
            ),
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                project=simple_project,
                value="group4",
                data={
                    "_level_": 5
                }
            ),

        ])

        user_content_type = ContentType.objects.get_for_model(User)
        TaggedObject.objects.bulk_create([
            TaggedObject(
                object_id=simple_user.id,
                tag_id=tags[0].id,
                content_type=user_content_type
            ),
            TaggedObject(
                object_id=simple_user.id,
                tag_id=tags[1].id,
                content_type=user_content_type
            ),
            TaggedObject(
                object_id=simple_user.id,
                tag_id=tags[3].id,
                content_type=user_content_type
            ),
            TaggedObject(
                object_id=simple_user.id,
                tag_id=tags[2].id,
                content_type=user_content_type
            ),
        ])

        staff_groups = get_user_staff_groups(simple_user.id)

        assert set(staff_groups) == set(['group1', 'group3', 'group2', 'group4'])


class TestTaggedObjectModel(object):
    """
    Тесты для модели тегов
    """
    @pytest.mark.django_db
    def test_get_tags_for_object(self, simple_project, simple_course):
        """Тест `Subject.__str__`"""
        tag = Tag(
            type=TagTypeCourse.get_db_type(),
            project=simple_project,
            value="тытышкин тег"
        )
        tag.save()

        tagged_object = TaggedObject(
            tag=tag,
            object_id=simple_course.id,
            content_type=ContentType.objects.get_for_model(Course)
        )
        tagged_object.save()

        tags = TaggedObject.get_tags_for_object(
            Course,
            simple_course.id
        )

        assert tags == set([tag])

    @pytest.mark.django_db
    def test_get_type_tags(self, simple_project, simple_course, simple_user):
        """Тест `Subject.__str__`"""
        course_tag = Tag(
            type=TagTypeCourse.get_db_type(),
            project=simple_project,
            value=simple_course.id,
        )
        course_tag.save()

        user_tag = Tag(
            type=TagTypeUser.get_db_type(),
            project=simple_project,
            value=simple_user.id,
        )
        user_tag.save()

        project_tag = Tag(
            type=TagTypeUser.get_db_type(),
            project=simple_project,
            value=simple_project.id,
        )
        project_tag.save()

        users_course = TaggedObject(
            tag=course_tag,
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User)
        )
        users_course.save()

        users_project = TaggedObject(
            tag=project_tag,
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User)
        )
        users_project.save()

        courses_project = TaggedObject(
            tag=project_tag,
            object_id=simple_course.id,
            content_type=ContentType.objects.get_for_model(Course)
        )
        courses_project.save()

        tags = TaggedObject.get_tags_for_type(Course)

        assert tags == set([project_tag])

        tags = TaggedObject.get_tags_for_type(User)

        assert tags == set([course_tag, project_tag])

        tags = TaggedObject.get_tags_for_type(Project)

        assert tags == set()

        assert TaggedObject.objects.count() == 3

        TaggedObject.objects.all().delete()

    @pytest.mark.django_db
    def test_get_matched_objects(self, simple_project, simple_course, simple_user, rgb_custom_tags):
        """ Матчим по двум типам тегов """
        user_type = ContentType.objects.get_for_model(User)
        course_type = ContentType.objects.get_for_model(Course)

        second_course = Course.objects.get(pk=simple_course.id)
        second_course.pk = None
        second_course.code = None
        second_course.name = simple_course.name + _rand()
        second_course.save()

        second_project = Project.objects.get(pk=simple_project.id)
        second_project.pk = None
        second_project.add_code = None
        second_project.title = simple_project.title + _rand()
        second_project.slug = second_project.slug + _rand()
        second_project.save()

        # тегируем пользователя двумя цветами: red, green
        TaggedObject(
            object_id=simple_user.id,
            content_type=user_type,
            tag=rgb_custom_tags["red"]
        ).save()

        TaggedObject(
            object_id=simple_user.id,
            content_type=user_type,
            tag=rgb_custom_tags["green"]
        ).save()

        # тегируем курс 1: всеми тремя цветами
        TaggedObject(
            object_id=simple_course.id,
            content_type=course_type,
            tag=rgb_custom_tags["red"]
        ).save()

        TaggedObject(
            object_id=simple_course.id,
            content_type=course_type,
            tag=rgb_custom_tags["green"]
        ).save()

        # тегируем курс 2: blue
        TaggedObject(
            object_id=second_course.id,
            content_type=course_type,
            tag=rgb_custom_tags["blue"]
        ).save()

        # тегируем курс 1 и пользователя 2 одним и тем же проектом
        project_tag_1 = Tag.objects.get(
            type=TagTypeProject.get_db_type(),
            project=simple_project,
            value=simple_project.id,
        )

        project_tag_2 = Tag.objects.get(
            type=TagTypeProject.get_db_type(),
            project=second_project,
            value=second_project.id,
        )

        TaggedObject(
            object_id=simple_course.id,
            content_type=course_type,
            tag=project_tag_1,
        ).save()

        TaggedObject(
            object_id=simple_user.id,
            content_type=user_type,
            tag=project_tag_1,
        ).save()

        # матчим
        result = _set_from_query_set(TaggedObject.get_matched_objects(
            object_id=simple_user.id,
            object_model=User,
            match_model=Course
        ))

        # ожидаем, что сматчится только курс 1, потому как все его теги совпали
        # а у курса 2 есть тег, которого нет у пользователя
        assert(result == set([simple_course, ]))

        # убираем за собой в базе
        TaggedObject.objects.all().delete()

    @pytest.mark.django_db
    def test_nothing_matched(self, simple_project, simple_course, simple_user, rgb_custom_tags):
        """ проверяем случай, когда курс матчится по кастомным тегам , но не матчится по проекту """
        user_type = ContentType.objects.get_for_model(User)
        course_type = ContentType.objects.get_for_model(Course)

        second_course = Course.objects.get(pk=simple_course.id)
        second_course.pk = None
        second_course.code = None
        second_course.name = simple_course.name + _rand()
        second_course.save()

        second_project = Project.objects.get(pk=simple_project.id)
        second_project.pk = None
        second_project.add_code = None
        second_project.title = simple_project.title + _rand()
        second_project.slug = second_project.slug + _rand()
        second_project.save()

        # тегируем пользователя двумя цветами: red, green
        TaggedObject(
            object_id=simple_user.id,
            content_type=user_type,
            tag=rgb_custom_tags["red"]
        ).save()

        TaggedObject(
            object_id=simple_user.id,
            content_type=user_type,
            tag=rgb_custom_tags["green"]
        ).save()

        # тегируем курс 1: всеми тремя цветами
        TaggedObject(
            object_id=simple_course.id,
            content_type=course_type,
            tag=rgb_custom_tags["red"]
        ).save()

        TaggedObject(
            object_id=simple_course.id,
            content_type=course_type,
            tag=rgb_custom_tags["green"]
        ).save()

        # тегируем курс 2: blue
        TaggedObject(
            object_id=second_course.id,
            content_type=course_type,
            tag=rgb_custom_tags["blue"]
        ).save()

        # тегируем курс 1 и пользователя 2 разными проектами
        project_tag_1 = Tag.objects.get(
            type=TagTypeProject.get_db_type(),
            project=simple_project,
            value=simple_project.id,
        )

        project_tag_2 = Tag.objects.get(
            type=TagTypeProject.get_db_type(),
            project=second_project,
            value=second_project.id,
        )

        TaggedObject(
            object_id=simple_course.id,
            content_type=course_type,
            tag=project_tag_1,
        ).save()

        TaggedObject(
            object_id=simple_user.id,
            content_type=user_type,
            tag=project_tag_2,
        ).save()

        # матчим
        result = _set_from_query_set(TaggedObject.get_matched_objects(
            object_id=simple_user.id,
            object_model=User,
            match_model=Course
        ))

        # ожидаем, что сматчится только курс 1, потому как все его теги совпали
        # а у курса 2 есть тег, которого нет у пользователя
        assert(result == set([]))

        # убираем за собой в базе
        TaggedObject.objects.all().delete()


class TestProjectTagAutocreation(object):
    """
    Тесты для автосоздания тегов проектов
    """
    @pytest.mark.django_db
    def test_project_tag_autocreation(self, simple_project):
        Tag.objects.get(
            project=simple_project,
            type=TagTypeProject.get_db_type(),
            value=simple_project.id,
        )


class TestGetUserCourses(object):
    """
    Тесты для проверки получения списка курсов по правилам назначения
    """
    @pytest.mark.django_db
    def test_get_course_by_static_tags_1(self, simple_project, simple_course, simple_user):
        """
            Кейс "Правила пользователя шире правил курса. Курс матчится."
            Создаем пользователя, который тегирован городом Г1 и группой Гр1
            Создаем курс К1, который имеет правило назначения, в котором только одно правило "быть из города Г1"
            Проверяем, что курс К1 отдается пользователю
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        moscow_tag = Tag(
            value=u'Москва',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        moscow_tag.save()

        nur_tag = Tag(
            value=u'Новый Уренгой',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        nur_tag.save()

        gpd_tag = Tag(
            value=u'Группа подготовки данных',
            type=TagTypeStaffGroup.get_db_type(),
            project=None
        )
        gpd_tag.save()

        user_tagged_by_city_nur = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=nur_tag,
        )
        user_tagged_by_city_nur.save()

        user_tagged_by_group_gpd = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=gpd_tag,
        )
        user_tagged_by_group_gpd.save()

        assignment_rule = AssignmentRule(
            course=simple_course,
            mandatory=False,
            title=u"Новоуренгойцы",
            formula=[
                [
                    {
                        "semantic_type": "City",
                        "operation": "==",
                        "id": nur_tag.id,
                        "value": u"Новый Уренгой",
                    },
                ],
            ],
        )
        assignment_rule.save()

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            user_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 4
        assert user_courses == [simple_course]

        # CLEAR
        user_tagged_by_city_nur.delete()
        user_tagged_by_group_gpd.delete()
        moscow_tag.delete()
        nur_tag.delete()
        gpd_tag.delete()

    @pytest.mark.django_db
    def test_get_course_by_static_tags_2(self, simple_project, simple_course, simple_user):
        """
            Кейс "Пользователь частично соответствует правилу назначения. Курс не матчится."
            Создаем пользователя, который тегирован городом Г1 и группой Гр1
            Создаем курс К1, который имеет правило назначения, в котором правила:
                - "быть из города Г1"
                - "быть из группы Гр2
            Проверяем, что курс К1 НЕ отдается пользователю
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        moscow_tag = Tag(
            value=u'Москва',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        moscow_tag.save()

        nur_tag = Tag(
            value=u'Новый Уренгой',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        nur_tag.save()

        group_tag_1 = Tag(
            value=u'Группа подготовки данных',
            type=TagTypeStaffGroup.get_db_type(),
            project=None
        )
        group_tag_1.save()

        group_tag_2 = Tag(
            value=u'Группа захвата',
            type=TagTypeStaffGroup.get_db_type(),
            project=None
        )
        group_tag_2.save()

        user_tagged_by_city_nur = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=nur_tag,
        )
        user_tagged_by_city_nur.save()

        user_tagged_by_group_group_1 = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=group_tag_1,
        )
        user_tagged_by_group_group_1.save()

        assignment_rule = AssignmentRule(
            course=simple_course,
            mandatory=False,
            title=u"Новоуренгойцы",
            formula=[
                [
                    {
                        "semantic_type": "City",
                        "operation": "==",
                        "id": nur_tag.id,
                        "value": u"Новый Уренгой",
                    },
                ],
                [
                    {
                        "semantic_type": "StaffGroup",
                        "operation": "==",
                        "id": group_tag_2.id,
                        "value": u"Группа захвата",
                    },
                ],
            ],
        )
        assignment_rule.save()

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            user_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 4
        assert user_courses == []

        # CLEAR
        user_tagged_by_city_nur.delete()
        user_tagged_by_group_group_1.delete()
        moscow_tag.delete()
        nur_tag.delete()
        group_tag_1.delete()
        group_tag_2.delete()

    @pytest.mark.django_db
    def test_get_course_by_static_tags_3(self, simple_project, simple_course, simple_user):
        """
            Кейс "Пользователь всеми тегами соответствует правилу назначения"
            Создаем пользователя, который тегирован городом Г1 и группой Гр1
            Создаем курс К1, который имеет правило назначения, в котором правила:
                - "быть из города Г1"
                - "быть из группы Гр1"
            Проверяем, что курс К1 отдается пользователю
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        moscow_tag = Tag(
            value=u'Москва',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        moscow_tag.save()

        nur_tag = Tag(
            value=u'Новый Уренгой',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        nur_tag.save()

        group_tag_1 = Tag(
            value=u'Группа подготовки данных',
            type=TagTypeStaffGroup.get_db_type(),
            project=None
        )
        group_tag_1.save()

        user_tagged_by_city_nur = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=nur_tag,
        )
        user_tagged_by_city_nur.save()

        user_tagged_by_group_group_1 = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=group_tag_1,
        )
        user_tagged_by_group_group_1.save()

        assignment_rule = AssignmentRule(
            course=simple_course,
            mandatory=False,
            title=u"Новоуренгойцы",
            formula=[
                [
                    {
                        "semantic_type": "City",
                        "operation": "==",
                        "id": nur_tag.id,
                        "value": u"Новый Уренгой",
                    },
                ],
                [
                    {
                        "semantic_type": "StaffGroup",
                        "operation": "==",
                        "id": group_tag_1.id,
                        "value": u"Группа подготовки данных",
                    },
                ],
            ],
        )
        assignment_rule.save()

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            user_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 4
        assert user_courses == [simple_course]

        # CLEAR
        user_tagged_by_city_nur.delete()
        user_tagged_by_group_group_1.delete()
        moscow_tag.delete()
        nur_tag.delete()
        group_tag_1.delete()

    @pytest.mark.django_db
    def test_get_course_by_static_tags_4(self, simple_project, simple_course, simple_user):
        """
            Кейс "Пользователь всеми тегами соответствует правилу назначения, в котором есть или для каждого типа тегов"
            Создаем пользователя, который тегирован городом Г1 и группой Гр1
            Создаем курс К1, который имеет правило назначения, в котором правила:
                - "быть из города Г1 ИЛИ быть из города Г2"
                - "быть из группы Гр1 ИЛИ быть из группы Гр2"
            Проверяем, что курс К1 отдается пользователю
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        moscow_tag = Tag(
            value=u'Москва',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        moscow_tag.save()

        nur_tag = Tag(
            value=u'Новый Уренгой',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        nur_tag.save()

        group_tag_1 = Tag(
            value=u'Группа подготовки данных',
            type=TagTypeStaffGroup.get_db_type(),
            project=None
        )
        group_tag_1.save()

        group_tag_2 = Tag(
            value=u'Группа захвата',
            type=TagTypeStaffGroup.get_db_type(),
            project=None
        )
        group_tag_2.save()

        user_tagged_by_city_nur = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=nur_tag,
        )
        user_tagged_by_city_nur.save()

        user_tagged_by_group_group_1 = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=group_tag_1,
        )
        user_tagged_by_group_group_1.save()

        assignment_rule = AssignmentRule(
            course=simple_course,
            mandatory=False,
            title=u"Новоуренгойцы",
            formula=[
                [
                    {
                        "semantic_type": "City",
                        "operation": "==",
                        "id": nur_tag.id,
                        "value": u"Новый Уренгой",
                    },
                    {
                        "semantic_type": "City",
                        "operation": "==",
                        "id": moscow_tag.id,
                        "value": u"Москва",
                    },
                ],
                [
                    {
                        "semantic_type": "StaffGroup",
                        "operation": "==",
                        "id": group_tag_1.id,
                        "value": u"Группа подготовки данных",
                    },
                    {
                        "semantic_type": "StaffGroup",
                        "operation": "==",
                        "id": group_tag_2.id,
                        "value": u"Группа захвата",
                    },
                ],
            ],
        )
        assignment_rule.save()

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            user_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 4
        assert user_courses == [simple_course]

        # CLEAR
        user_tagged_by_city_nur.delete()
        user_tagged_by_group_group_1.delete()
        moscow_tag.delete()
        nur_tag.delete()
        group_tag_1.delete()
        group_tag_2.delete()

    @pytest.mark.django_db
    def test_get_course_by_static_tags_5(self, simple_project, simple_course, simple_user):
        """
            Кейс "Проверка отдачи нескольких курсов. Чать курсов не отдается."
            Создаем пользователя, который тегирован городом Г1 и группой Гр2
            Создаем курс К1, который имеет правило назначения, в котором правила:
                - "быть из города Г1 ИЛИ быть из города Г2"
            Создаем курс К2, который имеет правило назначения, в котором правила:
                - "быть из группы Гр1 ИЛИ быть из группы Гр2"
            Создаем курс К3, который имеет правило назначения, в котором правила:
                - "быть из города Г1"
                - "быть из группы Гр1"

            Проверяем, что курсы К1,К2 отдаются пользователю, а  курс К3 - не отдаётся
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        course2 = Course.objects.get(pk=simple_course.id)
        course2.pk = None
        course2.code = None
        course2.name = simple_course.name + _rand()
        course2.save()

        course3 = Course.objects.get(pk=simple_course.id)
        course3.pk = None
        course3.code = None
        course3.name = simple_course.name + _rand()
        course3.save()

        moscow_tag = Tag(
            value=u'Москва',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        moscow_tag.save()

        nur_tag = Tag(
            value=u'Новый Уренгой',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        nur_tag.save()

        group_tag_1 = Tag(
            value=u'Группа подготовки данных',
            type=TagTypeStaffGroup.get_db_type(),
            project=None
        )
        group_tag_1.save()

        group_tag_2 = Tag(
            value=u'Группа захвата',
            type=TagTypeStaffGroup.get_db_type(),
            project=None
        )
        group_tag_2.save()

        user_tagged_by_city_nur = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=nur_tag,
        )
        user_tagged_by_city_nur.save()

        user_tagged_by_group_group_2 = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=group_tag_2,
        )
        user_tagged_by_group_group_2.save()

        assignment_rule_course_1 = AssignmentRule.objects.create(
            course=simple_course,
            mandatory=False,
            title=u"Новоуренгойцы или москвичи",
            formula=[
                [
                    {
                        "semantic_type": "City",
                        "operation": "==",
                        "id": nur_tag.id,
                        "value": u"Новый Уренгой",
                    },
                    {
                        "semantic_type": "City",
                        "operation": "==",
                        "id": moscow_tag.id,
                        "value": u"Москва",
                    },
                ],
            ],
        )

        assignment_rule_course_2 = AssignmentRule.objects.create(
            course=course2,
            mandatory=False,
            title=u"ГПД или спецназ",
            formula=[
                [
                    {
                        "semantic_type": "StaffGroup",
                        "operation": "==",
                        "id": group_tag_1.id,
                        "value": u"Группа подготовки данных",
                    },
                    {
                        "semantic_type": "StaffGroup",
                        "operation": "==",
                        "id": group_tag_2.id,
                        "value": u"Группа захвата",
                    },
                ],
            ],
        )

        assignment_rule_course_3 = AssignmentRule.objects.create(
            course=course3,
            mandatory=False,
            title=u"Новоуренгойцы ГПДшники",
            formula=[
                [
                    {
                        "semantic_type": "City",
                        "operation": "==",
                        "id": nur_tag.id,
                        "value": u"Новый Уренгой",
                    },
                ],
                [
                    {
                        "semantic_type": "StaffGroup",
                        "operation": "==",
                        "id": group_tag_1.id,
                        "value": u"Группа подготовки данных",
                    },
                ],
            ],
        )

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            user_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 6
        assert set(user_courses) == set([course2, simple_course])

        # CLEAR
        user_tagged_by_city_nur.delete()
        user_tagged_by_group_group_2.delete()
        moscow_tag.delete()
        nur_tag.delete()
        group_tag_1.delete()
        group_tag_2.delete()
        assignment_rule_course_1.delete()
        assignment_rule_course_2.delete()
        assignment_rule_course_3.delete()

    @pytest.mark.django_db
    def test_get_course_by_static_tags_6(self, simple_project, simple_course, simple_user):
        """
            Кейс "Проверка операции отрицания"
            Создаем пользователя, который тегирован городом Г1
            Создаем курс К1, который имеет правило назначения, в котором только одно правило "быть из города Г1"
            Создаем курс К2, который имеет правило назначения, в котором только одно правило "быть из города Г2"
            Проверяем, что курс К1 отдается пользователю
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        course2 = Course.objects.get(pk=simple_course.id)
        course2.pk = None
        course2.code = None
        course2.name += _rand()
        course2.save()

        moscow_tag = Tag(
            value=u'Москва',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        moscow_tag.save()

        nur_tag = Tag(
            value=u'Новый Уренгой',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        nur_tag.save()

        user_tagged_by_city_nur = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=nur_tag,
        )
        user_tagged_by_city_nur.save()

        assignment_rule_1 = AssignmentRule.objects.create(
            course=course2,
            mandatory=False,
            title=u"Не москвичи",
            formula=[
                [
                    {
                        "semantic_type": "City",
                        "operation": "!=",
                        "id": moscow_tag.id,
                        "value": u"Москва",
                    },
                ],
            ],
        )

        assignment_rule_2 = AssignmentRule.objects.create(
            course=simple_course,
            mandatory=False,
            title=u"Москвичи",
            formula=[
                [
                    {
                        "semantic_type": "City",
                        "operation": "==",
                        "id": moscow_tag.id,
                        "value": u"Москва",
                    },
                ],
            ],
        )

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            user_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 5
        assert user_courses == [course2]

        # CLEAR
        user_tagged_by_city_nur.delete()
        moscow_tag.delete()
        nur_tag.delete()
        assignment_rule_1.delete()
        assignment_rule_2.delete()

    @pytest.mark.django_db
    @pytest.mark.parametrize(
        "re_to_match,result",
        [
            ("^.*$", pytest.lazy_fixture('simple_course')),
            ("\d+", None),
            ("^Зачем.*дары.*$", pytest.lazy_fixture('simple_course')),
            ("Зачем рабам дары свободы", pytest.lazy_fixture('simple_course'))
        ]
    )
    def test_get_course_by_static_tags_7(self, simple_project, simple_course, simple_user, re_to_match, result):
        """
            Кейс "Проверка операции regexp"
            Создаем пользователя, который тегирован произвольным строковым тегом (TagTypeGenericString)
            Создаем курс К1, который имеет правило назначения, в котором только одно правило "матчить по подстроке"
            Проверяем, что курс К1 отдается пользователю
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        generic_string_tag = Tag(
            value=u'Зачем рабам дары свободы, их не разбудит чести клич...',
            type=TagTypeGenericString.get_db_type(),
            project=None
        )
        generic_string_tag.save()

        user_tagged_by_generic_tag = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=generic_string_tag,
        )
        user_tagged_by_generic_tag.save()

        assignment_rule_1 = AssignmentRule.objects.create(
            course=simple_course,
            mandatory=False,
            title=u"Пушкин - наше фсио",
            formula=[
                [
                    {
                        "semantic_type": "GenericString",
                        "operation": "regexp",
                        "id": generic_string_tag.id,
                        "value": re_to_match,
                    },
                ],
            ],
        )

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            user_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 4
        expected_courses = [result] if result is not None else []
        assert user_courses == expected_courses

        # CLEAR
        user_tagged_by_generic_tag.delete()
        generic_string_tag.delete()
        assignment_rule_1.delete()

    @pytest.mark.django_db
    @pytest.mark.parametrize(
        "operation,rule,result",
        [
            (">", 99, pytest.lazy_fixture('simple_course')),
            ("<", 101, pytest.lazy_fixture('simple_course')),
            (">=", -100, pytest.lazy_fixture('simple_course')),
            (">=", 98, pytest.lazy_fixture('simple_course')),
            ("<=", 100, pytest.lazy_fixture('simple_course')),
            ("<=", 101, pytest.lazy_fixture('simple_course')),
            ("==", 100, pytest.lazy_fixture('simple_course')),
            ("!=", 0, pytest.lazy_fixture('simple_course')),

            (">", 100, None),
            ("<", 99, None),
            (">=", 199, None),
            ("<=", 99, None),
            ("==", -100, None),
            ("!=", 100, None)
        ]
    )
    def test_get_course_by_static_tags_8(self, simple_project, simple_course, simple_user, operation, rule, result):
        """
            Кейс "Проверка сравнительных операций с произвольным числовым тегом"
            Создаем пользователя, который тегирован произвольным строковым тегом (TagTypeInteger)
            Создаем курс К1, который имеет правило назначения, в котором только одно правило "матчить по подстроке"
            Проверяем, что курс К1 отдается пользователю
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        generic_integer_tag = Tag(
            value=100,
            type=TagTypeGenericInteger.get_db_type(),
            project=None
        )
        generic_integer_tag.save()

        user_tagged_by_generic_tag = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=generic_integer_tag,
        )
        user_tagged_by_generic_tag.save()

        assignment_rule_1 = AssignmentRule.objects.create(
            course=simple_course,
            mandatory=False,
            title=u"Сравниваем с числом 100",
            formula=[
                [
                    {
                        "semantic_type": "GenericInteger",
                        "operation": operation,
                        "id": generic_integer_tag.id,
                        "value": rule,
                    },
                ],
            ],
        )

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            user_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 4
        expected_courses = [result] if result is not None else []
        assert user_courses == expected_courses

        # CLEAR
        user_tagged_by_generic_tag.delete()
        generic_integer_tag.delete()
        assignment_rule_1.delete()

    @pytest.mark.django_db
    def test_get_course_by_static_tags_9(self, simple_project, simple_course, simple_user):
        """
            Кейс "Проверка нематчинга, когда у пользователя нет тегов"
            Создаем пользователя, который не тегирован
            Создаем курс К1, который имеет простое правило назначения
            Проверяем, что курс К1 НЕ отдается пользователю
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        generic_integer_tag = Tag(
            value=100,
            type=TagTypeGenericInteger.get_db_type(),
            project=None
        )
        generic_integer_tag.save()

        assignment_rule_1 = AssignmentRule.objects.create(
            course=simple_course,
            mandatory=False,
            title=u"Сравниваем с числом 100",
            formula=[
                [
                    {
                        "semantic_type": "GenericInteger",
                        "operation": "==",
                        "id": generic_integer_tag.id,
                        "value": 100,
                    },
                ],
            ],
        )

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            user_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 4
        assert user_courses == []

        # CLEAR
        generic_integer_tag.delete()
        assignment_rule_1.delete()

    @pytest.mark.django_db
    def test_get_course_by_static_tags_10(self, simple_project, simple_course, simple_user):
        """
            Кейс "Проверка нематчинга, когда у курса нет правил назначения"
            Создаем пользователя, который тегирован простым тегом
            Создаем курс К1, который не имеет правил назначения
            Проверяем, что курс К1 НЕ отдается пользователю
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        generic_integer_tag = Tag(
            value=100,
            type=TagTypeGenericInteger.get_db_type(),
            project=None
        )
        generic_integer_tag.save()

        user_tagged_by_generic_tag = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=generic_integer_tag,
        )
        user_tagged_by_generic_tag.save()

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            user_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 3
        assert user_courses == []

        # CLEAR
        user_tagged_by_generic_tag.delete()
        generic_integer_tag.delete()

    @pytest.mark.django_db
    def test_get_course_by_static_tags_11(self, simple_project, simple_course, simple_user):
        """
            Кейс "Проверка нематчинга, когда курс матчится, но он в другом проекте"
            Создаем пользователя, который тегирован простым тегом
            Создаем курс К1, который имеет подходящее правило назначения
            Проверяем, что курс К1 НЕ отдается пользователю
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = None
        simple_course.save()

        generic_integer_tag = Tag(
            value=100,
            type=TagTypeGenericInteger.get_db_type(),
            project=None
        )
        generic_integer_tag.save()

        user_tagged_by_generic_tag = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=generic_integer_tag,
        )
        user_tagged_by_generic_tag.save()

        assignment_rule_1 = AssignmentRule.objects.create(
            course=simple_course,
            mandatory=False,
            title=u"Сравниваем с числом 100",
            formula=[
                [
                    {
                        "semantic_type": "GenericInteger",
                        "operation": "==",
                        "id": generic_integer_tag.id,
                        "value": 100,
                    },
                ],
            ],
        )

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            user_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 1
        assert user_courses == []

        # CLEAR
        user_tagged_by_generic_tag.delete()
        generic_integer_tag.delete()
        assignment_rule_1.delete()

    @pytest.mark.xfail
    @pytest.mark.django_db
    def test_get_course_by_static_tags_12(self, simple_project, simple_course, simple_user):
        """
            Кейс "Неподдерживаемая операция"
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        generic_integer_tag = Tag(
            value=100,
            type=TagTypeGenericInteger.get_db_type(),
            project=None
        )
        generic_integer_tag.save()

        user_tagged_by_generic_tag = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=generic_integer_tag,
        )
        user_tagged_by_generic_tag.save()
        assignment_rule_1 = AssignmentRule.objects.create(
            course=simple_course,
            mandatory=False,
            title=u"Сравниваем с числом 100",
            formula=[
                [
                    {
                        "semantic_type": "GenericInteger",
                        "operation": "regexp",
                        "id": generic_integer_tag.id,
                        "value": 100,
                    },
                ],
            ],
        )

        # RUN
        with pytest.raises(RuntimeError) as exception_info:
            get_user_courses(simple_user, simple_project)

        # TEST
        assert exception_info.value.message == u"Attempt to perform unsupported operation regexp"

        # CLEAR
        user_tagged_by_generic_tag.delete()
        generic_integer_tag.delete()
        assignment_rule_1.delete()

    @pytest.mark.parametrize(
        "left,right,input,result",
        [
            (0, 1, 0, pytest.lazy_fixture('simple_course')),
            (0, 1, 1, pytest.lazy_fixture('simple_course')),
            (1, 3, 2, pytest.lazy_fixture('simple_course')),
            (1, 1, 1, pytest.lazy_fixture('simple_course')),
            (-3, -1, -2, pytest.lazy_fixture('simple_course')),
            (-3, -1, 2, None),
            (1, 3, 0, None),
            (1, 1, 2, None),
            (0, 1, -1, None),
        ]
    )
    @pytest.mark.django_db
    def test_get_course_by_static_tags_13(self,
                                          simple_project,
                                          simple_course,
                                          simple_user,
                                          left, right, input, result):
        """
        Проверяем range-операцию для чисел
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        generic_integer_tag = Tag(
            value=input,
            type=TagTypeGenericInteger.get_db_type(),
            project=None
        )
        generic_integer_tag.save()

        user_tagged_by_generic_tag = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=generic_integer_tag,
        )
        user_tagged_by_generic_tag.save()
        assignment_rule_1 = AssignmentRule.objects.create(
            course=simple_course,
            mandatory=False,
            title=u"Проверяем на вхождение в диапазон дат",
            formula=[
                [
                    {
                        "semantic_type": "GenericInteger",
                        "operation": "range",
                        "id": generic_integer_tag.id,
                        "value": "{};{}".format(left, right),
                    },
                ],
            ],
        )

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            matched_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 4
        if result is None:
            result = []
        else:
            result = [result]
        assert matched_courses == result

        # CLEAR
        user_tagged_by_generic_tag.delete()
        generic_integer_tag.delete()
        assignment_rule_1.delete()

    @pytest.mark.parametrize(
        "left,right,input,result",
        [
            ('2019-01-01', '2019-01-03', '2019-01-02', pytest.lazy_fixture('simple_course')),
            ('2019-01-01', '2019-01-03', '2019-01-01', pytest.lazy_fixture('simple_course')),
            ('2019-01-01', '2019-01-03', '2019-01-03', pytest.lazy_fixture('simple_course')),
            ('2019-01-01', '2019-01-03', '2018-12-31', None),
            ('2019-01-01', '2019-01-03', '2019-01-04', None),
        ]
    )
    @pytest.mark.django_db
    def test_get_course_by_static_tags_14(self,
                                          simple_project,
                                          simple_course,
                                          simple_user,
                                          left, right, input, result):
        """
        Проверяем range-операцию для дат
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        @staticmethod
        def get_source_values_for_object_mock(object):
            return [input]

        class_obj = TagTypeStaffStartDate
        class_obj.get_source_values_for_object = get_source_values_for_object_mock

        assignment_rule_1 = AssignmentRule.objects.create(
            course=simple_course,
            mandatory=False,
            title=u"Проверяем на вхождение в диапазон дат",
            formula=[
                [
                    {
                        "semantic_type": "StaffStartDate",
                        "operation": "range",
                        "id": 0,
                        "value": "{};{}".format(left, right),
                    },
                ],
            ],
        )

        # RUN
        queries_context = CaptureQueriesContext()
        with queries_context:
            matched_courses = get_user_courses(simple_user, simple_project)

        # TEST
        assert len(queries_context) == 4
        if result is None:
            result = []
        else:
            result = [result]
        assert matched_courses == result

        # CLEAR
        assignment_rule_1.delete()

    @pytest.mark.parametrize(
        "is_mandatory,result_len",
        [
            (True, 1),
            (False, 0)
        ]
    )
    @pytest.mark.django_db
    def test_mandatory_assignment_rule_create(
            self,
            simple_project,
            simple_course,
            simple_user,
            is_mandatory,
            result_len
    ):
        """
            Проверяем, что при создании обязательного правила назначения добавляется связь с курсом,
            существующим пользователям, попадающим под созданное правило
        """
        # INIT
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        simple_course.project = simple_project
        simple_course.save()

        nur_tag = Tag(
            value=u'Новый Уренгой',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        nur_tag.save()

        user_tagged_by_city_nur = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=nur_tag,
        )
        user_tagged_by_city_nur.save()

        # RUN
        assignment_rule = AssignmentRule.objects.create(
            course=simple_course,
            mandatory=is_mandatory,
            title=u"Новоуренгойцы",
            formula=[
                [
                    {
                        "semantic_type": "City",
                        "operation": "==",
                        "id": nur_tag.id,
                        "value": u"Новый Уренгой",
                    },
                ],
            ],
        )

        # TEST
        auto_added_user_courses = CourseStudent.objects.filter(
            student=simple_user,
            course=simple_course,
            assignment_rule=assignment_rule
        )
        assert(auto_added_user_courses.count() == result_len)

        # CLEAR
        user_tagged_by_city_nur.delete()
        nur_tag.delete()

    @pytest.mark.parametrize(
        "is_mandatory,result_len",
        [
            (True, 1),
            (False, 0)
        ]
    )
    @pytest.mark.django_db
    def test_add_mandatory_course_for_new_user(
            self,
            simple_project,
            simple_course,
            simple_user,
            is_mandatory,
            result_len
    ):
        """
            Проверяем, что при создании обязательного правила назначения добавляется связь с курсом,
            существующим пользователям, попадающим под созданное правило
        """
        # INIT
        simple_course.project = simple_project
        simple_course.save()

        nur_tag = Tag(
            value=u'Новый Уренгой',
            type=TagTypeCity.get_db_type(),
            project=None
        )
        nur_tag.save()

        user_tagged_by_city_nur = TaggedObject(
            object_id=simple_user.id,
            content_type=ContentType.objects.get_for_model(User),
            tag=nur_tag,
        )
        user_tagged_by_city_nur.save()

        assignment_rule = AssignmentRule.objects.create(
            course=simple_course,
            mandatory=is_mandatory,
            title=u"Новоуренгойцы",
            formula=[
                [
                    {
                        "semantic_type": "City",
                        "operation": "==",
                        "id": nur_tag.id,
                        "value": u"Новый Уренгой",
                    },
                ],
            ],
        )

        # RUN
        user_project = UserProject(
            user=simple_user,
            project=simple_project,
        )
        user_project.save()

        # TEST
        auto_added_user_courses = CourseStudent.objects.filter(
            student=simple_user,
            course=simple_course,
            assignment_rule=assignment_rule
        )
        assert(auto_added_user_courses.count() == result_len)

        # CLEAR
        user_tagged_by_city_nur.delete()
        nur_tag.delete()
