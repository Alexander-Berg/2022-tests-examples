from builtins import object
import json

import pytest
from django.contrib.contenttypes.models import ContentType

from kelvin.tags.models import Tag, TaggedObject, TagTypeStaffGroup


@pytest.mark.django_db
class TestGroupTagTasks(object):

    def test_tag_users_from_staff_groups_by_ids(self, mocker, student, default_project):
        Tag.objects.get_or_create(
            project=default_project,
            type=TagTypeStaffGroup.get_db_type(),
            value=u'Яндекс',
            defaults={
                'data': {'is_deleted': False, 'type': 'department', 'id': 1, 'name': u'Яндекс'}
            },
        )
        Tag.objects.get_or_create(
            project=default_project,
            type=TagTypeStaffGroup.get_db_type(),
            value=u'Департамент HR',
            defaults={
                'data': {'is_deleted': False, 'type': 'department', 'id': 2, 'name': u'Департамент HR'},
            },
        )
        Tag.objects.get_or_create(
            project=default_project,
            type=TagTypeStaffGroup.get_db_type(),
            value=u'Отдел обучения и развития',
            defaults={
                'data': {'is_deleted': False, 'type': 'department', 'id': 3, 'name': u'Отдел обучения и развития'},
            },
        )
        Tag.objects.get_or_create(
            project=default_project,
            type=TagTypeStaffGroup.get_db_type(),
            value=u'Служба разработки Мёбиуса',
            defaults={
                'data': {'is_deleted': False, 'type': 'department', 'id': 4, 'name': u'Служба разработки Мёбиуса'},
            },
        )
        Tag.objects.get_or_create(
            project=default_project,
            type=TagTypeStaffGroup.get_db_type(),
            value=u'Группа разработки платформы дистанционного обучения',
            defaults={
                'data': {
                    'is_deleted': False,
                    'type': 'department',
                    'id': 5,
                    'name': u'Группа разработки платформы дистанционного обучения'
                },
            },
        )
        Tag.objects.get_or_create(
            project=default_project,
            type=TagTypeStaffGroup.get_db_type(),
            value=u'Неяндекс',
            defaults={
                'data': {'is_deleted': False, 'type': 'department', 'id': 10, 'name': u'Неяндекс'},
            },
        )
        Tag.objects.get_or_create(
            project=default_project,
            type=TagTypeStaffGroup.get_db_type(),
            value=u'Мальчики',
            defaults={
                'data': {'ancestors': [], 'is_deleted': False, 'name': u'Мальчики', 'type': 'wiki', 'id': 7}
            },
        )
        mocker.patch('kelvin.common.staff_reader.staff_reader.get_user_groups').return_value = [
            {
                'group': {
                    'ancestors': [
                        {'is_deleted': False, 'type': 'department', 'id': 1, 'name': u'Яндекс'},
                        {'is_deleted': False, 'type': 'department', 'id': 2, 'name': u'Департамент HR'},
                        {'is_deleted': False, 'type': 'department', 'id': 3, 'name': u'Отдел обучения и развития'},
                        {'is_deleted': False, 'type': 'department', 'id': 4, 'name': u'Служба разработки Мёбиуса'}
                    ],
                    'is_deleted': False,
                    'name': u'Группа разработки платформы дистанционного обучения',
                    'type': 'department',
                    'id': 5
                },
                'id': 5
            },
            {
                'group': {
                    'ancestors': [], 'is_deleted': False, 'name': u'Мальчики', 'type': 'wiki', 'id': 7
                },
                'id': 7
            }
        ]

        from kelvin.tags.tasks import tag_users_from_staff_groups_by_ids
        tag_users_from_staff_groups_by_ids([student.id], default_project.id)

        tagged_objects = TaggedObject.objects.filter(
            tag__type=TagTypeStaffGroup.get_db_type(),
            content_type=ContentType.objects.get(app_label='accounts', model='user'),
            object_id=student.id,
        )

        assert tagged_objects.count() == 5
        tagged_object = tagged_objects.filter(tag__data__id=1).first()
        assert tagged_object.tag.value == u'Яндекс'

        mocker.patch('kelvin.common.staff_reader.staff_reader.get_user_groups').return_value = [
            {
                'group': {
                    'ancestors': [
                        {'is_deleted': False, 'type': 'department', 'id': 10, 'name': u'Неяндекс'},
                        {'is_deleted': False, 'type': 'department', 'id': 2, 'name': u'Департамент HR'},
                        {'is_deleted': False, 'type': 'department', 'id': 3, 'name': u'Отдел обучения и развития'},
                        {'is_deleted': False, 'type': 'department', 'id': 4, 'name': u'Служба разработки Мёбиуса'}
                    ],
                    'is_deleted': False,
                    'name': u'Группа разработки платформы дистанционного обучения',
                    'type': 'department',
                    'id': 5
                },
                'id': 5
            },
            {
                'group': {
                    'ancestors': [], 'is_deleted': False, 'name': u'Мальчики', 'type': 'wiki', 'id': 7
                },
                'id': 7
            }
        ]

        from kelvin.tags.tasks import tag_users_from_staff_groups_by_ids
        tag_users_from_staff_groups_by_ids([student.id], default_project.id)

        tagged_objects = TaggedObject.objects.filter(
            tag__type=TagTypeStaffGroup.get_db_type(),
            content_type=ContentType.objects.get(app_label='accounts', model='user'),
            object_id=student.id,
        )

        assert tagged_objects.count() == 5

        tagged_object = tagged_objects.filter(tag__data__id=10).first()
        assert tagged_object.tag.value == u'Неяндекс'

        assert tagged_objects.filter(tag__data__id=1).first() is None

    def test_update_staff_group_tags(self, mocker, default_project):
        mocker.patch('kelvin.common.staff_reader.staff_reader.get_all_groups').return_value = [
            {'id': 1, 'name': u'Группа тестирования Мёбиуса', 'is_deleted': False},
            {'id': 2, 'name': u'Группа тестирования Немёбиуса', 'is_deleted': False}
        ]

        from kelvin.tags.tasks import update_staff_group_tags
        update_staff_group_tags()

        tags = Tag.objects.filter(
            type=TagTypeStaffGroup.get_db_type(),
            project__slug='DEFAULT',
        ).order_by('data')

        assert tags.count() == 2
        assert tags[0].value == u'Группа тестирования Мёбиуса'
        assert tags[1].value == u'Группа тестирования Немёбиуса'

        mocker.patch('kelvin.common.staff_reader.staff_reader.get_all_groups').return_value = [
            {'id': 1, 'name': u'Группа тестирования Мёбиуса', 'parent': {'name': u'Управление Y'}, 'is_deleted': True},
            {'id': 2, 'name': u'Группа тестирования Платона', 'parent': {'name': u'Управление Y'}, 'is_deleted': False}
        ]

        from kelvin.tags.tasks import update_staff_group_tags
        update_staff_group_tags()

        tags = Tag.objects.filter(
            type=TagTypeStaffGroup.get_db_type(),
            project__slug='DEFAULT',
        ).order_by('data')

        assert tags.count() == 1
        assert tags[0].value == u'Группа тестирования Платона'

        mocker.patch('kelvin.common.staff_reader.staff_reader.get_all_groups').return_value = [
            {'id': 1, 'name': u'Группа тестирования Мёбиуса', 'parent': {'name': u'Управление Y'}, 'is_deleted': True},
            {'id': 2, 'name': u'Группа тестирования Кюри', 'parent': {'name': u'Управление Y'}, 'is_deleted': False}
        ]

        from kelvin.tags.tasks import update_staff_group_tags
        update_staff_group_tags()

        tags = Tag.objects.filter(
            type=TagTypeStaffGroup.get_db_type(),
            project__slug='DEFAULT',
        ).order_by('data')

        assert tags.count() == 1
        assert tags[0].value == u'Группа тестирования Кюри'

        mocker.patch('kelvin.common.staff_reader.staff_reader.get_all_groups').return_value = [
            {'id': 1, 'name': u'Группа тестирования Мёбиуса', 'parent': {'name': u'Управление Y'}, 'is_deleted': True},
            {'id': 2, 'name': u'Группа тестирования Кюри', 'parent': {'name': u'Управление Y'}, 'is_deleted': False},
            {'id': 3, 'name': u'Группа тестирования Кюри', 'parent': {'name': u'Управление Я'}, 'is_deleted': False}
        ]

        from kelvin.tags.tasks import update_staff_group_tags
        update_staff_group_tags()

        tags = Tag.objects.filter(
            type=TagTypeStaffGroup.get_db_type(),
            project__slug='DEFAULT',
        ).order_by('data')

        assert tags.count() == 2
        assert tags[0].value == u'Группа тестирования Кюри (Управление Y)'
        assert tags[1].value == u'Группа тестирования Кюри (Управление Я)'

        mocker.patch('kelvin.common.staff_reader.staff_reader.get_all_groups').return_value = [
            {'id': 1, 'name': u'Группа тестирования Мёбиуса', 'parent': {'name': u'Управление Y'}, 'is_deleted': True},
            {'id': 2, 'name': u'Группа тестирования Кюри', 'parent': {'name': u'Управление Y'}, 'is_deleted': False},
            {'id': 3, 'name': u'Группа тестирования Кюри', 'parent': {'name': u'Управление Я'}, 'is_deleted': False},
            {'id': 4, 'name': u'Группа тестирования Вольта', 'parent': {'name': u'Управление Y'}, 'is_deleted': False},
            {'id': 5, 'name': u'Группа тестирования Вольта', 'parent': {'name': u'Управление Y'}, 'is_deleted': False},
        ]

        from kelvin.tags.tasks import update_staff_group_tags
        update_staff_group_tags()

        tags = Tag.objects.filter(
            type=TagTypeStaffGroup.get_db_type(),
            project__slug='DEFAULT',
        ).order_by('data')

        assert tags.count() == 3

    def test_force_staff_tags_renewal(self, mocker, default_project):
        mocker.patch('kelvin.common.staff_reader.staff_reader.get_all_groups').return_value = [
            {'id': 1, 'name': u'Группа тестирования Мёбиуса', 'is_deleted': False},
            {'id': 2, 'name': u'Группа тестирования Немёбиуса', 'is_deleted': False}
        ]

        from kelvin.tags.tasks import force_renew_staff_group_tags
        force_renew_staff_group_tags()

        tags = Tag.objects.filter(
            type=TagTypeStaffGroup.get_db_type(),
            project__slug='DEFAULT',
        ).order_by('data')

        assert tags.count() == 2
        assert tags[0].value == u'Группа тестирования Мёбиуса'
        assert tags[1].value == u'Группа тестирования Немёбиуса'

        mocker.patch('kelvin.common.staff_reader.staff_reader.get_all_groups').return_value = [
            {'id': 1, 'name': u'Группа тестирования Мёбиуса', 'parent': {'name': u'Управление Y'}, 'is_deleted': False,
             'level': 1},
            {'id': 2, 'name': u'Группа тестирования Платона', 'parent': {'name': u'Управление Y'}, 'is_deleted': False,
             'level': 2}
        ]

        force_renew_staff_group_tags()

        tags = Tag.objects.filter(
            type=TagTypeStaffGroup.get_db_type(),
            project__slug='DEFAULT',
            data__level__gt=0
        ).order_by('data')

        assert tags.count() == 2
