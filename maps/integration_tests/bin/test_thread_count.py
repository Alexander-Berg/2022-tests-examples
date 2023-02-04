import json
import unittest
import logging
from library.python import resource

from .common import (
    start_task,
    get_task_result,
    API_ENDPOINT,
    ADD_MVRP_TASK_QUERY,
    TASK_MVRP_RESULT_QUERY_PATTERN
)


def start_normal_task(task, thread_count=None):
    task["options"]["quality"] = "normal"
    task["options"]["matrix_router"] = "geodesic"
    if thread_count is not None:
        task["options"]["thread_count"] = thread_count

    return start_task(API_ENDPOINT + ADD_MVRP_TASK_QUERY, task)


def get_optimization_steps(task_id):
    r = get_task_result(API_ENDPOINT + TASK_MVRP_RESULT_QUERY_PATTERN.format(task_id), task_id)
    if "result" not in r:
        logging.error(json.dumps(r, indent=4))
    return r["result"]["metrics"]["optimization_steps"]


class ThreadCountTest(unittest.TestCase):
    def test_thread_count(self):
        # If we increase the number of threads n times, we expect 0.7*n times improvement
        # But, qloud sometimes counts hyperthreaded cores as separate cores, and solver doesn't
        # have any improvements with hyperthreads.
        ACCEPTABLE_IMPROVEMENT_COEF = 2

        task = json.loads(resource.find("thread_count_task.json"))
        id1 = start_normal_task(task, thread_count=2)
        logging.info("Started task with 2 threads {}".format(id1))
        id2 = start_normal_task(task, thread_count=10)
        logging.info("Started task with 10 threads {}".format(id2))

        steps1 = get_optimization_steps(id1)
        steps2 = get_optimization_steps(id2)
        logging.info("Optimization steps with 2 threads {}".format(steps1))
        logging.info("Optimization steps with 10 threads {}".format(steps2))
        self.assertTrue(steps2 > ACCEPTABLE_IMPROVEMENT_COEF * steps1)
