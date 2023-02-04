from bcl.banks.registry import IngUa


def test_creator_domestic(get_assoc_acc_curr, get_payment_bundle, get_source_payment):

    get_assoc_acc_curr(IngUa.id, account={'number': '40702810800000007671', 'currency_code': 'UAH'})  # Понадобится объект счёта.

    creator = IngUa.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment()]))
    compiled = creator.create_bundle()

    assert ':LCY' in compiled


def test_creator_foreign(get_assoc_acc_curr, get_payment_bundle, get_source_payment):

    acc = '40702643800000007672'
    get_assoc_acc_curr(IngUa.id, account=acc)

    creator = IngUa.payment_dispatcher.get_creator(get_payment_bundle([get_source_payment({
        'f_acc': acc,
    })]))
    compiled = creator.create_bundle()

    assert ':FCY' in compiled
