import json
import uuid

import pytest
from django_celery_results import models


@pytest.fixture
def task_builder(db):
    def builder(**kwargs):
        kwargs.setdefault('task_id', str(uuid.uuid4()))
        task_args = kwargs.get('task_args')
        if task_args and not isinstance(task_args, str):
            kwargs['task_args'] = json.dumps(task_args)
        task_kwargs = kwargs.get('task_kwargs')
        if task_kwargs and not isinstance(task_kwargs, str):
            kwargs['task_kwargs'] = json.dumps(task_kwargs)
        kwargs.setdefault('content_type', 'unknown')
        kwargs.setdefault('content_encoding', 'unknown')
        return models.TaskResult.objects.create(**kwargs)

    return builder
