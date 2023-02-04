import datetime
from collections import namedtuple
from contextlib import contextmanager
from decimal import Decimal
from pathlib import Path

import pytest
from django.conf import settings

from bcl.exceptions import BclException, UserHandledException
from bcl.toolbox.client_sftp import SftpClient
from bcl.toolbox.signatures import GpgUtils
from bcl.toolbox.utils import (
    replace_by_map, Sanitizer, get_subitem,
    mark_string_symbols, XmlUtils, DecimalUtils, DateUtils, make_batches, ZipUtils,
    context_logging, send_email, CipherUtils, Swift, natsort_filenames,
    parse_recipients_string, partition, pluralize, humanize_size, download, get_env_split,
    handled_exceptions,
)


def test_envsplit():
    settings.ENVIRON['MY'] = 'a, b ,c,,'
    assert get_env_split('MY') == ['a', 'b', 'c']


def test_handled_exceptions():
    exc_bag = []

    with handled_exceptions(exc_bag):
        raise UserHandledException('one')

    with handled_exceptions(exc_bag):
        raise UserHandledException('two')

    assert exc_bag == ['one', 'two']


def test_settings_getter():

    settings.ENVIRON['BCL_DB__PASSWORD'] = 'dummy'
    user, password = settings.SECTION_SETTINGS_GETTER('DbSettings', None, ['User', 'Password'])
    assert user == 'postgres'  # из xml
    assert password == 'dummy'  # из окружения

    settings.ENVIRON['BCL_HTTP__ALFA_YA__PASSWORD'] = 'stub'
    login, password = settings.SECTION_SETTINGS_GETTER('HttpSetting', 'alfa_ya', ['Login', 'Password'])
    assert login == '643223'  # из xml
    assert password == 'stub'  # из окружения


def test_sftp_iter_entries(monkeypatch):

    SftpAttrs = namedtuple('SftpAttrs', ['filename', 'st_mtime', 'st_size', 'st_mode'])

    class Mocker:
        def listdir_iter(self, path):
            return [
                SftpAttrs('one.txt', 12345, 12345, 123),
                SftpAttrs('two.txt', None, 67890, 123),
            ]

    mocker = Mocker()

    @contextmanager
    def sftp_connection(self):
        yield mocker

    monkeypatch.setattr(SftpClient, 'sftp_connection', sftp_connection)

    client = SftpClient.configure('unicredit_ya')
    entries = list(client.iter_entries(Path('some/other')))

    assert len(entries) == 2
    entry = entries[0]
    assert entry.filename == 'one.txt'
    assert str(entry.fpath) == 'some/other/one.txt'
    assert not entry.is_dir
    assert str(entry.mtime) == '1970-01-01 06:25:45'
    assert entry.size == 12345


def test_humanize_size():
    assert humanize_size(0) == '0 B'
    assert humanize_size(102030) == '99.64 KB'
    assert humanize_size(102030405060) == '95.02 GB'


def test_download():
    result = download('filname.txt', b'123')
    assert result.content == b'123'
    assert result.status_code == 200
    assert result.serialize_headers() == (
        b'Content-Type: application/force-download\r\nContent-Disposition: attachment; filename=filname.txt')


def test_pluralize():
    forms = 'яблоко', 'яблока', 'яблок'
    assert pluralize(1, *forms) == '1 яблоко'
    assert pluralize(2, *forms) == '2 яблока'
    assert pluralize(11, *forms) == '11 яблок'


def test_account_notify_balance_to_parsed():
    recipients = parse_recipients_string(' one@at.com, two@at.com,three@at.com, ')
    assert recipients == ['one@at.com', 'two@at.com', 'three@at.com']
    assert parse_recipients_string('') == []


def test_partition():

    items = {1: True, 2: False, 3: False, 4:False}

    result = partition(sorted(items.items(), key=lambda item: item[0]), lambda item: item[1])
    assert result[0] == [(1, True)]
    assert result[1] == [(2, False), (3, False), (4, False)]


def test_natsort_filenames():

    filenames = [
        'st_2018-09-07(2).txt',
        'st_2018-09-07(1).txt',
        'st_2018-09-07.txt',
        'st_2018-09-07(3).txt',
    ]

    assert natsort_filenames(filenames) == [
        'st_2018-09-07.txt',
        'st_2018-09-07(1).txt',
        'st_2018-09-07(2).txt',
        'st_2018-09-07(3).txt',
    ]


def test_cipher_utils():
    text = 'Мама мыла раму 😉'

    ciphered = CipherUtils.cipher_text(text)
    deciphered = CipherUtils.decipher_text(ciphered)

    assert deciphered == text


def test_gpg_utils(mock_gpg):
    dummy_communicate = mock_gpg
    dummy_communicate.return_value = b'bogusout', b''

    with pytest.raises(BclException):
        GpgUtils.sign('bogus', 'dummypass', 'somedata')

    signed = b'-----BEGIN PGP MESSAGE-----\nsigned'
    dummy_communicate.return_value = signed, ''
    assert GpgUtils.sign('bogus', 'dummypass', 'somedata') == signed

    assert GpgUtils.decrypt('bogus', 'dummypass', 'somedata') == signed


def test_zip_utils():
    contents = {'one': b'sometext', 'two': b'othertext'}

    packed = ZipUtils.pack(contents)
    assert packed[0] == 80  # начало zip

    unpacked = ZipUtils.unpack(packed)
    assert unpacked == contents


def test_make_batches():
    assert list(make_batches([1, 2, 3], 2)) == [[1, 2], [3]]
    assert list(make_batches([], 2)) == []
    assert list(make_batches([1], 2)) == [[1]]
    assert list(make_batches([1, 2], 2)) == [[1, 2]]


@pytest.mark.xfail(condition=settings.ARCADIA_RUN, reason='В тестах аркадийной сборки перебит Handler')
def test_context_logging(caplog):
    import logging

    logger = logging.getLogger(__name__)
    # Сделаем, чтобы собиралась отладочная информация из .debug().
    logger.manager.disable = 0

    # По умолчанию собираются только ошибки.
    with context_logging(logger, 'outer'):
        pass
    assert caplog.messages == []

    caplog.set_level(logging.DEBUG, logger=logger.name)

    with context_logging(logger, 'outer'):
        with context_logging(logger, '.inner', str):
            with context_logging(logger, '.innermost', str('A')):
                pass

    assert caplog.messages == [
        'outer started ...',
        'str.inner started ...',
        'str.innermost started ...',
        'str.innermost finished',
        'str.inner finished',
        'outer finished',
    ]

    caplog.clear()

    with context_logging(logger, 'outer'):
        try:
            with context_logging(logger, 'inner'):
                raise ValueError('no-no-no')

        except Exception:
            pass

    assert caplog.messages == [
        'outer started ...',
        'inner started ...',
        'inner finished with exception: ValueError',
        'outer finished',
    ]


def test_combine_datetime():
    date = datetime.datetime.strptime('2017-06-02', '%Y-%m-%d')
    time = datetime.datetime.strptime('11:07:08', '%H:%M:%S')
    assert str(DateUtils.combine_datetime(date, time)) == '2017-06-02 11:07:08'

    expected_time = datetime.datetime.today().strftime('%H:%M')

    assert str(DateUtils.combine_datetime(date, None)).startswith('2017-06-02 ' + expected_time)

    assert DateUtils.format_nice(datetime.datetime(2017, 8, 1, 16, 15), with_time=False) == '2017-08-01'
    assert DateUtils.format_nice(datetime.datetime(2017, 8, 1, 16, 15)) == '2017-08-01 16:15'

    assert DateUtils.format_nice(datetime.datetime(17, 8, 30, 15, 59), with_time=False) == '0017-08-30'
    assert DateUtils.format_nice(datetime.datetime(17, 8, 30, 15, 59)) == '0017-08-30 15:59'

    assert DateUtils.format_nice(datetime.datetime(2017, 8, 1, 16, 15), preset='ru') == '01.08.2017 16:15'


def test_dateutils_basics():
    from_date = datetime.datetime.now()

    assert '00:00:00' not in str(from_date)
    assert '00:00:00' in str(DateUtils.day_start(DateUtils.tomorrow(from_date)))


def test_decimal_utils():
    round_ = DecimalUtils.round

    assert round_(Decimal('123.1236')) == Decimal('123.12')
    assert round_(Decimal('123.1236'), places=0) == Decimal('123')
    assert round_(Decimal('123.1236'), places=3) == Decimal('123.124')
    assert round_(Decimal('123.12'), places=1) == Decimal('123.1')
    assert round_(Decimal('123.10'), places=1) == Decimal('123.1')
    assert round_(Decimal('123.1'), places=1) == Decimal('123.1')
    assert str(round_(Decimal('123.1'), places=2)) == '123.10'
    assert str(round_(Decimal('123.100000'), places=2)) == '123.10'

    round_ = DecimalUtils.round_floor

    assert round_(Decimal('123.1236')) == Decimal('123.12')
    assert round_(Decimal('123.1236'), places=0) == Decimal('123')
    assert round_(Decimal('123.1236'), places=3) == Decimal('123.123')
    assert round_(Decimal('123.12'), places=1) == Decimal('123.1')
    assert round_(Decimal('123.10'), places=1) == Decimal('123.1')
    assert round_(Decimal('123.1'), places=1) == Decimal('123.1')

    normalize = DecimalUtils.normalize
    assert str(normalize(Decimal('123.10000'))) == '123.10'
    assert str(normalize(Decimal('0.135'))) == '0.135'
    assert str(normalize(Decimal('17'))) == '17.00'
    assert str(normalize(Decimal('400000'))) == '400000.00'
    assert str(normalize(Decimal('400000.000000'))) == '400000.00'
    assert str(normalize(Decimal('0.05'))) == '0.05'
    assert str(normalize(Decimal('0.95'))) == '0.95'
    assert str(normalize(Decimal('0.9'))) == '0.90'
    assert str(normalize(Decimal('-123.10000'))) == '-123.10'


def test_xml_read():
    assert 'Generated by Standards Editor' in XmlUtils.read_file('pain.001.001.06.xsd')


def test_xml_validate():

    xml = '''<?xml version="1.0"?>
        <root>
            <dummy_int>20</dummy_int>
            <dummy_int2>20</dummy_int2>
        </root>
        '''

    xsd = '''<?xml version="1.0" encoding="UTF-8" ?>
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xs:element name="root">
          <xs:complexType>
             <xs:sequence>
                <xs:element name="dummy_int" type="xs:int"/>
                <xs:element name="dummy_int2" type="xs:int"/>
             </xs:sequence>
          </xs:complexType>
        </xs:element>
        </xs:schema>
        '''

    valid, errors = XmlUtils.validate(xml, xsd)

    assert valid
    assert not errors

    valid, errors = XmlUtils.validate(xml.replace('20', 'bug'), xsd)

    assert not valid
    assert len(errors) == 2
    assert "'bug' is not a valid value" in errors[0]
    assert "'bug' is not a valid value" in errors[1]


def test_xml_defatl():
    assert XmlUtils.defat('<a>     <b>data  some\n\n</b>  \n   </a>') == '<a><b>data  some\n\n</b></a>'


def test_xml_from_to__dict():
    src_dict = {}
    root = src_dict.setdefault('root', {})
    root['@attr1'] = '1'
    root['@attr2'] = '2'

    sub1 = {}
    sub1['@subattr3'] = '3'
    sub1['inner'] = '4'

    sub2 = {}
    sub2['@sub2attr5'] = '5'
    sub2['='] = '6'

    root['sub'] = [sub1, sub2]

    xml = XmlUtils.from_dict(src_dict)
    assert xml == '<root attr1="1" attr2="2"><sub subattr3="3"><inner>4</inner></sub><sub sub2attr5="5">6</sub></root>'

    xml = XmlUtils.from_dict(src_dict, namespace='a', namespace_map={'a': 'some', 'b': 'other'})
    assert xml == (
        '<a:root xmlns:a="some" xmlns:b="other" attr1="1" attr2="2">'
        '<a:sub subattr3="3"><a:inner>4</a:inner></a:sub>'
        '<a:sub sub2attr5="5">6</a:sub></a:root>')

    out_dict = XmlUtils.to_dict(xml)
    xml2 = XmlUtils.from_dict(out_dict, namespace='a', namespace_map={'a': 'some', 'b': 'other'})

    assert xml == xml2


def test_transliterate():
    assert Sanitizer.transliterate('мама myla раму')[0] == "MAMA 'MYLA 'RAMU"
    assert Sanitizer.transliterate('мама myla раму', force=True)[0] == "MAMA 'MYLA 'RAMU"
    assert Sanitizer.transliterate('make better')[0] == "make better"
    assert Sanitizer.transliterate('make better', force=True)[0] == "'MAKE BETTER"


def test_append_replace():
    append = Sanitizer.append_replace

    assert append('Some', '10', max_len=15) == 'Some10'
    assert append('Some', '10', max_len=5) == 'Som10'
    assert append('some', '123456', max_len=5) == '12345'


def test_str_resplit():

    assert Sanitizer.str_resplit('12345678901234\n123456789012345\n6789012345678', 10) == (
        '1234567890\r\n1234\r\n1234567890\r\n12345\r\n6789012345\r\n678')

    assert Sanitizer.str_resplit('12345678901234\n123456789012345\n6789012345678', 10, max_lines=3) == (
        '1234567890\r\n1234\r\n1234567890')

    assert Sanitizer.str_resplit('Yandex.Taxi B.V.\nSchiphol Boulevard 165, 1118 BG Schiphol', 35, max_lines=4) == (
        'Yandex.Taxi B.V.\r\nSchiphol Boulevard 165, 1118 BG Sch\r\niphol')

    assert Sanitizer.str_resplit('г Москва, ул. Краснопролетарская, д. 1, стр. 3', 30, max_lines=1) == (
        'г Москва, ул. Краснопролетарск')

    assert Sanitizer.str_resplit('Short\nMoscow Yandex', 35, max_lines=4) == 'Short\r\nMoscow Yandex'
    assert Sanitizer.str_resplit('Short\nMoscow Yandex', 35, max_lines=4, as_list=True) == ['Short', 'Moscow Yandex']


def test_replace_by_map():
    assert replace_by_map('some', {'s': 'd', 'm': 'k'}) == 'doke'
    assert replace_by_map('', {'a': 'b'}) == ''
    assert replace_by_map(None, {'a': 'b'}) == ''


def test_mark_string_symbols():

    changed, annotation_map = Sanitizer.swap_cyrillic_similars('Мichеl Jасksоn')

    assert mark_string_symbols(changed, annotation_map) == (
        'Michel Jackson\n'
        '^   ^   ^^  ^ ')
    assert mark_string_symbols('Some', []) == (
        'Some\n'
        '    ')


def test_swap_cyrillic_similars():

    src = 'Мichеl Jасksоn'
    result = Sanitizer.swap_cyrillic_similars(src)
    swapped, letters = result

    assert swapped == 'Michel Jackson'
    assert len(letters) == 5

    assert Sanitizer.check_cyrillics(src)
    assert not Sanitizer.check_cyrillics(swapped)


def test_check_cyrillics():
    assert not Sanitizer.check_cyrillics('some text')

    text = 'some кириллический text ещё'

    indexes = Sanitizer.check_cyrillics(text)

    assert indexes
    assert mark_string_symbols(text, indexes) == (
        'some кириллический text ещё\n'
        '     ^^^^^^^^^^^^^      ^^^')


def test_sanitize_payment_field():
    assert (
        Sanitizer.sanitize_payment_field("Документ ''Первый'' №303 от «Организация some’s и dale's»") ==
        'Документ "Первый" N303 от "Организация somes и dales"')

    assert (
        Sanitizer.sanitize_payment_field(
            "Документ ''Первый'' №303 _от «Организация some’s & dale's»", additional_rules=[('_', ' ')], xml=True
        ) == 'Документ "Первый" N303  от "Организация somes &amp; dales"')


def test_get_subitem():
    src = {
        'root': {'subitem': {'item': 'value'}}
    }

    assert get_subitem(src, 'root.subitem.item') == 'value'
    assert get_subitem(src, 'root.subitem.none') is None
    assert get_subitem(src, 'root.subitem.none', 66) == 66

    class Deep: pass

    a = Deep()
    b = Deep()
    c = Deep()

    a.b = b
    b.c = c
    c.x = 'some'

    assert get_subitem(a, 'b.c.x') == 'some'
    assert get_subitem(a, 'b.c.y') is None
    assert get_subitem(a, 'b.q.x') is None


def test_send_email(mailoutbox):
    sent = send_email(
        'somesubj', 'body', 'at@ya.ru',
        attachments=[
            ('fileone', 'body1', 'text/html'),
            ('filetwo', 'body2', 'text/csv')
        ])

    assert sent == 1
    assert len(mailoutbox) == 1


def test_get_bic_info(monkeypatch):

    def mock_func(*args, **kwargs):

        class Test:
            def get_bics_info(self, *args, **kwargs):
                return {
                    'AGCAAM22XXX': {
                        'countryName': 'Armenia', 'instName': 'ACBA-CREDIT AGRICOLE BANK CJSC', 'branchInfo': '',
                        'addrOpArea': '', 'addrOpCity': 'YEREVAN', 'addrOpZip': '', 'addrOpRegion': '0002 YEREVAN',
                        'bic8': 'AGCAAM22', 'bicBranch': 'XXX', 'addrOpStreet': '', 'addrOpStreetNumber': '',
                        'addrOpBuilding': '', 'addrRegStreet': 'street',
                    }
                }

        return Test()

    monkeypatch.setattr('bcl.toolbox.utils.Swift.get_swift_ref', mock_func)
    assert Swift.get_bic_info('AGCAAM22XXX')

    def mock_func(*args, **kwargs):
        from refsclient.exceptions import RefsClientException

        class Test:
            def get_bics_info(self, *args, **kwargs):
                raise RefsClientException()

        return Test()

    monkeypatch.setattr('bcl.toolbox.utils.Swift.get_swift_ref', mock_func)
    assert not Swift.get_bic_info('IMBKRUMM')
