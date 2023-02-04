import json

from bcl.banks.registry import Tinkoff
from bcl.core.models import states, Payment
from bcl.toolbox.utils import DateUtils


def test_faked_statement(fake_statements, get_source_payment, get_assoc_acc_curr, init_user):

    init_user(robot=True)

    associate = Tinkoff

    _, account2, _ = get_assoc_acc_curr(associate, account='123')
    account2.fake_statement = True
    account2.save()

    get_source_payment({
        'f_acc': account2.number,
        'status': states.COMPLETE,
        'dt': DateUtils.yesterday(),
    }, associate=associate)

    statements = fake_statements(associate, account='456')

    assert len(statements) == 1

    data = json.loads(statements[0].zip_raw.decode('utf-8'))

    # Проверяем, что проиходит фильтрация по нужному статусу.
    assert Payment.objects.filter(status=associate.statement_downloader_faked.status_complete).count() == 3
    assert len(data['items']) == 2
    assert 2 >= len(data['items'][0]['payments']) >= 1
    assert 2 >= len(data['items'][1]['payments']) >= 1

    parser = associate.statement_dispatcher.get_parser(statements[0])
    result = parser.process()

    assert len(result) == 2

    register, payments = result[0]
    assert not register.intraday
    assert register.is_valid
    assert len(payments) in {1, 2}

    register, payments = result[1]
    assert not register.intraday
    assert register.is_valid
    assert len(payments) in {1, 2}
