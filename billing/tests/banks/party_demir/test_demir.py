from decimal import Decimal

import pytest

from bcl.banks.registry import Demir
from bcl.exceptions import ValidationError


def test_payment_creator(get_payment_bundle, get_source_payment, get_assoc_acc_curr):
    """
41000073442014	2022-03-02	28918099	210201410231	1408183300Ïî	118005	Ïðåäñòàâèòåëüñòâî Àêöèîíåðíîãî îáùåñòâà Â Êûðãûçñêîé Ðåñïóáëèêå	1181000300012973	CJSC Demir Kyrgyz International Bank	FARMAMIR - LTD	000000000000001091820183520119	00000000000000000000	00000000000000109017	OJSC UniCredit Bank	Òðèñòà ïÿòüäåñÿò Íîëü	1000001.00	00000000000014153100	KGS	Çà êîíñóëüòàöèîííûå óñëóãè
41000073442014	2022-03-02	28918099	210201410231	1408183300Ïî	118005	Ïðåäñòàâèòåëüñòâî Àêöèîíåðíîãî îáùåñòâà Â Êûðãûçñêîé Ðåñïóáëèêå	1181000300012973	CJSC Demir Kyrgyz International Bank	FARMAMIR LTD	000000000000001091820183520119	00000000000000000000	00000000000000109017	OJSC UniCredit Bank	Òðèñòà ïÿòüäåñÿò Íîëü	350.00	00000000000014153100	KGS	Çà êîíñóëüòàöèîííûå óñëóãè.,-

    """
    _, acc, _ = get_assoc_acc_curr(Demir, account='1234567')

    def build():
        compiled = Demir.payment_dispatcher.get_creator(get_payment_bundle([
            get_source_payment({
                'f_acc': '1234567',
            })
        ], account=acc)).create_bundle()
        return compiled

    with pytest.raises(ValidationError) as e:
        build()

    assert 'ОКПО|СФКР' in f'{e.value}'
    acc.org_remote_id = '233|566'
    acc.save()

    compiled = build()
    assert compiled.endswith(
        '233	7705713772	566	044525700	OOO Яндекс	1234567	АО БАНК	ООО Кинопортал	40702810301400002360'
        '		044525593	АО БАНК	сто пятьдесят два  ноль	152.00		RUB	Назначение		'
    )


def test_statement_parser(parse_statement_fixture):

    register, payments = parse_statement_fixture(
        'statement_demir.xls', Demir, '1180000203177016', 'RUB')[0]
    statement = register.statement

    assert statement.type == statement.TYPE_FINAL
    assert register.is_valid
    assert register.account.number == '1180000203177016'
    assert len(payments) == 10

    pay = payments[0]
    assert pay.is_in
    assert pay.number == 'l100'
    assert pay.summ == Decimal('50000')

    assert payments[1].number == '200'
