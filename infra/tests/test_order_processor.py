# coding=utf-8
import logging

import enum
import pytest

from awacs.lib.order_processor.model import needs_removal, WithOrder, BaseProcessor, cancel_order
from awacs.lib.order_processor.runner import StateRunner
from infra.awacs.proto import model_pb2
from awtest import check_log


@pytest.fixture(autouse=True)
def setup_logging(caplog):
    caplog.set_level(logging.DEBUG)


def test_needs_removal():
    for cert_pb in (model_pb2.Certificate(), model_pb2.CertificateRenewal()):
        cert_pb.spec.state = model_pb2.CertificateSpec.PRESENT
        assert not needs_removal(cert_pb)
        cert_pb.spec.state = model_pb2.CertificateSpec.REMOVED_FROM_AWACS
        assert needs_removal(cert_pb)

    balancer_pb = model_pb2.Balancer()
    assert not needs_removal(balancer_pb)

    for e_pb in (model_pb2.Backend(), model_pb2.Domain()):
        e_pb.spec.deleted = False
        assert not needs_removal(e_pb)
        e_pb.spec.deleted = True
        assert needs_removal(e_pb)

    for e_pb in (model_pb2.BalancerOperation(), model_pb2.DomainOperation()):
        e_pb.spec.incomplete = True
        assert not needs_removal(e_pb)
        e_pb.spec.incomplete = False
        assert needs_removal(e_pb)


def test_runner(ctx):
    b_pb = model_pb2.Namespace()
    b_pb.meta.id = 'ns'

    class States1(enum.Enum):
        START = 1
        RUN = 2
        FINISHED = 3
        CANCELLING = 4
        CANCELLED = 5

    class States2(enum.Enum):
        WRONG = 1

    class E(WithOrder):
        name = 'E'
        states = States1

        def zk_update(self, *_, **__):
            pass

    with pytest.raises(AssertionError, match='Unknown final cancelled state "States2.WRONG"'):
        StateRunner(
            entity_class=E,
            processing_interval=0.0,
            processors=[],
            initial_state=States1.START,
            final_state=States1.FINISHED,
            final_cancelled_state=States2.WRONG)

    with pytest.raises(AssertionError, match='Initial, final, and final cancelled states cannot match: '
                                             'States1.START / States1.START / States1.CANCELLING'):
        StateRunner(
            entity_class=E,
            processing_interval=0.0,
            processors=[],
            initial_state=States1.START,
            final_state=States1.START,
            final_cancelled_state=States1.CANCELLING)

    with pytest.raises(AssertionError, match='Initial, final, and final cancelled states cannot match: '
                                             'States1.START / States1.FINISHED / States1.START'):
        StateRunner(
            entity_class=E,
            processing_interval=0.0,
            processors=[],
            initial_state=States1.START,
            final_state=States1.FINISHED,
            final_cancelled_state=States1.START)

    with pytest.raises(AssertionError, match='Initial, final, and final cancelled states cannot match: '
                                             'States1.FINISHED / States1.START / States1.START'):
        StateRunner(
            entity_class=E,
            processing_interval=0.0,
            processors=[],
            initial_state=States1.FINISHED,
            final_state=States1.START,
            final_cancelled_state=States1.START)

    with pytest.raises(AssertionError, match='Some states have no corresponding processors: CANCELLING, RUN, START'):
        StateRunner(
            entity_class=E,
            processing_interval=0.0,
            processors=[],
            initial_state=States1.START,
            final_state=States1.FINISHED,
            final_cancelled_state=States1.CANCELLED)

    class Incomplete(BaseProcessor):
        pass

    with pytest.raises(AssertionError, match='Processor "Incomplete" must implement attribute "state"'):
        StateRunner(
            entity_class=E,
            processing_interval=0.0,
            processors=[Incomplete],
            initial_state=States1.START,
            final_state=States1.FINISHED,
            final_cancelled_state=States1.CANCELLED)

    class Incomplete(BaseProcessor):
        state = States1.START

    with pytest.raises(AssertionError, match='Processor "Incomplete" must implement attribute "next_state"'):
        StateRunner(
            entity_class=E,
            processing_interval=0.0,
            processors=[Incomplete],
            initial_state=States1.START,
            final_state=States1.FINISHED,
            final_cancelled_state=States1.CANCELLED)

    class Incomplete(BaseProcessor):
        state = States1.START
        next_state = States1.RUN

    with pytest.raises(AssertionError, match='Processor "Incomplete" must implement attribute "cancelled_state"'):
        StateRunner(
            entity_class=E,
            processing_interval=0.0,
            processors=[Incomplete],
            initial_state=States1.START,
            final_state=States1.FINISHED,
            final_cancelled_state=States1.CANCELLED)

    class Incomplete(BaseProcessor):
        state = States1.START
        next_state = States1.RUN
        cancelled_state = States1.CANCELLING

    with pytest.raises(AssertionError, match='Processor "Incomplete" must implement method "process"'):
        StateRunner(
            entity_class=E,
            processing_interval=0.0,
            processors=[Incomplete],
            initial_state=States1.START,
            final_state=States1.FINISHED,
            final_cancelled_state=States1.CANCELLED)

    class WrongState(BaseProcessor):
        state = States2.WRONG
        next_state = States1.RUN
        cancelled_state = States1.CANCELLING

        def process(self, ctx):
            pass

    with pytest.raises(AssertionError, match='Processor "WrongState" has unknown state "States2.WRONG"'):
        StateRunner(
            entity_class=E,
            processing_interval=0.0,
            processors=[WrongState],
            initial_state=States1.START,
            final_state=States1.FINISHED,
            final_cancelled_state=States1.CANCELLED)

    class P1(BaseProcessor):
        state = States1.START
        next_state = States1.RUN
        cancelled_state = States1.CANCELLING

        def process(self, ctx):
            pass

    class P2(BaseProcessor):
        state = States1.START
        next_state = States1.FINISHED
        cancelled_state = States1.CANCELLING

        def process(self, ctx):
            pass

    with pytest.raises(AssertionError, match='Duplicate processors for state "START": "P2" and "P1"'):
        StateRunner(
            entity_class=E,
            processing_interval=0.0,
            processors=[P1, P2],
            initial_state=States1.START,
            final_state=States1.FINISHED,
            final_cancelled_state=States1.CANCELLED)

    P2.state = States1.RUN

    class P3(BaseProcessor):
        state = States1.CANCELLING
        next_state = States1.CANCELLED
        cancelled_state = States1.CANCELLING

        def process(self, ctx):
            pass

    with pytest.raises(AssertionError, match='Cancel processor "P3" cannot have a cancelled state'):
        StateRunner(
            entity_class=E,
            processing_interval=0.0,
            processors=[P1, P2, P3],
            initial_state=States1.START,
            final_state=States1.FINISHED,
            final_cancelled_state=States1.CANCELLED)

    P2.cancelled_state = States1.CANCELLING
    P3.cancelled_state = None

    StateRunner(
        entity_class=E,
        processing_interval=0.0,
        processors=[P1, P2, P3],
        initial_state=States1.START,
        final_state=States1.FINISHED,
        final_cancelled_state=States1.CANCELLED)


def test_running_and_cancelling(ctx):
    n_pb = model_pb2.Namespace()
    n_pb.meta.id = 'ns'

    class States(enum.Enum):
        START = 1
        FINISHED = 2
        CANCELLING_1 = 3
        CANCELLING_2 = 4
        CANCELLED = 6

    class E(WithOrder):
        name = 'E'
        states = States

        def zk_update(self):
            yield n_pb

    class P1(BaseProcessor):
        state = States.START
        next_state = States.FINISHED
        cancelled_state = States.CANCELLING_1

        def process(self, ctx):
            return self.next_state

    class P2(BaseProcessor):
        state = States.CANCELLING_1
        next_state = States.CANCELLING_2
        cancelled_state = None

        def process(self, ctx):
            return self.next_state

    class P3(BaseProcessor):
        state = States.CANCELLING_2
        next_state = States.CANCELLED
        cancelled_state = None

        def process(self, ctx):
            raise RuntimeError('oops')

    runner = StateRunner(
        entity_class=E,
        processing_interval=0.0,
        processors=[P1, P2, P3],
        initial_state=States.START,
        final_state=States.FINISHED,
        final_cancelled_state=States.CANCELLED)

    ctx.log.info('1. Normal run')

    runner.process(ctx, n_pb)
    assert n_pb.order.progress.state.id == 'START'
    runner.process(ctx, n_pb)
    assert n_pb.order.progress.state.id == 'FINISHED'

    ctx.log.info('2. Cancelled order')

    n_pb = model_pb2.Namespace()
    n_pb.meta.id = 'ns'
    runner.process(ctx, n_pb)
    cancel_order(n_pb, 'author', 'comment')
    runner.process(ctx, n_pb)
    assert n_pb.order.progress.state.id == 'CANCELLING_2'

    ctx.log.info('3. Cancelled order, error in cancelling processor')

    def process_with_error(self, ctx):
        raise RuntimeError('oops')

    P2.process = process_with_error
    n_pb = model_pb2.Namespace()
    n_pb.meta.id = 'ns'
    runner.process(ctx, n_pb)
    cancel_order(n_pb, 'author', 'comment')
    with pytest.raises(RuntimeError):
        runner.process(ctx, n_pb)
    assert n_pb.order.progress.state.id == 'CANCELLING_1'

    def good_process(self, ctx):
        return self.next_state

    P2.process = good_process

    runner.process(ctx, n_pb)
    assert n_pb.order.progress.state.id == 'CANCELLING_2'
    with pytest.raises(RuntimeError):
        runner.process(ctx, n_pb)
    assert n_pb.order.progress.state.id == 'CANCELLING_2'

    P3.process = good_process
    runner.process(ctx, n_pb)
    assert n_pb.order.progress.state.id == 'CANCELLED'


def test_force_cancelling(ctx):
    n_pb = model_pb2.Namespace()
    n_pb.meta.id = 'ns'

    class States(enum.Enum):
        START = 1
        FINISHED = 2
        CANCELLING = 3
        CANCELLED = 6

    class E(WithOrder):
        name = 'E'
        states = States

        def zk_update(self):
            yield n_pb

    class P1(BaseProcessor):
        state = States.START
        next_state = States.FINISHED
        cancelled_state = None

        def process(self, ctx):
            return self.next_state

    class P2(BaseProcessor):
        state = States.CANCELLING
        next_state = States.CANCELLED
        cancelled_state = None

        def process(self, ctx):
            return self.next_state

    runner = StateRunner(
        entity_class=E,
        processing_interval=0.0,
        processors=[P1, P2],
        initial_state=States.START,
        final_state=States.FINISHED,
        final_cancelled_state=States.CANCELLED)

    ctx.log.info('1. Normal run')
    runner.process(ctx, n_pb)
    assert n_pb.order.progress.state.id == 'START'
    runner.process(ctx, n_pb)
    assert n_pb.order.progress.state.id == 'FINISHED'

    ctx.log.info('2. Force-cancelled order')
    n_pb = model_pb2.Namespace()
    n_pb.meta.id = 'ns'
    runner.process(ctx, n_pb)
    assert n_pb.order.progress.state.id == 'START'
    cancel_order(n_pb, 'author', 'comment', forced=True)
    runner.process(ctx, n_pb)
    assert n_pb.order.progress.state.id == 'CANCELLED'


def test_error_logging(ctx, caplog):
    b_pb = model_pb2.Namespace()
    b_pb.meta.id = 'ns'

    class States(enum.Enum):
        START = 1
        FINISHED = 2
        CANCELLED = 3
        UNICODE = 4
        HTTP = 5

    class E(WithOrder):
        name = 'E'
        states = States

        def zk_update(self):
            yield b_pb

    class P1(BaseProcessor):
        state = States.START
        next_state = States.UNICODE
        cancelled_state = None

        def process(self, _):
            raise RuntimeError

    class P2(BaseProcessor):
        state = States.UNICODE
        next_state = States.HTTP
        cancelled_state = None

        def process(self, _):
            raise RuntimeError('юникод')

    class Response(object):
        def __init__(self, content):
            self.content = content

    class HTTPError(Exception):
        def __init__(self, message):
            super(HTTPError, self).__init__(message)
            self.response = Response('контент')

    class P3(BaseProcessor):
        state = States.HTTP
        next_state = States.FINISHED
        cancelled_state = None

        def process(self, _):
            raise HTTPError('ошибка')

    runner = StateRunner(
        entity_class=E,
        processing_interval=0.0,
        processors=[P1, P2, P3],
        initial_state=States.START,
        final_state=States.FINISHED,
        final_cancelled_state=States.CANCELLED)

    runner.process(ctx, b_pb)  # assigned initial state

    with check_log(caplog) as log:
        with pytest.raises(RuntimeError):
            runner.process(ctx, b_pb)
        assert u'Unexpected exception while running processor for START: RuntimeError' in log.records_text()

    P1.process = lambda _, __: States.UNICODE
    with check_log(caplog) as log:
        runner.process(ctx, b_pb)
        assert u'RuntimeError' not in log.records_text()

    with check_log(caplog) as log:
        with pytest.raises(RuntimeError):
            runner.process(ctx, b_pb)
        assert u'Unexpected exception while running processor for UNICODE: RuntimeError: юникод' in log.records_text()

    P2.process = lambda _, __: States.HTTP
    runner.process(ctx, b_pb)

    with check_log(caplog) as log:
        with pytest.raises(HTTPError):
            runner.process(ctx, b_pb)
        assert u'Unexpected exception while running processor for HTTP: ' \
               u'HTTPError: ошибка; Reason: контент' in log.records_text()
