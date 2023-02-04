import random
import time
from builtins import object

import pytest

from django.db.utils import IntegrityError

from kelvin.courses.models import AssignmentRule, Course


class TestAssignmentRules(object):
    """
    Тесты для проверки операций с моделью правил назначения
    """

    @pytest.mark.django_db
    def test_cannot_create_duplicate_formula(self, simple_course):
        """
        Проверяем, что нельзя одному курсу добавить правила с двумя одинаковыми формулами
        """
        # INIT
        rule1 = AssignmentRule.objects.create(
            title="rule1",
            formula="[[]]",
            course=simple_course
        )
        # RUN
        with pytest.raises(IntegrityError) as exception_info:
            rule2 = AssignmentRule.objects.create(
                title="rule2",
                formula="[[]]",
                course=simple_course
            )
        # TEST
        assert exception_info is not None

    @pytest.mark.django_db
    def test_cannot_create_duplicate_title(self, simple_course):
        """
        Проверяем, что нельзя одному курсу добавить правила с двумя одинаковыми тайтлами
        """
        # INIT
        rule1 = AssignmentRule.objects.create(
            title="rule1",
            formula="[[{'k2': 'v2'}]]",
            course=simple_course
        )
        # RUN
        with pytest.raises(IntegrityError) as exception_info:
            rule2 = AssignmentRule.objects.create(
                title="rule1",
                formula="[[{'k2': 'v2'}]]",
                course=simple_course
            )
        # TEST
        assert exception_info is not None

    @pytest.mark.django_db
    def test_cannot_create_non_duplicate_formula(self, simple_course):
        """
        Проверяем, что формулы могут быть одинаковыми в рамках разных курсов
        """
        # INIT
        course_copy = Course.objects.get(pk=simple_course.id)
        course_copy.pk = None
        course_copy.id = None
        course_copy.name = u"blablabla"
        course_copy.code = u"FFFF0000"
        course_copy.save()
        rule1 = AssignmentRule.objects.create(
            title="rule1",
            formula="[[]]",
            course=simple_course
        )
        # RUN
        rule2 = AssignmentRule.objects.create(
            title="rule2",
            formula="[[]]",
            course=course_copy
        )
        # TEST
        assert rule2.id > rule1.id

    @pytest.mark.django_db
    def test_cannot_create_non_duplicate_title(self, simple_course):
        """
        Проверяем, что тайтлы могут быть одинаковыми в рамках разных курсов
        """
        # INIT
        course_copy = Course.objects.get(pk=simple_course.id)
        course_copy.pk = None
        course_copy.id = None
        course_copy.name = "blablabla"
        course_copy.code = u"FFFF0000"
        course_copy.save()
        rule1 = AssignmentRule.objects.create(
            title="rule1",
            formula="[[{'k1': 'v1'}]]",
            course=simple_course
        )
        # RUN
        rule2 = AssignmentRule.objects.create(
            title="rule1",
            formula="[[{'k2': 'v2'}]]",
            course=course_copy
        )
        # TEST
        assert rule2.id > rule1.id
