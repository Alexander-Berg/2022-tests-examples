from builtins import object
import pytest
import re
from django.conf import settings

from itertools import chain

from kelvin.courses.journal import CourseGroupJournal, LessonJournal
from kelvin.tags.models import TaggedObject, Tag, TagTypeStaffGroup
from django.contrib.contenttypes.models import ContentType
from kelvin.accounts.models import User, UserStaffGroups


@pytest.mark.django_db
class TestJournal(object):
    def test_lesson_journal_table(self, statistic):
        """
        Проверяет правильность формирования таблицы по журналу занятия
        """
        # INIT
        tags = Tag.objects.bulk_create([
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                value=u'Яндекс',
                data={'level': 1}
            ),
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                value=u'Служба разработки Мёбиуса',
                data={'level': 2}
            ),
            Tag(
                type=TagTypeStaffGroup.get_db_type(),
                value=u'Группа разработки платформы дистанционного обучения',
                data={'level': 3}
            ),
        ])

        UserStaffGroups.objects.all().delete()

        def tag_user_by_staff_groups(user_id):
            user_content_type = ContentType.objects.get_for_model(User)
            tagged_objects = TaggedObject.objects.bulk_create([
                TaggedObject(
                    object_id=user_id,
                    content_type=user_content_type,
                    tag=tags[2]
                ),
                TaggedObject(
                    object_id=user_id,
                    content_type=user_content_type,
                    tag=tags[0]
                ),
                TaggedObject(
                    object_id=user_id,
                    content_type=user_content_type,
                    tag=tags[1]
                ),
            ])

        course, clesson, students, true_result = statistic

        for user in set(students):
            # для всех пользователей из отчета добавляем теги групп, чтобы проверить
            tag_user_by_staff_groups(user.id)

        table = LessonJournal(clesson).table()[1:]

        for user_num, user in enumerate(students):

            assert len(table[user_num]) == 12, (
                u'Длина строки в журнале не равна 12')
            assert table[user_num][:5] == [
                user.id,
                user.last_name,
                user.first_name,
                user.username,
                user.is_dismissed,
            ], u'Не совпадает информация про учеников'
            assert (
                table[user_num][5:9] ==
                list(chain.from_iterable(true_result[user_num]))
            ), u'Не совпадает информация по попыткам ученика'
            assert (
                re.match(r'^\d\d\d\d-\d\d-\d\d$', table[user_num][9])
            ), u'Дата начала занятия не соответствует формату'
            assert (
                re.match(r'^\d\d\d\d-\d\d-\d\d$', table[user_num][10])
            ), u'Дата последнего прохождения занятия не соответствует формату'
            assert (
                table[user_num][11] == u'Яндекс/Служба разработки Мёбиуса/Группа разработки платформы дистанционного обучения'
            ), u'Стафф-группы в неверном формате'

    def test_course_journal_table(self, course_student_stats):
        """
        Проверяет правильность формирования таблицы по журналу курса
        """
        UserStaffGroups.objects.all().delete()

        course, clessons, students, stats = course_student_stats

        header = [
            u'id пользователя',
            u'фамилия',
            u'имя',
            u'логин',
            u'бывший сотрудник',
            u'1 : {}. Баллы'.format(clessons[0].lesson.name),
            u'1 : {}. Прогресс'.format(clessons[0].lesson.name),
            u'2 : {}. Баллы'.format(clessons[1].lesson.name),
            u'2 : {}. Прогресс'.format(clessons[1].lesson.name),
            u'1 : результат по модулю',
            u'2 : результат по модулю',
            u'Группа на стаффе'
        ]

        expected_table = [
            header,
        ]

        for stat in stats:
            user = stat.student
            clesson_data = stat.clesson_data
            expected_table.append([
                user.id,
                user.last_name,
                user.first_name,
                user.username,
                user.is_dismissed,
                '{} / {}'.format(clesson_data[clessons[0].id]['points'], clesson_data[clessons[0].id]['max_points']),
                clesson_data[clessons[0].id]['progress'],
                '{} / {}'.format(clesson_data[clessons[1].id]['points'], clesson_data[clessons[1].id]['max_points']),
                clesson_data[clessons[1].id]['progress'],
                '{}laboratory/course/{}/assignments/{}/students/{}'.format(
                    settings.FRONTEND_HOST, course.id, clessons[0].id, user.id),
                '{}laboratory/course/{}/assignments/{}/students/{}'.format(
                    settings.FRONTEND_HOST, course.id, clessons[1].id, user.id),
                ''
            ])

        table = CourseGroupJournal(course).table()
        assert table == expected_table, (
            u'Таблица должны быть оформлена в соответствии с форматом')
