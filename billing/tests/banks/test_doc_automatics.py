from bcl.banks.common.document.automator import DocumentAutomator
from bcl.banks.registry import Ing
from bcl.banks.party_ing import IngRuSftpConnector
from bcl.core.models import Svo, SvoItem, Spd, Letter, DocRegistryRecord, Payment, states, Prove


def test_autocreation(
    build_payment_bundle, time_freeze, init_contract, get_source_payment, init_user, init_doc_prove,
    get_assoc_acc_curr,
):
    associate, acc, _ = get_assoc_acc_curr(Ing, account='40702810800000007671')
    user = init_user()

    attrs_base = {
        't_resident': '0',
        'account_id': acc.id,
    }
    attrs_contract = {
        **attrs_base,
        'prop': 2,
        'oper_code': '10100',

        'contract_num': 'zzzz-xx-yy',
        'contract_dt': '2021-10-13',
        'contract_currency_id': 643,
        'contract_sum': 15.66,
        'advance_return_dt': '2021-10-14',
        'expected_dt': '2021-10-15',
    }
    contract = init_contract('12345678/1234/1234/1/1')  # этот контракт используется в первом платеже

    for_letter_pay_1 = get_source_payment(
        {**attrs_base, 'ground': 'for_letter_1', 'trans_pass': ''}, associate=associate)
    for_letter_pay_2 = get_source_payment(
        {**attrs_base, 'ground': 'for_letter_2', 'trans_pass': ''}, associate=associate)
    for_letter_prove = init_doc_prove(payments=[for_letter_pay_1, for_letter_pay_2], number='for_letter')

    # Для проверки случая существования СВО.
    for_svo_payment_existing = get_source_payment(
        {**attrs_base, 'ground': 'for_svo_existing'},
        associate=associate)

    svo_prove = init_doc_prove(payments=[for_svo_payment_existing], number='for_svo_existing')
    svo = Svo.objects.create(associate_id=associate.id, user=user, contract=contract)
    svo.set_items([
        SvoItem(
            prove=svo_prove,
            payment=for_svo_payment_existing,
        ),
    ])

    with time_freeze('2020-04-03'):
        bundles = build_payment_bundle(associate=associate, payment_dicts=[
            {
                # рублёвый с паспортом - в отдельный пакет
                **attrs_base,
                'account_id': acc.id,
            },
            {
                # валютный с паспортом
                **attrs_contract,
                'ground': 'myground',
                'currency_id': 840,
                'trans_pass': '123x456y789',
            },
            {
                # рублевый без паспорта
                **attrs_contract,
                'trans_pass': '',
            },
            for_svo_payment_existing,
            for_letter_pay_1,

        ], h2h=True)

        payments = list(Payment.objects.all().order_by('id'))
        assert len(payments) == 6

        for idx in (5,):
            # невалидные
            assert payments[idx].ground == 'Назначение', idx
            assert payments[idx].processing_notes == 'Не найдены документы, необходимые для платежа.', idx
            assert payments[idx].is_invalidated, idx

        for idx in (0, 1, 2, 3, 4):
            # валидные
            grounds = {
                0: 'for_letter_1',
                1: 'for_letter_2',
                2: 'for_svo_existing',
                3: 'Назначение',
                4: 'myground',
            }
            assert payments[idx].ground == grounds[idx], idx
            assert payments[idx].processing_notes == '', idx
            assert not payments[idx].is_invalidated, idx

        bundle = bundles[0]

        payments = list(bundle.payments.order_by('id'))
        assert len(payments) == 2

        # создался и прописался контракт
        contract_new = payments[1].contract
        assert contract_new.unumber == '123x456y789'
        assert contract_new.payment_set.count() == 1

        # к старому контракту привязаны два платежа: payment_svo и новый
        assert contract.payment_set.count() == 2

        records = list(DocRegistryRecord.objects.all().order_by('id'))

        assert len(records) == 5
        for idx in (0, 1, 3):
            assert records[idx].type_alias == 'svo', idx

        for idx in (2,):
            assert records[idx].type_alias == 'letter', idx

        for idx in (4,):
            assert records[idx].type_alias == 'spd', idx

        letters = list(Letter.objects.all().order_by('id'))
        assert len(letters) == 1

        spds = list(Spd.objects.all().order_by('id'))
        assert len(spds) == 1

        svos = list(Svo.objects.all().order_by('id'))
        assert len(svos) == 4
        assert svos[0] == svo

        svo_new = svos[2]
        assert svo_new.associate_id == associate.id
        assert svo_new.account_id == acc.id
        assert svo_new.contract_id == contract_new.id
        svo_items = list(svo_new.proves.all())
        assert len(svo_items) == 1

        svo_prove = svo_items[0]
        assert svo_prove.associate_id == associate.id
        assert svo_prove.account_id == acc.id
        assert svo_prove.contract_id == contract_new.id
        assert svo_prove.currency_id == 840
        assert svo_prove.contract_num == 'zzzz-xx-yy'


def test_automator_sent_unsent(
    get_assoc_acc_curr, init_user, init_doc_prove, django_assert_num_queries, init_contract, get_source_payment):

    associate, acc, _ = get_assoc_acc_curr(Ing, account='40702810800000007671')
    contract = init_contract('12345678/1234/1234/1/1')
    user = init_user()

    payment_1 = get_source_payment(
        {'t_resident': '0', 'account_id': acc.id},
        associate=associate)

    svo_prove_1 = init_doc_prove(payments=[payment_1], number='for_svo_existing')

    def get_svo(status, prove):
        svo = Svo.objects.create(associate_id=associate.id, user=user, contract=contract, status=status)
        svo.set_items([SvoItem(prove=prove, payment=payment_1)])
        return svo

    svo_1 = get_svo(states.NEW, prove=svo_prove_1)

    automator = DocumentAutomator([payment_1])

    # Собираем в пакет в первый раз. Формируем СВО и СПД налету.
    with django_assert_num_queries(28) as _:
        drop = automator.run()
        assert not drop

    # Инитируем отправку СВО
    svo_1.status = states.IN_DELIVERY
    svo_1.save()

    # Проверяем исключение платежа из пакета.
    with django_assert_num_queries(19) as _:
        drop = automator.run()
        assert len(drop) == 1
        assert drop[0].processing_notes == f'Документы к платежу уже отправлены: ID {svo_prove_1.id}.'
    svo_prove_autocreated = Prove.objects.last()
    assert svo_prove_autocreated.status == states.BUNDLED

    # Имитируем вариант с созданием нового СВО для того же платежа и ПД.
    svo_prove_2 = init_doc_prove(payments=[payment_1], number='for_svo_prve_2')
    svo_2 = get_svo(states.NEW, prove=svo_prove_2)

    # Проверяем исключение платежа из пакета из-за неотправленных.
    with django_assert_num_queries(17) as _:
        drop = automator.run()
        assert len(drop) == 1

    notes = drop[0].processing_notes
    assert 'Документы к платежу уже отправлены:' in notes
    assert f'Документы ещё не отправлялись' in notes

    # Проверяем возможность игнорирования необходимости документов.
    payment_1.refresh_from_db()
    assert payment_1.is_invalidated  # предыдущий статус меняется
    assert payment_1.invalidation == Payment.INV_DOCS_REQ
    payment_1.status = states.NEW
    payment_1.invalidation = Payment.INV_DOCS_IGNORE
    payment_1.save()
    drop = automator.run()
    assert not drop
    payment_1.refresh_from_db()
    assert payment_1.is_new  # предыдущий статус сохраняется.


def test_automator_boostrap_contracts(django_assert_num_queries, init_contract, get_source_payment):
    associate = Ing

    contract = init_contract('12345678/1234/1234/1/1')

    # известный контракт прописан только строкой
    payment_1 = get_source_payment(associate=associate)
    # известный контракт и строкой, и внешним ключом
    payment_2 = get_source_payment({'contract_id': contract.id}, associate=associate)
    # неизвестный контракт прописан строкой
    payment_3 = get_source_payment({'trans_pass': '5656'}, associate=associate)

    payments = [
        payment_1,
        payment_2,
        payment_3,
    ]

    with django_assert_num_queries(4) as _:
        result = DocumentAutomator.boostrap_contracts(payments)
        assert len(result) == 2
        assert result.pop(contract.unumber) == contract.id
        assert result.pop('5656') > contract.id

    for payment in payments:
        payment.refresh_from_db()
        assert payment.contract


def test_spd_validation_on_send(
    get_assoc_acc_curr, init_user, get_source_payment, init_doc_prove, init_contract, mock_gpg,
    monkeypatch, sftp_client
):
    client = sftp_client()

    user = init_user()
    contract = init_contract('12345678/1234/1234/1/1')
    associate, acc, _ = get_assoc_acc_curr(Ing, account='40702810800000007671')

    monkeypatch.setattr(IngRuSftpConnector, 'get_client', lambda *args, **kwargs: client)

    prove_1 = init_doc_prove(user=user, associate=associate, account=acc, payments=[{'account': acc}])
    payment_1 = prove_1.payments.first()

    # Создадим новую СПД и успешно отправим.
    spd, rec_spd = DocumentAutomator.init_spd(payment=payment_1, contract_id=contract.id, user=user, proves=[prove_1])
    rec_spd.schedule()
    rec_spd.refresh_from_db()
    assert rec_spd.is_processing_ready
    associate.automate_documents(rec_spd)

    def refresh(rec_spd):
        rec_spd.refresh_from_db()
        prove_1.refresh_from_db()
        spd.refresh_from_db()

    refresh(rec_spd)
    assert not rec_spd.latest_exception
    assert rec_spd.status == rec_spd.state_processing_done
    assert prove_1.is_exported_h2h
    assert spd.is_exported_h2h
    assert len(client.files_contents) == 1

    # А теперь попробуем отправить новую СПД с тем же ПД.
    # Такого происходить не должно.
    spd_2, rec_spd_2 = DocumentAutomator.init_spd(
        payment=payment_1, contract_id=contract.id, user=user, proves=[prove_1])
    rec_spd_2.schedule()
    associate.automate_documents(rec_spd_2)

    refresh(rec_spd_2)
    assert rec_spd_2.is_error
    assert 'уже были отправлены ранее' in rec_spd_2.processing_notes
    assert prove_1.is_exported_h2h
    assert spd_2.is_error
    assert len(client.files_contents) == 1
