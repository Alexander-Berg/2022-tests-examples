import json

from balance.mapper import NirvanaBlock, Export


def test_put_operation_adds_object_into_queue_idempotent(logic, session):
    tickets = []
    for _ in range(2):  # check idempotency
        resp = logic.put_operation('some_operation', 'some_instance', '"some request"')
        body = json.loads(resp.body)
        ticket = body['ticket']
        tickets.append(ticket)
        nb = session.query(NirvanaBlock).getone(ticket)
        assert nb
        assert nb.id == int(ticket)
        assert nb.request == 'some request'
        assert nb.operation == 'some_operation'
        assert nb.instance_id == 'some_instance'
        export = session.query(Export).getone(type='NIRVANA_BLOCK', classname='NirvanaBlock', object_id=int(ticket))
        assert export

    assert tickets[0] == tickets[1]


def test_put_operation_returns_200_and_start_response(logic, session):
    resp = logic.put_operation('some_operation', 'some_instance', '"some request"')
    body = json.loads(resp.body)
    assert resp.status == 200
    assert body['ticket']


def test_put_operation_returns_different_ticket_for_different_request(logic, session):
    resp = logic.put_operation('some_operation', 'some_instance', '"some request"')
    body = json.loads(resp.body)
    ticket_1 = body['ticket']
    resp = logic.put_operation('some_operation', 'another_instance', '"some request"')
    body = json.loads(resp.body)
    ticket_2 = body['ticket']
    assert ticket_1 != ticket_2
