import factory
import pytest

from watcher.db import Problem
from watcher.enums import ProblemReason
from watcher.logic.timezone import now
from .base import PROBLEM_SEQUENCE


@pytest.fixture(scope='function')
def problem_factory(meta_base, shift_factory, staff_factory):
    class ProblemFactory(factory.alchemy.SQLAlchemyModelFactory):
        class Meta(meta_base):
            model = Problem

        id = factory.Sequence(lambda n: n + PROBLEM_SEQUENCE)
        shift = factory.SubFactory(shift_factory)
        staff = factory.SubFactory(staff_factory)
        report_date = now()
        reason = ProblemReason.nobody_on_duty

    return ProblemFactory
