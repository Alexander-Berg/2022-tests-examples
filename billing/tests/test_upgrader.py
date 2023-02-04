import pytest

from bcl.banks.registry import Sber


@pytest.mark.skip('Проверяет фоновую миграцию по BCL-2094. Отключена до лучших времён')
def test_upgrade(run_task, get_assoc_acc_curr, get_source_payment):

    associate = Sber

    _, acc1, _ = get_assoc_acc_curr(associate, account='1')
    _, acc00, _ = get_assoc_acc_curr(associate, account='00')
    _, acc2, _ = get_assoc_acc_curr(associate, account='2')

    pay1 = get_source_payment({'f_acc': acc1.number}, associate=associate)
    pay2 = get_source_payment({'f_acc': acc2.number}, associate=associate)
    assert not pay1.account_id
    assert not pay2.account_id

    result = run_task('background_upgrade')
    assert result == '{"acc_id": %s, "done": false}' % acc2.id

    result = run_task('background_upgrade', latest_result=result)
    assert result == '{"acc_id": %s, "done": true}' % acc2.id

    pay1.refresh_from_db()
    pay2.refresh_from_db()

    assert pay1.account == acc1
    assert pay2.account == acc2
