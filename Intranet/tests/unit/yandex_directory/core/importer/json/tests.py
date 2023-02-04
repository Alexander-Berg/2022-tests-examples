# -*- coding: utf-8 -*-

from hamcrest import (
    assert_that,
    equal_to,
)
from unittest.mock import patch

from testutils import TestCase

from intranet.yandex_directory.src.yandex_directory.core.importer.from_json import (
    JsonImporter
)


class TestDepartmentValidator(TestCase):
    def setUp(self):
        super(TestDepartmentValidator, self).setUp()
        self.valid_json = {
            "departments": [
                {
                    "id": 1,
                    "name": "Институт 1"
                },
                {
                    "id": 2,
                    "name": "Группа 232",
                    "parent": 1,
                    "head_id": 100500
                }
            ],
            "users": [
                {

                    "id": "100500",
                    "name": {
                        "first": "Василий",
                        "last": "Пупкин",
                        "middle": "Иванович"
                    },
                    "nickname": "vasyliy",
                    "department": 2,
                    "position": "староста",
                    "about": "Обращаться по любым вопросам",
                    "gender": "male",
                    "birthday": "1979-11-03",
                    "contacts": [
                        {

                            "type": "email",
                            "value": "vasya@mail.ru"
                        },
                        {
                            "main": True,
                            "type": "phone",
                            "value": "+7 (123) 456-78-90"
                        }
                    ]
                },
                {
                    "id": "100501",
                    "name": {
                        "first": "Инна",
                        "last": "Гулькина"
                    },
                    "nickname": "inna",
                    "department": 2,
                    "position": "студент",
                    "gender": "female",
                    "birthday": "1990-05-16"
                },
                {
                    "id": "100502",
                    "name": {
                        "first": "Пётр",
                        "last": "Дудиков"
                    },
                    "department": 2
                }
            ]
        }

    def test_valid(self):
        # успешно проводим прореку по json схеме
        importer = JsonImporter(
            self.meta_connection,
            self.main_connection,
            self.organization['id'],
        )
        importer.load(self.valid_json)
        
        assert_that(
            importer.users,
            equal_to(self.valid_json['users'])
        )
        assert_that(
            importer.departments,
            equal_to(self.valid_json['departments'])
        )
