from datetime import date

from bcl.banks.registry import Sber
from bcl.core.cleaner import Cleaner
from bcl.core.models import Statement, StatementPayment, StatementRegister


def test_cleanup(run_task, get_assoc_acc_curr, get_source_payment, mock_mds, parse_statement_fixture, time_freeze):

    associate = Sber

    def generate_statement():
        register, payments = parse_statement_fixture(
            'sber_intraday_cleanup.txt',
            associate=associate,
            encoding=associate.statement_dispatcher.parsers[0].encoding,
            acc='40702840500091003838', curr='RUB'
        )[0]
        return register.statement

    with time_freeze('2018-07-11'):
        statement_1 = generate_statement()
        assert statement_1.raw.startswith(b'PK')
        assert not statement_1.mds_path
        assert statement_1.update_dt.date() == date(2018, 7, 11)

    with time_freeze('2021-06-01'):
        statement_2 = generate_statement()
        assert not statement_2.mds_path
        assert statement_2.raw.startswith(b'PK')

        assert Statement.objects.count() == 2
        assert StatementRegister.objects.count() == 2
        assert StatementPayment.objects.count() == 2

        cleaner = Cleaner('')
        cleaner.run()

    assert cleaner.mds.client.log == [
        "upload bcl.statements/1 {'Metadata': {'id': '1', 'dt': '2021-06-01 00:00:00'}}"
    ]

    assert Statement.objects.count() == 2
    assert StatementRegister.objects.count() == 1
    assert StatementPayment.objects.count() == 1

    statement_1.refresh_from_db()
    assert statement_1.mds_path == f':bcl:statements/{statement_1.id}'
    assert statement_1.raw == b''

    statement_2.refresh_from_db()
    assert not statement_2.mds_path
    assert statement_2.raw.startswith(b'PK')

    # то же, через фоновое задание
    result = run_task('cleanup')
    assert result == '{}'
