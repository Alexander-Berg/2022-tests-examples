import os
import sys

import pytest
import yatest

from ads.ml_engine.tests_tasks.test_expand import (
    get_ymls, production_folders, is_not_graveyard, list_dumps_mock,
    is_testable_task,
)
from ads.libs.py_yaml_loader import (
    load_task_pipe,
    LoadTasks, GenerateTasks,
    LastLogDateSetter,
    InflateTasks,
    HeadTasks, EditTasks,
)
import yabs.ml.dump


learn_tasks_path = yatest.common.source_path('yabs/utils/learn-tasks2')
folders_to_test = [
    d for d in os.listdir(learn_tasks_path)
    if os.path.isdir(os.path.join(learn_tasks_path, d)) and d not in production_folders
]

ymls = get_ymls(learn_tasks_path, folders_to_test, is_not_graveyard)
yml_names = [yml.split('learn-tasks2/')[1] for yml in ymls]


@pytest.fixture
def mock_list_dumps(monkeypatch):
    monkeypatch.setattr(yabs.ml.dump, 'list_dumps', list_dumps_mock)


@pytest.mark.parametrize("yml", ymls, ids=yml_names)  # noqa
def test_expand(yml, mock_list_dumps):

    if not is_testable_task(yml, learn_tasks_path):
        print >> sys.stderr, "Task contains strange parts; skip tests for it {}".format(yml)
        return

    tasks_pipe = load_task_pipe(
        loader=LoadTasks([yml]),
        modifiers=[
            LastLogDateSetter(timestamp=100000),
            GenerateTasks(),
            InflateTasks(),
            HeadTasks(),
            EditTasks(),
        ],
    )
    for task in tasks_pipe:
        pass
