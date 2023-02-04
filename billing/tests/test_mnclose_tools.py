import luigi
import pytest

from dwh.grocery.task.mnclose import (
    ExternalMNClose,
    NO_MNCLOSE,
    mnclosed,
)
from dwh.grocery.targets.mnclose_target import MNCloseTarget
from dwh.grocery.tools.conf import (
    GROCERY_TMP_ROOT,
)


class FakeExternalMNClose(ExternalMNClose):
    default_task = NO_MNCLOSE


class SadTask(luigi.ExternalTask):

    def output(self):
        return luigi.LocalTarget(str(GROCERY_TMP_ROOT / "kolbaska"))


class TestMNCloseTools:

    @pytest.mark.skip(reason="not fixed yet")
    def test_mnclosed(self):

        @mnclosed(FakeExternalMNClose)
        class A(luigi.Task):

            def requires(self):
                return {
                    'kolbaska': SadTask()
                }

        a = A()
        inpt = a.input()
        assert "mnclose" in inpt
        assert len(inpt) == 2
        mnclose_target: MNCloseTarget = inpt['mnclose']
        assert isinstance(mnclose_target, MNCloseTarget)
        assert mnclose_target.exists()

        # luigi.parameter.UnknownParameterException: A[args=(), kwargs={'mnclose': 'xxx'}]: unknown parameter mnclose
        a_f = A(mnclose="xxx")
        inpt = a_f.input()
        assert "mnclose" in inpt
        assert len(inpt) == 1
        mnclose_target: MNCloseTarget = inpt['mnclose']
        assert isinstance(mnclose_target, MNCloseTarget)
        assert not mnclose_target.exists()
