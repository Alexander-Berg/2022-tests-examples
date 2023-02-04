import pytest
from billing.hot.faas.tasklets.deploy_faas.impl.deploy_controller.stage import Stage
from copy import deepcopy
from yp import data_model


class TestCheckChangesNoChanges:
    @pytest.fixture
    def sequence_func(self, faas_tasklet, mock):
        return lambda: [
            Stage("mock", mock),
            Stage("testing changes", faas_tasklet._check_changes_in_spec),
        ]

    @pytest.fixture
    def spec(self, base_spec):
        return deepcopy(base_spec)

    @pytest.fixture
    def faas_resources(self):
        return []

    def test(self, tasklet_with_sequence, spec):
        tasklet_with_sequence.run()
        assert (
            str(tasklet_with_sequence.deploy_controller.graceful_exception) == "Gracefull shutdown: No changes to "
            "spec, nothing to commit"
        )


class TestCheckChanges:
    @pytest.fixture
    def sequence_func(self, faas_tasklet, mock):
        def change_spec():
            faas_tasklet.context.spec = data_model.TStageSpec()

        return lambda: [
            Stage("mock", mock),
            Stage("get spec", faas_tasklet._get_current_spec),
            Stage("change spec", change_spec),
            Stage("testing changes", faas_tasklet._check_changes_in_spec),
        ]

    @pytest.fixture
    def spec(self, base_spec):
        return deepcopy(base_spec)

    @pytest.fixture
    def faas_resources(self):
        return []

    def test(self, tasklet_with_sequence, spec):
        tasklet_with_sequence.run()
        assert tasklet_with_sequence.deploy_controller.graceful_exception is None
