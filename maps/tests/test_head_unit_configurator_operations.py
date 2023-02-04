from .basic_test import BasicTest

from data_types.head_unit import HeadUnit

import lib.manager as manager
import lib.db as db

import rstr
import string
import time


class TestHeadUnitConfiguratorOperations(BasicTest):
    def test_get_config_endpoint(self):
        db.remove_experiments()

        for _ in self.wait_for_experiments_update():
            head_id = HeadUnit().head_id
            experiment_name = rstr.rstr(string.ascii_letters, 32)

            no_experiment_result = manager.get_experiment_config(
                experiment_name=experiment_name,
                head_id=head_id
            ) >> 200
            enabled = no_experiment_result['enabled']
            if not enabled:
                break
        assert not enabled

        db.add_experiment(experiment_name, head_id)

        for _ in self.wait_for_experiments_update():
            has_experiment_result = manager.get_experiment_config(
                experiment_name=experiment_name,
                head_id=head_id,
            ) >> 200
            enabled = has_experiment_result['enabled']
            if enabled:
                break
        assert enabled

    def test_is_alive_endpoint(self):
        head_id = HeadUnit().head_id
        head_timestamp = int(time.time())

        response = manager.put_is_alive(
            head_id=head_id,
            head_timestamp=head_timestamp,
        ) >> 200

        assert response['status'] == 'ok'
        assert response['headid'] == head_id
        assert response['head_timestamp'] == head_timestamp

    def test_is_alive_with_invalid_timestamp(self):
        head_id = HeadUnit().head_id
        head_timestamp = int(time.time()) * 1000

        response = manager.put_is_alive(
            head_id=head_id,
            head_timestamp=head_timestamp,
        ) >> 200

        assert response['status'] == 'ok'
        assert response['headid'] == head_id
        assert response['head_timestamp'] == head_timestamp
