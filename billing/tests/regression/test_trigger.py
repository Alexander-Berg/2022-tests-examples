import pytest

from dwh.grocery.task import OracleUpdateDTYTTask
from dwh.grocery.targets import OracleTableTarget, TriggerStatus


class TestTriggerTarget:

    def test_trigger_creation_happy_path(self, table_with_update_dt_yt):
        table: OracleTableTarget = table_with_update_dt_yt
        trigger_task = OracleUpdateDTYTTask(table=table).requires()
        trigger = trigger_task.output()
        assert not trigger.exists()
        trigger_task.run()
        assert trigger.exists()
        assert trigger.get_status() == TriggerStatus.valid
        trigger.drop()
        assert not trigger.exists()

    def test_trigger_creation_sad_path(self, fake_t_product):
        table: OracleTableTarget = fake_t_product
        trigger_task = OracleUpdateDTYTTask(table=table).requires()
        trigger = trigger_task.output()
        assert not trigger.exists()
        with pytest.raises(RuntimeError):
            trigger_task.run()
        assert not trigger.exists()
        assert trigger.get_status() is None
