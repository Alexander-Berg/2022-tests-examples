# -*- coding: utf-8 -*-

import pprint
import pytest
import yaml

import mocks as mocked
import pipeline_templates as temp
from ..pipeliner import Pipeline, restore_pipeline, fill_defaults
from ..utils import SeveralAllowedTransitionsException, load_pipeline

INITIAL = 'INITIAL'


def get_pipeline(pipeline, state=INITIAL, alarm=None, _is_file=False):
    ticket = mocked.StartrekTicket(state=state, alarm=alarm)
    pipeline = load_pipeline(pipeline, _is_file=_is_file)
    pipeline = restore_pipeline(pipeline)
    pipeline = fill_defaults(pipeline)
    p = Pipeline(pipeline=pipeline, ticket=ticket)
    return (p, ticket)


# ------------------------------------------------------------------------------------------------------

def test_several_allowed_transitions_exception():
    p, _ = get_pipeline(pipeline=temp.SEVERAL_ALLOWED_TRANSITIONS)
    with pytest.raises(SeveralAllowedTransitionsException):
        p.go()


def test_no_allowed_transitions():
    p, _ = get_pipeline(pipeline=temp.NO_ALLOWED_TRANSITIONS)
    p.go()
    assert p.state == INITIAL


def test_end_of_pipeline():
    p, _ = get_pipeline(pipeline=temp.TRIVIAL_GRAPH)
    p.go()
    assert p.state == INITIAL


@pytest.mark.parametrize('yaml_file', [temp.ALARM_EXCEPTION_IN_CONDITION,
                                       temp.ALARM_EXCEPTION_IN_ACTION])
def test_alarm_raising(yaml_file):
    p, ticket = get_pipeline(pipeline=yaml_file)
    p.go()
    assert p.state == INITIAL
    assert ticket.crashId == u'alarm'
    p.go()
    assert p.state == INITIAL
    assert ticket.crashId == u'alarm'


def test_smoke_pipeline():
    p, ticket = get_pipeline(pipeline=temp.SUCCESSFULL_SMOKE)
    assert p.state == INITIAL
    p.go()
    assert p.state == 'HAVE_CHANGES'
    p.go()
    assert p.state == 'DEPLOYED'
    p.go()
    assert p.state == 'TEST_IN_PROGRESS'
    assert len(p.ticket.comments.comments) == 1
    p.go()
    assert p.state == 'TEST_COMPLETE'
    pass


def test_restore_pipeline_all():
    p, _ = get_pipeline(pipeline='pipelines/totally_imported.yml', _is_file=True)
    assert p.state == INITIAL
    p.go()
    assert p.state == 'HAVE_CHANGES'
    p.go()
    assert p.state == 'DEPLOYED'
    p.go()
    assert p.state == 'TEST_IN_PROGRESS'
    pass
