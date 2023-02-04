from faker import Faker

from django.core.exceptions import ValidationError
from django.db.models import Max
from django.test import TestCase

from lms.courses.models import CourseCategory
from lms.courses.tests.factories import CourseCategoryFactory

from ..models import EnrollSurveyField
from ..serializers import EnrollSurveyDataSerializer
from .factories import EnrollSurveyFactory, EnrollSurveyFieldFactory

fake = Faker()


class EnrollSurveyDataSerializerTestCase(TestCase):
    def setUp(self):
        self.survey = EnrollSurveyFactory()

    def assert_invalid(self, invalid_data, num_queries=1):
        with self.assertNumQueries(num_queries):
            serializer = EnrollSurveyDataSerializer(data=invalid_data, context={'survey': self.survey})
            validation_result = serializer.is_valid(raise_exception=False)
            self.assertFalse(
                validation_result,
                msg=f'invalid_data={invalid_data}, errors={serializer.errors}',
            )

    def assert_valid(self, valid_data, num_queries=1):
        with self.assertNumQueries(num_queries):
            serializer = EnrollSurveyDataSerializer(data=valid_data, context={'survey': self.survey})
            validation_result = serializer.is_valid(raise_exception=False)
            self.assertTrue(
                validation_result,
                msg=f'valid_data={valid_data}, errors={serializer.errors}',
            )

    def test_invalid_max_length(self):
        for field_type in [
            EnrollSurveyField.TYPE_TEXT,
            EnrollSurveyField.TYPE_TEXTAREA,
            EnrollSurveyField.TYPE_EMAIL,
            EnrollSurveyField.TYPE_URL,
        ]:
            max_length = fake.pyint(min_value=20, max_value=255)
            field = EnrollSurveyFieldFactory(
                survey=self.survey, field_type=field_type,
                parameters={'max_length': max_length},
            )

            payload = {
                field.name: fake.pystr(min_chars=max_length + 1, max_chars=max_length + 100),
            }

            self.assert_invalid(payload)

    def test_invalid_email(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_EMAIL,
            parameters={'max_length': 255},
        )

        payload = {
            field.name: fake.pystr(min_chars=100, max_chars=200),
        }

        self.assert_invalid(payload)

    def test_invalid_url(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_URL,
            parameters={'max_length': 255},
        )

        payload = {
            field.name: fake.pystr(min_chars=100, max_chars=200),
        }

        self.assert_invalid(payload)

    def test_invalid_number(self):
        valid_min = 100
        valid_max = 200
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_NUMBER,
            parameters={
                'min_value': fake.pyint(max_value=valid_min),
                'max_value': fake.pyint(min_value=valid_max),
            }
        )

        payload = {
            field.name: fake.pystr(),
        }

        self.assert_invalid(payload)

    def test_number_too_small(self):
        valid_min = 100
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_NUMBER,
            parameters={'min_value': valid_min},
        )

        payload = {
            field.name: fake.pyint(max_value=valid_min - 1),
        }

        self.assert_invalid(payload)

    def test_number_too_big(self):
        valid_max = 200
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_NUMBER,
            parameters={'max_value': valid_max}
        )

        payload = {
            field.name: fake.pyint(min_value=valid_max + 1),
        }

        self.assert_invalid(payload)

    def test_select_not_in_options(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_SELECT,
            parameters={
                'options': [
                    {
                        'value': fake.pystr(min_chars=10, max_chars=20),
                        'content': fake.pystr(min_chars=10, max_chars=20),
                    } for _ in range(fake.pyint(min_value=2, max_value=10))
                ],
            },
        )

        payload = {
            field.name: fake.pystr(min_chars=10, max_chars=20)
        }

        self.assert_invalid(payload)

    def test_multicheckbox_not_in_options(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_MULTICHECKBOX,
            parameters={
                'options': [
                    [
                        {
                            'value': fake.pystr(min_chars=10, max_chars=20),
                            'content': fake.pystr(min_chars=10, max_chars=20),
                        } for _ in range(fake.pyint(min_value=1, max_value=10))
                    ] for _ in range(fake.pyint(min_value=2, max_value=10))
                ],
            },
        )

        payload = {
            field.name: fake.pystr(min_chars=10, max_chars=20)
        }

        self.assert_invalid(payload)

    def test_multicheckbox_duplicates(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_MULTICHECKBOX,
            parameters={
                'options': [
                    [
                        {
                            'value': fake.pystr(min_chars=10, max_chars=20),
                            'content': fake.pystr(min_chars=10, max_chars=20),
                        } for _ in range(fake.pyint(min_value=1, max_value=10))
                    ] for _ in range(fake.pyint(min_value=2, max_value=10))
                ],
            },
        )

        payload = {
            field.name: [list(field.options.keys())[0] for _ in range(3)]
        }

        self.assert_invalid(payload)

    def test_invalid_multicheckbox_required_null(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_MULTICHECKBOX, is_required=True,
            parameters={
                'options': [
                    [
                        {
                            'value': fake.pystr(min_chars=10, max_chars=20),
                            'content': fake.pystr(min_chars=10, max_chars=20),
                        } for _ in range(fake.pyint(min_value=1, max_value=10))
                    ] for _ in range(fake.pyint(min_value=2, max_value=10))
                ],
            },
        )

        payload = {
            field.name: None,
        }

        self.assert_invalid(payload)

    def test_valid_multicheckbox_required_empty(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_MULTICHECKBOX, is_required=True,
            parameters={
                'options': [
                    [
                        {
                            'value': fake.pystr(min_chars=10, max_chars=20),
                            'content': fake.pystr(min_chars=10, max_chars=20),
                        } for _ in range(fake.pyint(min_value=1, max_value=10))
                    ] for _ in range(fake.pyint(min_value=2, max_value=10))
                ],
            },
        )

        payload = {
            field.name: [],
        }

        self.assert_valid(payload)

    def test_valid_max_length(self):
        for field_type in [
            EnrollSurveyField.TYPE_TEXT,
            EnrollSurveyField.TYPE_TEXTAREA,
        ]:
            max_length = fake.pyint(min_value=20, max_value=255)
            field = EnrollSurveyFieldFactory(
                survey=self.survey, field_type=field_type,
                parameters={'max_length': max_length},
            )

            payload = {
                field.name: fake.pystr(max_chars=max_length),
            }

            self.assert_valid(payload)

    def test_valid_email(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_EMAIL,
            parameters={'max_length': 255},
        )

        payload = {
            field.name: fake.email(),
        }

        self.assert_valid(payload)

    def test_valid_url(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_URL,
            parameters={'max_length': 255},
        )

        payload = {
            field.name: fake.url(),
        }

        self.assert_valid(payload)

    def test_valid_number_with_min_max(self):
        valid_min = 100
        valid_max = 200
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_NUMBER,
            parameters={
                'min_value': valid_min,
                'max_value': valid_max,
            },
        )

        payload = {
            field.name: fake.pyint(min_value=valid_min, max_value=valid_max),
        }

        self.assert_valid(payload)

    def test_valid_number_with_min(self):
        valid_min = 100
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_NUMBER,
            parameters={
                'min_value': valid_min,
            },
        )

        payload = {
            field.name: fake.pyint(min_value=valid_min),
        }

        self.assert_valid(payload)

    def test_valid_number_with_max(self):
        valid_max = 200
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_NUMBER,
            parameters={
                'max_value': valid_max,
            },
        )

        payload = {
            field.name: fake.pyint(max_value=valid_max),
        }

        self.assert_valid(payload)

    def test_valid_number_without_min_max(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_NUMBER,
            parameters={},
        )

        payload = {
            field.name: fake.pyint(),
        }

        self.assert_valid(payload)

    def test_valid_select(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_SELECT,
            parameters={
                'options': [
                    {
                        'value': fake.pystr(min_chars=10, max_chars=20),
                        'content': fake.pystr(min_chars=10, max_chars=20),
                    } for _ in range(fake.pyint(min_value=2, max_value=10))
                ],
            },
        )

        payload = {
            field.name: fake.random_element(elements=field.options.keys()),
        }

        self.assert_valid(payload)

    def test_valid_multicheckbox(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_MULTICHECKBOX,
            parameters={
                'options': [
                    [
                        {
                            'value': fake.pystr(min_chars=10, max_chars=20),
                            'content': fake.pystr(min_chars=10, max_chars=20),
                        } for _ in range(fake.pyint(min_value=1, max_value=10))
                    ] for _ in range(fake.pyint(min_value=2, max_value=10))
                ],
            },
        )

        options = list(field.options.keys())
        payload = options[:2]

        self.assert_invalid(payload)

    def test_invalid_required_number(self):
        max_length = 200
        EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_NUMBER,
            is_required=True,
            parameters={
                'max_value': max_length,
            },
        )

        payload = {}

        self.assert_invalid(payload)

    def test_valid_required_number(self):
        max_length = 200
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_NUMBER,
            is_required=True,
            parameters={
                'max_value': max_length,
            },
        )

        payload = {
            field.name: fake.pyint(max_value=max_length),
        }

        self.assert_valid(payload)

    def test_invalid_text_required(self):
        max_length = 255
        EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_TEXT,
            is_required=True,
            parameters={'max_length': max_length},
        )

        payload = {}

        self.assert_invalid(payload)

    def test_invalid_empty_text_required(self):
        max_length = 255
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_TEXT,
            is_required=True,
            parameters={'max_length': max_length},
        )

        payload = {field.name: ''}

        self.assert_invalid(payload)

    def test_valid_text_required(self):
        max_length = 255
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_TEXT,
            is_required=True,
            parameters={'max_length': max_length},
        )

        payload = {field.name: fake.pystr(max_chars=max_length)}

        self.assert_valid(payload)

    def test_valid_text_not_required(self):
        max_length = 255
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_TEXT,
            is_required=False,
            parameters={'max_length': max_length},
        )

        payload = {field.name: ''}

        self.assert_valid(payload)

    def test_valid_multicheckbox_not_required(self):
        EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_MULTICHECKBOX,
            parameters={
                'options': [
                    [
                        {
                            'value': fake.pystr(min_chars=10, max_chars=20),
                            'content': fake.pystr(min_chars=10, max_chars=20),
                        } for _ in range(fake.pyint(min_value=1, max_value=10))
                    ] for _ in range(fake.pyint(min_value=2, max_value=10))
                ],
            },
            is_required=False,
        )

        payload = {}

        self.assert_valid(payload)

    def test_valid_multicheckbox_not_required_null(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_MULTICHECKBOX,
            parameters={
                'options': [
                    [
                        {
                            'value': fake.pystr(min_chars=10, max_chars=20),
                            'content': fake.pystr(min_chars=10, max_chars=20),
                        } for _ in range(fake.pyint(min_value=1, max_value=10))
                    ] for _ in range(fake.pyint(min_value=2, max_value=10))
                ],
            },
            is_required=False,
        )

        payload = {
            field.name: None,
        }

        self.assert_valid(payload)

    def test_valid_multicheckbox_not_required_empty(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_MULTICHECKBOX,
            parameters={
                'options': [
                    [
                        {
                            'value': fake.pystr(min_chars=10, max_chars=20),
                            'content': fake.pystr(min_chars=10, max_chars=20),
                        } for _ in range(fake.pyint(min_value=1, max_value=10))
                    ] for _ in range(fake.pyint(min_value=2, max_value=10))
                ],
            },
            is_required=False,
        )

        payload = {
            field.name: [],
        }

        self.assert_valid(payload)

    def test_dataset_invalid_dataset(self):
        with self.assertRaises(ValidationError):
            EnrollSurveyFieldFactory(
                survey=self.survey, field_type=EnrollSurveyField.TYPE_DATASET,
                parameters={
                    'dataset': {
                        'name': fake.pystr(min_chars=10, max_chars=20),
                    },
                },
                is_required=False,
            )

    def test_dataset_not_in_queryset(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_DATASET,
            parameters={
                'dataset': {
                    'name': 'course_categories',
                },
            },
            is_required=False,
        )

        CourseCategoryFactory.create_batch(3)
        inactive_category = CourseCategoryFactory(is_active=False)

        payload = {field.name: str(inactive_category.id)}
        self.assert_invalid(payload, num_queries=2)

        max_category_id = CourseCategory.objects.aggregate(max_id=Max('id')).get('max_id', 0) or 0
        payload = {field.name: str(max_category_id + 1)}
        self.assert_invalid(payload, num_queries=2)

    def test_dataset_in_queryset(self):
        field = EnrollSurveyFieldFactory(
            survey=self.survey, field_type=EnrollSurveyField.TYPE_DATASET,
            parameters={
                'dataset': {
                    'name': 'course_categories',
                },
            },
            is_required=False,
        )

        categories = CourseCategoryFactory.create_batch(3)

        payload = {field.name: str(categories[0].id)}
        self.assert_valid(payload, num_queries=2)
