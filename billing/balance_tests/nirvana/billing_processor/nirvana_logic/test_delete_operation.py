def test_delete_operation_returns_204_and_set_terminate_1_and_idempotent(logic, nirvana_block):
    for _ in range(2):  # check idempotency
        res = logic.delete_operation('some_operation', 'some_instance', nirvana_block.id)
        assert res.status == 204
        assert res.body == ''
        assert nirvana_block.terminate == 1


def test_delete_operation_returns_400_if_ticket_not_found(logic):
    resp = logic.delete_operation('some_operation', 'some_instance', '-1')
    assert resp.status == 400
    assert resp.reason == 'Unknown ticket: -1'
