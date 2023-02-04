from bcl.banks.registry import Ibaz


def test_payment_creator(build_payment_bundle):

    associate = Ibaz

    bundle = build_payment_bundle(
        associate,
        payment_dicts=[
            {'f_iban': 'AZ62İBAZ38010019449111111120', 't_bic': '010305'},
        ],
        account={'number': 'one'})

    assert (
        ',38010019449111111120,120,RUB,152.00,RUB,A,'
        f'0000000000000002,"ООО ""Кинопортал""",7725713770,010305,Назначение[{bundle.payments[0].number}],,,,XOHKS\r\n'
    ) in bundle.tst_compiled

    bundle = build_payment_bundle(
        associate,
        payment_dicts=[
            {'ground': 'Длинное назначение, которое никак не поместится '
                       'в четыре строки по тридцать пять символов в каждой и будет непременно обрезано, '
                       'как того и требует формат.', 't_bic': ''},
        ],
        account={'number': 'one'})

    assert (
        ',0000000000000001,001,RUB,152.00,RUB,A,'
        '0000000000000002,"ООО ""Кинопортал""",7725713770,,"Длинное назначение, которое никак н",'
        'е поместится в четыре строки по три,дцать пять символов в каждой и буде,'
        f'"т непременно обрезано, как того [{bundle.payments[0].number}]",XOHKS\r\n'
    ) in bundle.tst_compiled

    bundle = build_payment_bundle(
        associate,
        payment_dicts=[
            {'f_iban': 'AZ62İBAZ38010019449111111120', 't_swiftcode': 'IBAZXXXX'},
        ],
        account={'number': 'one'})

    assert (
        f',38010019449111111120,120,RUB,152.00,A,0000000000000002,002,RUB,Назначение[{bundle.payments[0].number}]\r\n'
    ) in bundle.tst_compiled
