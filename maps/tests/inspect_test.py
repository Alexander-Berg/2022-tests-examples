import json

import pytest
from click.testing import CliRunner

from maps.infra.sandbox import SedemManagedMixin, ReleaseSpec, inspect
from sandbox import sdk2
from sandbox.common.projects_handler import TaskTypeLocation


def invoke_extract_params(task_type: str) -> str:
    runner = CliRunner()
    result = runner.invoke(inspect.extract_params, ['--task-type', task_type])
    if result.exception:
        raise result.exception
    assert result.exit_code == 0, result.output
    return result.output


class ExampleTask(SedemManagedMixin, sdk2.Task):
    class Parameters(SedemManagedMixin.Parameters):
        secret = sdk2.parameters.YavSecret('Some secret', required=True)
        string = sdk2.parameters.String('Some string', required=True)

        @classmethod
        def release_spec(cls) -> ReleaseSpec:
            spec = super().release_spec()

            stable_unit = spec.add_deploy_unit('stable')
            stable_unit.secret = 'sec-XXX'
            stable_unit.string = 'some string'

            testing_unit = spec.add_deploy_unit('testing')
            testing_unit.secret = 'sec-YYY'
            testing_unit.string = 'another string'

            return spec


@pytest.fixture(scope='function', autouse=True)
def mock_tasks_loader(monkeypatch) -> None:
    def fake_project_types() -> dict[TaskTypeLocation]:
        return {
            ExampleTask.type: TaskTypeLocation(__name__, ExampleTask, 0)
        }

    monkeypatch.setattr(
        inspect.projects_handler,
        'load_project_types',
        fake_project_types
    )


def test_params_extraction() -> None:
    params_json = invoke_extract_params('EXAMPLE_TASK')
    assert json.loads(params_json) == {
        'task_type': 'EXAMPLE_TASK',
        'deploy_units': [
            {
                'name': 'stable',
                'parameters': [{
                    'name': 'string',
                    'jsonValue': '"some string"',
                }],
                'secrets': [{
                    'name': 'secret',
                    'secret': {
                        'secret_id': 'sec-XXX',
                    },
                }],
            },
            {
                'name': 'testing',
                'parameters': [{
                    'name': 'string',
                    'jsonValue': '"another string"',
                }],
                'secrets': [{
                    'name': 'secret',
                    'secret': {
                        'secret_id': 'sec-YYY',
                    },
                }],
            },
        ],
    }
