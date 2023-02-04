from datetime import datetime

import pytest
from freezegun import freeze_time

from bcl.banks.party_ing.document_parsers import Auth27Parser
from bcl.banks.party_ing.common import IngRuSftpConnector
from bcl.banks.registry import Ing
from bcl.core.models import states, Currency, DocRegistryRecord
from bcl.exceptions import ValidationError


def test_bundling_currency(build_payment_bundle, time_freeze):

    associate = Ing

    with pytest.raises(ValidationError):
        # Нет поддержки валютных платежей.
        build_payment_bundle(associate, payment_dicts=[{'currency_id': Currency.USD}], h2h=True)


def test_bundling(build_payment_bundle, time_freeze):

    associate = Ing

    oper_code = 21300

    bundle = build_payment_bundle(associate, h2h=True, payment_dicts=[
        {
            'oper_code': oper_code,
            'ground': '{VO21200} Назначение платежа'
        }
    ])
    compiled = bundle.tst_compiled

    org = bundle.account.org
    org.connection_id = 'yatech'
    org.save()

    assert '<Nm>OOO Яндекс</Nm>' in compiled
    assert 'pain.001.001.03' in compiled
    assert 'SplmtryData' not in compiled
    assert f'{{VO{oper_code}}} VO' in compiled

    payment_sender = associate.payment_sender(bundle)
    assert payment_sender.settings_alias == 'ya'

    with time_freeze('2020-04-03'):
        fname = associate.connector_dispatcher.get_connector(caller=payment_sender).bundle_filename_generate()
        assert fname == f'9804305001.20200403.{bundle.number}.PRUCRU.P'

    associate.payment_sender.validate_contents(bundle.tst_compiled)


def test_statements_files_naming(time_freeze):

    associate = Ing

    with time_freeze('2020-04-03'):
        func_filter = associate.statement_downloader(final=True).filename_get_filter()

        assert list(map(func_filter, (
            's9914368.ING.180106.15h30.CAMT05200102.DPA01.S.201801067317098.P.NL.xml',
            f'sXXXXXXX.{associate.our_remote_id}.20200403.REFERENCE.C52PRU.P',

        ))) == [False, True]


def test_status_files_naming(time_freeze):

    associate = Ing

    with time_freeze('2020-04-03'):

        func_filter = associate.connector_dispatcher.get_connector(
            caller=associate.payment_synchronizer()
        ).status_filename_get_filter()

        assert list(map(func_filter, (
            'FTPA1202.1543305381323.00000001163810.PAIN002.P',
            'sXXXXXXX.FTPID.20200403.1234567.OSRPRU.P',

        ))) == [False, True]


def test_statuses(read_fixture, get_payment_bundle):
    associate = Ing

    bundle = get_payment_bundle([{'number': '12', 'status': states.EXPORTED_H2H}], associate=associate, id=5783793)

    xml = read_fixture('ing_ru_002.xml', decode='utf-8')

    syncer = associate.payment_synchronizer()
    status = syncer.parse_statuses_xml(xml)
    syncer.update_payments(status)

    payment = bundle.payments[0]
    assert payment.status == states.PROCESSING


def test_statement_final(get_assoc_acc_curr, parse_statement_fixture):

    associate = Ing

    results = parse_statement_fixture('ing_ru_053.xml', associate, [
        '40702810300001003838',
    ], 'RUB')

    assert len(results) == 1

    register, pays = results[0]
    assert register.is_valid
    assert len(pays) == 1
    assert pays[0].number == '12774'
    assert pays[0].is_out


def test_statement_intraday(get_assoc_acc_curr, parse_statement_fixture):

    associate = Ing

    results = parse_statement_fixture('ing_ru_052.xml', associate, [
        '40702810300001003838',
    ], 'RUB')

    assert len(results) == 1

    register, pays = results[0]
    assert register.is_valid
    assert len(pays) == 0  # Выписка содержит только наши списания, которые мы пропускаем.


def test_doc_tasks(run_task, sftp_client, mock_gpg_decrypt, monkeypatch, read_fixture, get_document, init_user):
    associate = Ing
    user = init_user(robot=True)

    client = sftp_client(files_contents={
        'out/s0000001.FTPID.20200810.REFERENCE.XMDPRU.P': read_fixture('ing_auth26.xml'),
        'out/OSRPRU/s0000002.FTPID.20200810.REFERENCE.OSRPRU.P': read_fixture('ing_auth27.xml'),
    })
    monkeypatch.setattr('bcl.banks.party_ing.common.IngRuSftpConnector.get_client', lambda alias: client)

    with freeze_time(datetime(2020, 8, 10)):

        # скачиваем докуемнт от банка
        doc_out = get_document(associate=associate, user=user, id=7)
        run_task('ing_auto_doc_download')
        doc_out.refresh_from_db()
        assert doc_out.is_processing_done

        # принимаем статусный файл
        doc_out = get_document(associate=associate, id=11)
        run_task('ing_auto_doc_sync')
        doc_out.refresh_from_db()
        assert doc_out.is_complete


def test_auth26_parser(read_fixture, get_document, init_user):

    user = init_user(robot=True)
    associate = Ing
    xml = read_fixture('ing_auth26.xml')
    dt = datetime(2020, 8, 10)

    doc_out = get_document(associate=associate, user=user, id=7)

    def get_documents(filter_func=None, path='', date_expected=None):
        for f_name, content in (('new_file', xml),):
            yield 'new_file', content

    downloader = Ing.document_downloader(on_date=dt)
    downloader.get_documents = get_documents
    downloader_result = downloader.run()
    assert len(downloader_result) == 1

    docs = list(DocRegistryRecord.objects.all().order_by('id'))
    assert len(docs) == 2
    assert docs[0].id == doc_out.id

    doc_in = docs[1]
    assert doc_in.attachments.count() == 1
    assert not doc_in.is_dir_out
    assert doc_in.is_new  # статус базовой записи не меняется
    assert doc_in.file_hash == '1cbcd0591e3a2b5d57ab422342fa115f8a709feaa70429379abc3af4'

    doc_out.refresh_from_db()
    assert doc_out.is_processing_done

    # Повторно тот же файл не импортируем.
    downloader_result = downloader.run()
    assert len(downloader_result) == 0


def test_auth27_parser(read_fixture, get_document):
    associate = Ing
    xml = read_fixture('ing_auth27.xml', decode='utf-8')

    parser = Auth27Parser()
    parser.associate = associate

    assert Auth27Parser.can_parse(xml)
    assert parser.parse(xml) == {'message_id': 'auth.024ING-11', 'document_status': 'ACPT'}

    doc = parser.run(xml, 'myfilename.xml')
    # нет исходящго документа
    assert doc is None

    doc_out = get_document(associate=associate, id=11)
    doc_result = parser.run(xml, 'myfilename.xml')

    assert doc_result == doc_out
    assert doc_result.is_complete


def test_auth24_creator(read_fixture, get_document, get_payment_bundle, mock_gpg):
    """Проверка формирования auth.024 документа"""
    associate = Ing

    doc = get_document(
        associate=associate, id=10, generate='svo',
        payments=[{'f_name': 'some'}],
        attachments={'one.txt': b'data1', 'two.txt': b'data2'},
    )
    prove = doc.dyn_prove
    org = prove.account.org
    org.inn = '6754987'
    org.save()

    bundle = get_payment_bundle(prove.dyn_payments, associate=associate)

    def put_patched(connector, link, f_name, contents):
        assert f_name == doc.file_name

    with freeze_time(datetime(2020, 8, 10)):
        doc.refresh_from_db()
        IngRuSftpConnector.put = put_patched
        Ing.automate_documents(doc)

    doc.refresh_from_db()

    content = doc.content.decode()
    assert '<MsgId>auth.024ING-10</MsgId>' in content
    assert '<Othr><Id>6754987</Id>' in content
    assert '<Nm>fakedorg</Nm>' in content
    assert '<MmbId>044525222</MmbId>' in content
    assert '<DtOfIsse>2021-10-02</DtOfIsse>' in content
    assert '<Amt Ccy="RUB">152.00</Amt>' in content
    assert '<AddtlInf>somenote</AddtlInf>' in content
    assert '<URL>two.txt</URL>' in content
    assert '<InclBinryObjct>ZGF0YTI=</InclBinryObjct>' in content
    assert f'<Id><MsgId>{bundle.number}</MsgId>' in content


def test_auth25_creator(read_fixture, get_document, mock_gpg):
    """Проверка формирования auth.025 документа"""
    associate = Ing

    spd_record = get_document(
        associate=associate, id=10, generate='spd',
        payments=[{'f_name': 'some', 'f_inn': '6754987'}],
        attachments={'one.txt': b'data1', 'two.txt': b'data2'},
    )
    prove = spd_record.dyn_prove
    org = prove.account.org
    org.inn = '6754987'
    org.save()

    def put_patched(connector, link, f_name, contents):
        assert f_name == spd_record.file_name

    with freeze_time(datetime(2020, 8, 10)):
        spd_record.refresh_from_db()
        IngRuSftpConnector.put = put_patched
        Ing.automate_documents(spd_record)

    spd_record.refresh_from_db()

    content = spd_record.content.decode()
    assert '<MsgId>auth.025ING-10</MsgId>' in content
    assert '<Othr><Id>6754987</Id>' in content
    assert '<Nm>fakedorg</Nm>' in content
    assert '<MmbId>044525222</MmbId>' in content
    assert f'<RegdCtrctId>{prove.contract.unumber}' in content
    assert '<DtOfIsse>2021-10-02</DtOfIsse>' in content
    assert '<OrgnlDoc><Id>prove12</Id><DtOfIsse>2020-10-13<' in content
    assert '<DocTp>2211</DocTp>' in content
    assert '<TtlAmt Ccy="RUB">134.50</TtlAmt><TtlAmtInCtrctCcy Ccy="RUB">431.50</TtlAmtInCtrctCcy>' in content
    assert '<Conds><Prtry>77</Prtry>' in content
    assert '<XpctdDt>2020-10-14<' in content
    assert '<URL>two.txt</URL>' in content
    assert '<InclBinryObjct>ZGF0YTI=</InclBinryObjct>' in content
    assert '<NtryId>888<' in content
    assert '<OrgnlDocDt>2021-10-12<' in content


def test_auth26_creator(read_fixture, get_document, mock_gpg):
    """Проверка формирования auth.026 документа"""
    associate = Ing

    doc = get_document(
        associate=associate, id=10, generate='letter_prove',
        payments=[{'f_name': 'some'}],
        attachments={'one.txt': b'data1', 'two.txt': b'data2'},
    )
    prove = doc.dyn_prove
    org = prove.account.org
    org.inn = '6754987'
    org.save()

    def put_patched(connector, link, f_name, contents):
        assert f_name == doc.file_name

    with freeze_time(datetime(2020, 8, 10)):
        doc.refresh_from_db()
        IngRuSftpConnector.put = put_patched
        Ing.automate_documents(doc)

    doc.refresh_from_db()

    content = doc.content.decode()
    assert '<MsgId>auth.026ING-10</MsgId>' in content
    assert '<Othr><Id>6754987</Id>' in content
    assert '<Nm>fakedorg</Nm>' in content
    assert '<MmbId>044525222</MmbId>' in content
    assert '<Sbjt>a-band-new-letter<' in content
    assert '<Tp>CCSD</' in content
    assert '<Desc>this is for you<' in content
    assert '<URL>two.txt</URL>' in content
    assert '<InclBinryObjct>ZGF0YTI=</InclBinryObjct>' in content


def test_file_filters():

    date_expected = datetime(2021, 3, 3).date()

    sync_filter = Ing.document_synchronizer().doc_status_filter(date_expected=date_expected)

    filename = 's8177048.9804305001.20210303.00493582SFTP.OSRPRU.P'

    assert sync_filter(filename)

    filename = 's8177048.9804305001.20210303.00493582SFTP.XMDPRU.P'

    download_filter = Ing.document_downloader(date_expected).get_doc_filter()

    assert download_filter(filename)
