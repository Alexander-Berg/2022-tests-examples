from builtins import object
import pytest
from django.contrib.contenttypes.models import ContentType

from kelvin.tags.models import (Tag, TaggedObject, TagTypeStaffCity,
                                TagTypeStaffOffice)


@pytest.mark.django_db
class TestLocationTagTasks(object):

    def test_tag_users_from_staff_offices_by_ids(self, mocker, student, default_project):
        Tag.objects.get_or_create(
            project=default_project,
            type=TagTypeStaffCity.get_db_type(),
            value=u'Москва',
            defaults={
                'data': {
                    'id': 1,
                    'name': {
                        'ru': u'Москва',
                    }
                }
            },
        )
        Tag.objects.get_or_create(
            project=default_project,
            type=TagTypeStaffOffice.get_db_type(),
            value=u'Морозов',
            defaults={
                'data': {
                    'id': 1,
                    'name': {
                        'ru': u'Морозов'
                    },
                    'city': {
                        'id': 1,
                        'name': {
                            'ru': u'Москва',
                        }
                    }
                }
            },
        )
        mocker.patch('kelvin.common.staff_reader.staff_reader.get_user_location').return_value = {
            'office': {
                'id': 1,
                'name': {
                    'ru': u'Морозов'
                },
                'city': {
                    'id': 1,
                    'name': {
                        'ru': u'Москва',
                    }
                }
            },
        }

        from kelvin.tags.tasks import tag_users_from_staff_location_by_ids
        tag_users_from_staff_location_by_ids([student.id], default_project.id)

        tagged_office_objects = TaggedObject.objects.filter(
            tag__type=TagTypeStaffOffice.get_db_type(),
            content_type=ContentType.objects.get(app_label='accounts', model='user'),
            object_id=student.id,
        )

        tagged_city_objects = TaggedObject.objects.filter(
            tag__type=TagTypeStaffCity.get_db_type(),
            content_type=ContentType.objects.get(app_label='accounts', model='user'),
            object_id=student.id,
        )

        assert tagged_office_objects.count() == 1
        tagged_object_office = tagged_office_objects.first()
        assert tagged_object_office.tag.value == u'Морозов'

        assert tagged_city_objects.count() == 1
        tagged_object_city = tagged_city_objects.first()
        assert tagged_object_city.tag.value == u'Москва'

    def test_update_staff_location_tags(self, mocker, default_project):
        mocker.patch('kelvin.common.staff_reader.staff_reader.get_all_offices').return_value = [
            {
                'id': 1,
                'name': {
                    'ru': u'Морозов',
                },
                'city': {
                    'id': 1,
                    'is_deleted': False,
                    'name': {
                        'ru': u'Москва',
                    }
                },
                'is_deleted': False
            },
            {
                'id': 2,
                'name': {
                    'ru': u'Образование',
                },
                'city': {
                    'id': 1,
                    'is_deleted': False,
                    'name': {
                        'ru': u'Москва',
                    }
                },
                'is_deleted': False
            },
            {
                'id': 3,
                'name': {
                    'ru': u'Палладиум',
                },
                'city': {
                    'id': 2,
                    'is_deleted': False,
                    'name': {
                        'ru': u'Екатеринбург',
                    }
                },
                'is_deleted': False
            },
            {
                'id': 4,
                'name': {
                    'ru': u'Бенуа',
                },
                'city': {
                    'id': 3,
                    'is_deleted': False,
                    'name': {
                        'ru': u'Санкт-Петербург',
                    }
                },
                'is_deleted': False
            },
            {
                'id': 5,
                'name': {
                    'ru': u'ГлавБолПикКонтора',
                },
                'city': {
                    'id': 4,
                    'is_deleted': False,
                    'name': {
                        'ru': u'Большие Пикули',
                    }
                },
                'is_deleted': False
            },
        ]

        from kelvin.tags.tasks import update_staff_location_tags
        update_staff_location_tags()

        office_tags = Tag.objects.filter(
            type=TagTypeStaffOffice.get_db_type(),
            project__slug='DEFAULT',
        ).order_by('data')

        assert office_tags.count() == 5
        assert office_tags[0].value == u'Морозов'
        assert office_tags[1].value == u'Образование'
        assert office_tags[2].value == u'Палладиум'
        assert office_tags[3].value == u'Бенуа'
        assert office_tags[4].value == u'ГлавБолПикКонтора'

        city_tags = Tag.objects.filter(
            type=TagTypeStaffCity.get_db_type(),
            project__slug='DEFAULT',
        ).order_by('data')

        assert city_tags.count() == 4
        assert city_tags[0].value == u'Москва'
        assert city_tags[1].value == u'Екатеринбург'
        assert city_tags[2].value == u'Санкт-Петербург'
        assert city_tags[3].value == u'Большие Пикули'

        mocker.patch('kelvin.common.staff_reader.staff_reader.get_all_offices').return_value = [
            {
                'id': 4,
                'name': {
                    'ru': u'Бенуа',
                },
                'city': {
                    'id': 3,
                    'is_deleted': False,
                    'name': {
                        'ru': u'Санкт-Петербург',
                    }
                },
                'is_deleted': False
            },
            {
                'id': 5,
                'name': {
                    'ru': u'ГлавБолПикКонтора',
                },
                'city': {
                    'id': 4,
                    'is_deleted': True,
                    'name': {
                        'ru': u'Большие Пикули',
                    }
                },
                'is_deleted': True
            },
        ]

        from kelvin.tags.tasks import update_staff_location_tags
        update_staff_location_tags()

        office_tags = Tag.objects.filter(
            type=TagTypeStaffOffice.get_db_type(),
            project__slug='DEFAULT',
        ).order_by('data')

        assert office_tags.count() == 4
        assert office_tags[0].value == u'Морозов'
        assert office_tags[1].value == u'Образование'
        assert office_tags[2].value == u'Палладиум'
        assert office_tags[3].value == u'Бенуа'

        city_tags = Tag.objects.filter(
            type=TagTypeStaffCity.get_db_type(),
            project__slug='DEFAULT',
        ).order_by('data')

        assert city_tags.count() == 3
        assert city_tags[0].value == u'Москва'
        assert city_tags[1].value == u'Екатеринбург'
        assert city_tags[2].value == u'Санкт-Петербург'
