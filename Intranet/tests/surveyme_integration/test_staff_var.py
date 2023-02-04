# -*- coding: utf-8 -*-
import responses

from django.test import TestCase
from django.utils.translation import override

from events.surveyme.factories import (
    ProfileSurveyAnswerFactory,
    SurveyQuestionFactory,
    SurveyQuestionChoiceFactory,
)
from events.accounts.factories import UserFactory
from events.surveyme.models import AnswerType
from events.surveyme_integration.variables.staff import (
    StaffBirthDateVariableRenderer,
    StaffCarsVariableRenderer,
    StaffChildrenVariableRenderer,
    StaffCityVariableRenderer,
    StaffContactsVariableRenderer,
    StaffDepartmentVariableRenderer,
    StaffDismissedVariableRenderer,
    StaffDivisionVariableRenderer,
    StaffEmploymentVariableRenderer,
    StaffFamilyStatusVariableRenderer,
    StaffFirstNameVariableRenderer,
    StaffLastNameVariableRenderer,
    StaffMiddleNameVariableRenderer,
    StaffHeadVariableRenderer,
    StaffHrPartnerVariableRenderer,
    StaffJoinAtVariableRenderer,
    StaffLoginVariableRenderer,
    StaffOfficeVariableRenderer,
    StaffQuitAtVariableRenderer,
    StaffTableNumberVariableRenderer,
    StaffTshirtSizeVariableRenderer,
    StaffEducationVariableRenderer,

    StaffMetaUserVariable,
    StaffMetaQuestionVariable,
    StaffExternalLoginVariable,
    StaffExternalLoginFromRequestVariable,
)
from events.surveyme_integration.variables.staff import BulkVariableRequester


class StaffVariableRendererTestBase:
    fixtures = ['initial_data.json']
    uid = '1120000000016772'
    cloud_uid = None
    username = 'Smosker'
    lang = 'ru'
    format_name = None
    cassette = None

    def setUp(self):
        user = UserFactory(uid=self.uid, cloud_uid=self.cloud_uid, username=self.username)
        self.answer = ProfileSurveyAnswerFactory(user=user)
        self.check = True

    def get_variable_init_data(self):
        return {'answer': self.answer}

    @responses.activate
    def test_get_data_from_staff(self):
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons',
            json=self.cassette,
        )
        with override(self.lang):
            var = self.variable(**self.get_variable_init_data())
            result = var.get_value(format_name=self.format_name)
            if self.check:
                self.assertEqual(result, self.expected_output)
            return result


class TestStaffBirthDate(StaffVariableRendererTestBase, TestCase):
    expected_output = '1992-08-11'
    variable = StaffBirthDateVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'personal': {'birthday': '1992-08-11'},
        }],
    }


class TestStaffLogin(StaffVariableRendererTestBase, TestCase):
    expected_output = 'smosker'
    variable = StaffLoginVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{'login': 'smosker'}],
    }


class TestStaffCars(StaffVariableRendererTestBase, TestCase):
    expected_output = {'plate: М 718 ОВ 77', 'model: Renault Duster', 'plate: Х 710 АЕ 777', 'model: Kia Ceed'}
    variable = StaffCarsVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'cars': [
                {'plate': 'М 718 ОВ 77', 'model': 'Renault Duster', 'id': 2811},
                {'plate': 'Х 710 АЕ 777', 'model': 'Kia Ceed', 'id': 12700},
            ],
        }],
    }

    def setUp(self):
        super().setUp()
        self.check = False

    def test_get_data_from_staff(self):
        result = super().test_get_data_from_staff()
        parsed_result = set(result.split('\n'))
        self.assertEqual(parsed_result, self.expected_output)


class TestStaffTshirtSize(StaffVariableRendererTestBase, TestCase):
    expected_output = 'L'
    variable = StaffTshirtSizeVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'personal': {'tshirt_size': 'L'},
        }],
    }


class TestStaffTableNumber(StaffVariableRendererTestBase, TestCase):
    expected_output = 27002
    variable = StaffTableNumberVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'location': {'table': {'number': 27002}},
        }],
    }


class TestStaffQuitAt(StaffVariableRendererTestBase, TestCase):
    uid = '1120000000026301'
    username = 'ludkr'
    expected_output = '2017-05-16'
    variable = StaffQuitAtVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'ludkr',
            'official': {'quit_at': '2017-05-16'},
        }],
    }


class TestStaffOffice(StaffVariableRendererTestBase, TestCase):
    expected_output = 'Москва, БЦ Морозов'
    variable = StaffOfficeVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'location': {'office': {'name': {'ru': 'Москва, БЦ Морозов', 'en': 'Moscow, BC Morozov'}}},
        }],
    }


class TestStaffJoinAt(StaffVariableRendererTestBase, TestCase):
    expected_output = '2013-09-05'
    variable = StaffJoinAtVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'official': {'join_at': '2013-09-05'},
        }],
    }


class TestStaffHrPartner(StaffVariableRendererTestBase, TestCase):
    expected_output = 'ko-di'
    variable = StaffHrPartnerVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'department_group': {
                'ancestors': [{
                    'department': {
                        'heads': [{
                            'role': 'hr_partner',
                            'person': {
                                'is_deleted': False,
                                'login': 'ko-di',
                            },
                        }],
                    },
                }],
                'department': {
                    'heads': [{
                        'role': 'chief',
                        'person': {
                            'is_deleted': False,
                            'login': 'arikon',
                        },
                    }],
                },
            },
        }],
    }


class TestStaffHead(StaffVariableRendererTestBase, TestCase):
    expected_output = 'arikon'
    variable = StaffHeadVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'department_group': {
                'ancestors': [{
                    'department': {
                        'heads': [{
                            'role': 'chief',
                            'person': {
                                'is_deleted': False,
                                'login': 'lukyanenkova',
                            },
                        }],
                    },
                }],
                'department': {
                    'heads': [{
                        'role': 'chief',
                        'person': {
                            'is_deleted': False,
                            'login': 'arikon',
                        },
                    }],
                },
            },
        }],
    }


class StaffHeadTest_Ashki(StaffVariableRendererTestBase, TestCase):
    uid = '1120000000072846'
    username = 'ashki'
    expected_output = 'lukyanenkova'
    variable = StaffHeadVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'ashki',
            'department_group': {
                'ancestors': [{
                    'department': {
                        'heads': [{
                            'role': 'chief',
                            'person': {
                                'is_deleted': False,
                                'login': 'lukyanenkova',
                            },
                        }],
                    },
                }],
                'department': {
                    'heads': [{
                        'role': 'chief',
                        'person': {
                            'is_deleted': False,
                            'login': 'ashki',
                        },
                    }],
                },
            },
        }],
    }


class StaffHeadTest_Bunina(StaffVariableRendererTestBase, TestCase):
    uid = '1120000000001062'
    username = 'bunina'
    expected_output = 'tigran'
    variable = StaffHeadVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'bunina',
            'department_group': {
                'ancestors': [],
                'department': {
                    'heads': [{
                        'role': 'chief',
                        'person': {
                            'is_deleted': False,
                            'login': 'tigran',
                        },
                    }, {
                        'role': 'general_director',
                        'person': {
                            'is_deleted': False,
                            'login': 'volozh',
                        },
                    }],
                },
            },
        }],
    }


class StaffHeadTest_Tigran(StaffVariableRendererTestBase, TestCase):
    uid = '1120000000000251'
    username = 'tigran'
    expected_output = 'volozh'
    variable = StaffHeadVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'tigran',
            'department_group': {
                'ancestors': [],
                'department': {
                    'heads': [{
                        'role': 'chief',
                        'person': {
                            'is_deleted': False,
                            'login': 'tigran',
                        },
                    }, {
                        'role': 'general_director',
                        'person': {
                            'is_deleted': False,
                            'login': 'volozh',
                        },
                    }],
                },
            },
        }],
    }


class StaffHeadTest_Volozh(StaffVariableRendererTestBase, TestCase):
    uid = '1120000000000529'
    username = 'volozh'
    expected_output = ''
    variable = StaffHeadVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'volozh',
            'department_group': {
                'ancestors': [],
                'department': {
                    'heads': [{
                        'role': 'chief',
                        'person': {
                            'is_deleted': False,
                            'login': 'tigran',
                        },
                    }, {
                        'role': 'general_director',
                        'person': {
                            'is_deleted': False,
                            'login': 'volozh',
                        },
                    }],
                },
            },
        }],
    }


class TestStaffFirstName(StaffVariableRendererTestBase, TestCase):
    expected_output = 'Владимир'
    variable = StaffFirstNameVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'name': {'first': {'ru': 'Владимир', 'en': 'Vladimir'}},
        }],
    }


class TestStaffFirstNameEng(StaffVariableRendererTestBase, TestCase):
    expected_output = 'Vladimir'
    lang = 'en'
    variable = StaffFirstNameVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'name': {'first': {'ru': 'Владимир', 'en': 'Vladimir'}},
        }],
    }


class TestStaffMiddle(StaffVariableRendererTestBase, TestCase):
    expected_output = ''
    variable = StaffMiddleNameVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'name': {'middle': ''},
        }],
    }


class TestStaffLastName(StaffVariableRendererTestBase, TestCase):
    expected_output = 'Колясинский'
    variable = StaffLastNameVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'name': {'last': {'ru': 'Колясинский', 'en': 'Koljasinskij'}},
        }],
    }


class TestStaffFamilyStatus(StaffVariableRendererTestBase, TestCase):
    expected_output = 'married'
    variable = StaffFamilyStatusVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'personal': {'family_status': 'married'},
        }],
    }


class TestStaffEmployment(StaffVariableRendererTestBase, TestCase):
    expected_output = 'full'
    variable = StaffEmploymentVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'official': {'employment': 'full'},
        }],
    }


class TestStaffDivision(StaffVariableRendererTestBase, TestCase):
    expected_output = 'Служба python-разработки сервисов для организаций'
    variable = StaffDivisionVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'department_group': {'parent': {'name': 'Служба python-разработки сервисов для организаций'}},
        }],
    }


class TestStaffContacts(StaffVariableRendererTestBase, TestCase):
    expected_output = 'email: smosker@gmail.com\nskype: live:91199e608002ffeb'
    variable = StaffContactsVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'contacts': [
                {'type': 'email', 'value': 'smosker@gmail.com'},
                {'type': 'skype', 'value': 'live:91199e608002ffeb'}
            ],
        }],
    }


class TestStaffDepartment(StaffVariableRendererTestBase, TestCase):
    expected_output = 'Группа составления программ для ЭВМ'
    variable = StaffDepartmentVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'department_group': {'name': 'Группа составления программ для ЭВМ'},
        }],
    }


class TestStaffDismissed(StaffVariableRendererTestBase, TestCase):
    expected_output = False
    variable = StaffDismissedVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'official': {'is_dismissed': False},
        }],
    }


class TestStaffChildren(StaffVariableRendererTestBase, TestCase):
    expected_output = 2
    variable = StaffChildrenVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'personal': {'children': 2},
        }],
    }


class TestStaffCity(StaffVariableRendererTestBase, TestCase):
    expected_output = 'Москва'
    variable = StaffCityVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'location': {'office': {'city': {'name': {'ru': 'Москва', 'en': 'Moscow'}}}},
        }],
    }


class TestStaffEducation(StaffVariableRendererTestBase, TestCase):
    expected_output = 'Национальный исследовательский университет "Высшая школа экономики"'
    variable = StaffEducationVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'education': {'place': {'ru': 'Национальный исследовательский университет "Высшая школа экономики"', 'en': ''}},
        }],
    }


class TestStaffEducationFallback(StaffVariableRendererTestBase, TestCase):
    lang = 'en'
    expected_output = 'Национальный исследовательский университет "Высшая школа экономики"'
    variable = StaffEducationVariableRenderer
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'education': {'place': {'ru': 'Национальный исследовательский университет "Высшая школа экономики"', 'en': ''}},
        }],
    }


class TestStaffMetaUserDefaultFormat(StaffVariableRendererTestBase, TestCase):
    expected_output = 'Владимир'
    variable = StaffMetaUserVariable
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'name': {'first': {'ru': 'Владимир', 'en': 'Vladimir'}},
        }],
    }


class TestStaffMetaUserWithFormat(StaffVariableRendererTestBase, TestCase):
    expected_output = 'Москва'
    format_name = StaffCityVariableRenderer.format_name
    variable = StaffMetaUserVariable
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'location': {'office': {'city': {'name': {'ru': 'Москва', 'en': 'Moscow'}}}},
        }],
    }


class TestStaffMetaQuestionWithFormat(StaffVariableRendererTestBase, TestCase):
    expected_output = 'Группа составления программ для ЭВМ'
    format_name = StaffDepartmentVariableRenderer.format_name
    variable = StaffMetaQuestionVariable
    cassette = {
        'links': {}, 'page': 1, 'limit': 1, 'total': 1, 'pages': 1,
        'result': [{
            'login': 'smosker',
            'department_group': {'name': 'Группа составления программ для ЭВМ'},
        }],
    }
    fixtures = ['initial_data.json']

    def get_variable_init_data(self):
        survey = self.answer.survey
        question = SurveyQuestionFactory(
            survey=survey,
            answer_type=AnswerType.objects.get(slug='answer_choices')
        )
        choice = SurveyQuestionChoiceFactory(
            label='Колясинскй Владимир (smosker)',
            survey_question=question,
        )
        self.answer.data = {
            'data': [{
                'question': question.get_answer_info(),
                'value': [{
                    'key': str(choice.pk),
                    'slug': choice.slug,
                    'text': choice.label,
                }],
            }],
        }
        self.answer.save()

        data = super().get_variable_init_data()

        data['question'] = question.id
        return data


class TestStaffFormatFields(TestCase):
    def test_format_field(self):
        fields_map = {
            ('office.city', 'office.location'): 'office.city,office.location',
            ('office.city.name', 'office.city.code', 'office.location'):
                'office.city.name,office.city.code,office.location',
            ('office.city', 'office.city.name', 'office.city.code'): 'office.city',
            ('office.city.name', 'office.city.code', 'office.city.smth'): ('office.city.name,office.city.code,'
                                                                           'office.city.smth'),
            ('office.city.name', 'office.city', 'department_group'): 'office.city,department_group',
            ('office.city.name', 'office.city', 'department_group.keys', 'department_group.parents'):
                'office.city,department_group.keys,department_group.parents',
            ('department_group.name', 'official.employment', 'name.last', 'name.middle', 'department_group',
             'contacts', 'cars', 'personal.tshirt_size', 'department_group.parent.name', 'location.office.name',
             'official.join_at', 'personal.birthday', 'personal.family_status', 'personal.children',
             'location.table.number', 'login', 'name.first', 'official.quit_at', 'location.office.city.name',
             'official.is_dismissed'): ('official.employment,name.last,name.middle,department_group,contacts,cars,'
                                        'personal.tshirt_size,location.office.name,official.join_at,personal.'
                                        'birthday,personal.family_status,personal.children,location.table.number,'
                                        'login,name.first,official.quit_at,location.office.city.name,'
                                        'official.is_dismissed')
        }
        field_formatter = BulkVariableRequester(None, None).format_fields
        for fields, formatted_fields in fields_map.items():
            self.assertEqual(field_formatter(fields), formatted_fields)


class TestStaffExternalLoginVariable(TestCase):
    fixtures = ['initial_data.json']
    variable = StaffExternalLoginVariable

    def setUp(self):
        user = UserFactory(uid='1120000000016799', username='smoskerYan', email='smoskerYan@yandex.ru')
        self.answer = ProfileSurveyAnswerFactory(user=user)
        self.question = SurveyQuestionFactory(
            survey=self.answer.survey,
            answer_type=AnswerType.objects.get(slug='answer_short_text')
        )

    @responses.activate
    def check_result(self, cassette=None, expected=None, pass_question=True, status=200):
        responses.add(
            responses.GET,
            'https://staff-api.test.yandex-team.ru/v3/persons',
            json=cassette or {},
            status=status,
        )
        kwargs = {'answer': self.answer}
        if pass_question:
            kwargs['question'] = self.question.id

        var = self.variable(**kwargs)
        self.assertEqual(var.get_value(), expected)

    def test_return_internal_login_success(self):
        self.answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'smoskerYan@yandex.ru',
            }],
        }
        self.answer.save()
        cassette = {'login': 'smosker'}
        self.check_result(cassette=cassette, expected='smosker')

    def test_return_internal_login__from_request_success(self):
        self.variable = StaffExternalLoginFromRequestVariable
        cassette = {'login': 'smosker'}
        self.check_result(cassette=cassette, expected='smosker', pass_question=False)

    def test_return_internal_login__from_request_success_upper_cased_login(self):
        self.variable = StaffExternalLoginFromRequestVariable
        self.answer.user.email = 'Smoskeryan@yandex.ru'
        cassette = {'login': 'smosker'}
        self.check_result(cassette=cassette, expected='smosker', pass_question=False)

    def test_return_internal_login_success_short(self):
        self.answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'smoskerYan@yandex.ru',
            }],
        }
        self.answer.save()
        cassette = {'login': 'smosker'}
        self.check_result(cassette=cassette, expected='smosker')

    def test_return_internal_login_fail_not_found(self):
        self.answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'smoskerYan12312@yandex.ru',
            }],
        }
        self.answer.save()
        self.check_result(status=404)

    def test_return_internal_login_from_request_fail_not_found(self):
        self.answer.user = UserFactory(uid='1120000000026301')
        self.answer.save()

        self.variable = StaffExternalLoginFromRequestVariable
        self.check_result(pass_question=False, status=404)

    def test_return_internal_login_fail_error(self):
        self.answer.data = {
            'data': [{
                'question': self.question.get_answer_info(),
                'value': 'smthtest@yandex.ru',
            }],
        }
        self.answer.save()
        self.check_result(status=404)

    def test_return_internal_login_from_request_fail_error(self):
        self.answer.user = UserFactory(uid='1120000000026301')
        self.answer.save()

        self.variable = StaffExternalLoginFromRequestVariable
        self.check_result(pass_question=False, status=404)

    def test_external_login(self):
        var = self.variable(answer=self.answer, question=self.question.pk)

        self.assertEqual(var._get_external_login('user@yandex.ru'), 'user')
        self.assertEqual(var._get_external_login('user@yandex.be'), 'user')
        self.assertEqual(var._get_external_login('user@yandex.ua'), 'user')
        self.assertEqual(var._get_external_login('user@yandex.com'), 'user')
        self.assertEqual(var._get_external_login('user@yandex.com.tr'), 'user')

        self.assertIsNone(var._get_external_login('user@gmail.com'))
        self.assertIsNone(var._get_external_login('user@hotmail.com'))
        self.assertIsNone(var._get_external_login('not an email'))
        self.assertIsNone(var._get_external_login(''))
