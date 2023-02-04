# -*- coding: utf-8 -*-

import datetime as dt

import pytest
from autodasha.db import mapper as a_mapper


@pytest.fixture
def queue_object(session):
    issue = a_mapper.Issue(id='test_check_export', key='test_check_export')
    queue_obj = a_mapper.ExportCheckQueueObject(issue=issue)
    session.add(queue_obj)

    queue_obj.next_dt = dt.datetime.combine(dt.date.today(), dt.time())
    session.add(queue_obj)
    session.flush()
    return queue_obj
