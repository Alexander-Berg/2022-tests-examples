import pytest

from django.core.exceptions import ValidationError

from kelvin.accounts.factories import UserFactory
from kelvin.courses.factories import CourseFactory, CourseLessonLinkFactory, CriterionFactory
from kelvin.courses.models.criterion.condition import validate_formula
from kelvin.lessons.factories import LessonFactory, LessonProblemLinkFactory
from kelvin.results.factories import CourseLessonResultFactory


class TestConditionsValidator:
    CLESSON_CONDITIONS = (
        "ClessonPoints",
        "ClessonPointsPercentage",
        "ClessonProgressPercentage",
    )

    CLESSON_INVALID = (
        (
            {
                "aggregator": "proisvedenie",
                "comparator": ">",
                "value": 100,
                "clesson_id": 1,
            }
        ),
        (
            {
                "aggregator": "sum",
                "comparator": "menshe",
                "value": 100,
                "clesson_id": 1,
            }
        ),
        (
            {
                "aggregator": "sum",
                "comparator": ">",
                "value": 101,
                "clesson_id": 1,
            }
        ),
        (
            {
                "aggregator": "sum",
                "comparator": ">",
                "value": 100,
            }
        ),
    )

    @pytest.mark.parametrize('clesson_condition', CLESSON_CONDITIONS)
    @pytest.mark.parametrize('request_data', CLESSON_INVALID)
    def test_clesson_points_invalid(self, clesson_condition, request_data):
        request_dict = {
            **{"type": clesson_condition}, **request_data,
        }
        with pytest.raises(ValidationError):
            validate_formula(request_dict)

    @pytest.mark.parametrize('clesson_condition', CLESSON_CONDITIONS)
    def test_clesson_points_valid(self, clesson_condition):
        request_dict = {
            "type": clesson_condition,
            "aggregator": "sum",
            "comparator": ">",
            "value": 50,
            "clesson_id": 1,
        }
        assert validate_formula(request_dict) is None

    COURSE_CONDITIONS = (
        "CoursePoints",
        "CoursePointsPercentage",
        "CourseProgressPercentage",
    )
    COURSE_INVALID = (
        (
            {
                "aggregator": "proisvedenie",
                "comparator": ">",
                "value": 100,
            }
        ),
        (
            {
                "aggregator": "sum",
                "comparator": "menshe",
                "value": 100,
            }
        ),
        (
            {
                "aggregator": "sum",
                "comparator": ">",
                "value": 101,
            }
        ),
    )

    @pytest.mark.parametrize('course_condition', COURSE_CONDITIONS)
    @pytest.mark.parametrize('request_data', COURSE_INVALID)
    def test_clesson_points_invalid(self, course_condition, request_data):
        request_dict = {
            **{"type": course_condition}, **request_data,
        }
        with pytest.raises(ValidationError):
            validate_formula(request_dict)

    @pytest.mark.parametrize('course_condition', COURSE_CONDITIONS)
    def test_clesson_points_valid(self, course_condition):
        request_dict = {
            "type": course_condition,
            "aggregator": "sum",
            "comparator": ">",
            "value": 50,
        }
        assert validate_formula(request_dict) is None

    def test_invalid_or_and(self):
        with pytest.raises(ValidationError):
            validate_formula(
                {
                    "AND": [
                        {
                            "type": "ClessonPoints",
                            "aggregator": "max",
                            "comparator": "==",
                            "value": 50,
                            "clesson_id": 1,
                        },
                    ],
                    "OR": [
                        {
                            "type": "ClessonPoints",
                            "aggregator": "max",
                            "comparator": "==",
                            "value": 50,
                            "clesson_id": 1,
                        },
                    ],
                }
            )

    def test_empty_and_or(self):
        with pytest.raises(ValidationError):
            validate_formula(
                {
                    "AND": [
                        {
                            "OR": [],
                        },
                    ]
                }
            )

    def test_deep_nested_conditions(self):
        assert validate_formula(
            {
                "OR": [
                    {
                        "AND": [
                            {
                                "OR": [
                                    {
                                        "AND": [
                                            {
                                                "type": "ClessonPoints",
                                                "aggregator": "max",
                                                "comparator": "==",
                                                "value": 50,
                                                "clesson_id": 1,
                                            },
                                            {
                                                "type": "ClessonProgressPercentage",
                                                "aggregator": "mul",
                                                "comparator": "<=",
                                                "value": 100,
                                                "clesson_id": 2,
                                            },
                                        ]
                                    },
                                    {
                                        "type": "CourseProgressPercentage",
                                        "aggregator": "max",
                                        "comparator": "!=",
                                        "value": 100,
                                    },
                                    {
                                        "type": "CourseProgressPercentage",
                                        "aggregator": "any",
                                        "comparator": ">",
                                        "value": 80,
                                    },
                                ]
                            }
                        ]
                    },
                    {
                        "OR": [
                            {
                                "type": "CoursePoints",
                                "aggregator": "sum",
                                "comparator": ">",
                                "value": 50,
                            },
                            {
                                "type": "CoursePointsPercentage",
                                "aggregator": "min",
                                "comparator": "<",
                                "value": 100,
                            },
                        ]
                    }
                ]
            }
        ) is None


@pytest.mark.django_db
class TestConditionEval:
    CLESSON_POINTS_CRITERIONS = (
        (
            {
                "type": "ClessonPoints",
                "value": 80,
                "clesson_id": "cll.id",
                "comparator": "==",
                "aggregator": "max",
            },
            70,
            80,
            False,
        ),
        (
            {
                "type": "ClessonPointsPercentage",
                "value": 50,
                "clesson_id": "cll.id",
                "comparator": ">=",
                "aggregator": "max",
            },
            10,
            20,
            True,
        )
    )

    @pytest.mark.parametrize('formula,points,max_points,result', CLESSON_POINTS_CRITERIONS)
    def test_eval_clesson_points(self, formula, points, max_points, result):
        student = UserFactory()
        course = CourseFactory()
        cll = CourseLessonLinkFactory(course=course)
        if "clesson_id" in formula:
            formula["clesson_id"] = cll.id
        clr = CourseLessonResultFactory(
            summary__student=student,
            summary__clesson=cll,
            points=points,
            max_points=max_points,
            answers={},
        )
        criterion = CriterionFactory(
            formula=formula,
            priority=1,
            assignment_rule__course=course,
        )

        assert criterion.evaluate(student) == result

    CLESSON_PROGRESS_CRITERIONS = (
        (
            {
                "type": "ClessonProgressPercentage",
                "value": 50,
                "clesson_id": "cll.id",
                "comparator": ">=",
                "aggregator": "max",
            },
            2,
            True,
        ),
        (
            {
                "type": "ClessonProgressPercentage",
                "value": 50,
                "clesson_id": "cll.id",
                "comparator": ">=",
                "aggregator": "max",
            },
            1,
            False,
        ),
    )

    @pytest.mark.parametrize('formula,num_answered,result', CLESSON_PROGRESS_CRITERIONS)
    def test_eval_clesson_progress(self, formula, num_answered, result):
        student = UserFactory()
        course = CourseFactory()
        lesson = LessonFactory()
        cll = CourseLessonLinkFactory(course=course, lesson=lesson)
        lpls = LessonProblemLinkFactory.create_batch(3, lesson=lesson)

        if "clesson_id" in formula:
            formula["clesson_id"] = cll.id
        clr = CourseLessonResultFactory(
            summary__student=student,
            summary__clesson=cll,
            points=1,
            max_points=1,
            answers={str(i): i for i in range(num_answered)},
        )
        criterion = CriterionFactory(
            formula=formula,
            priority=1,
            assignment_rule__course=course,
        )

        assert criterion.evaluate(student) == result

    COURSE_POINTS_CRITERIONS = (
        (
            {
                "type": "CoursePoints",
                "value": 70,
                "comparator": ">=",
                "aggregator": "sum",
            },
            3,
            20,
            20,
            False,
        ),
        (
            {
                "type": "CoursePointsPercentage",
                "value": 50,
                "comparator": ">=",
                "aggregator": "sum",
            },
            3,
            5,
            10,
            True,
        ),
    )

    @pytest.mark.parametrize('formula,num_lessons,points,max_points,result', COURSE_POINTS_CRITERIONS)
    def test_eval_course_points(self, formula, num_lessons, points, max_points, result):
        student = UserFactory()
        course = CourseFactory()
        lessons = LessonFactory.create_batch(num_lessons)
        clls = [CourseLessonLinkFactory(course=course, lesson=lesson) for lesson in lessons]
        lpls = [LessonProblemLinkFactory.create_batch(3, lesson=lesson) for lesson in lessons]

        clrs = [
            CourseLessonResultFactory(
                summary__student=student,
                summary__clesson=cll,
                points=points,
                max_points=max_points,
                answers={},
            )
            for cll in clls
        ]

        criterion = CriterionFactory(
            formula=formula,
            priority=1,
            assignment_rule__course=course,
        )
        assert criterion.evaluate(student) == result

    COURSE_PROGRESS_CRITERIONS = (
        (
            {
                "type": "CourseProgressPercentage",
                "value": 70,
                "comparator": ">=",
                "aggregator": "sum",
            },
            3,
            3,
            2,
            False,
        ),
        (
            {
                "type": "CourseProgressPercentage",
                "value": 50,
                "comparator": ">=",
                "aggregator": "sum",
            },
            3,
            4,
            2,
            True,
        ),
    )

    @pytest.mark.parametrize('formula,num_lessons,num_problems,num_answered,result', COURSE_PROGRESS_CRITERIONS)
    def test_eval_course_points(self, formula, num_lessons, num_problems, num_answered, result):
        student = UserFactory()
        course = CourseFactory()
        lessons = LessonFactory.create_batch(num_lessons)
        clls = [CourseLessonLinkFactory(course=course, lesson=lesson) for lesson in lessons]
        lpls = [LessonProblemLinkFactory.create_batch(num_problems, lesson=lesson) for lesson in lessons]

        clrs = [
            CourseLessonResultFactory(
                summary__student=student,
                summary__clesson=cll,
                points=1,
                max_points=1,
                answers={str(i): i for i in range(num_answered)},
            )
            for cll in clls
        ]

        criterion = CriterionFactory(
            formula=formula,
            priority=1,
            assignment_rule__course=course,
        )
        assert criterion.evaluate(student) == result
