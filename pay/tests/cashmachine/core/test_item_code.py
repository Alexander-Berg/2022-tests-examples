import pytest

from kkt_srv.cashmachine.core.item_code import *


@pytest.mark.parametrize(
    ('formatter', 'item_code_text', 'item_code_repr', 'item_code_binary'),
    [
        # Тестовые примеры взяты из Приказа ФНС https://normativ.kontur.ru/document?moduleId=1&documentId=370695
        (ItemCodeFormat.EAN8, '46198488', '4508+46198488', b'\x45\x08\x00\x00\x02\xC0\xEE\xD8'),
        (ItemCodeFormat.EAN13, '4606203090785', '450D+4606203090785', b'\x45\x0D\x04\x30\x77\x19\x57\x61'),
        (ItemCodeFormat.ITF14, '14601234567890', '490E+14601234567890', b'\x49\x0E\x0D\x47\x9D\x66\x52\xD2'),
    ]
)
def test_item_code_number_formats(formatter, item_code_text, item_code_repr, item_code_binary):
    assert item_code_text_to_binary(item_code_text) == (formatter, item_code_binary)
    assert item_code_binary_to_text(item_code_binary) == (formatter, item_code_text)
    assert item_code_binary_to_repr(item_code_binary) == (formatter, item_code_repr)


@pytest.mark.parametrize(
    ('item_code_text_input', 'item_code_text_repr', 'item_code_binary'),
    [
        # Тестовые примеры взяты из Приказа ФНС https://normativ.kontur.ru/document?moduleId=1&documentId=370695
        (
            '010460043993125621JgXJ5.T',
            '444D+010460043993125621JgXJ5.T',
            b'\x44\x4D\x04\x2F\x1F\x96\x81\x78\x4A\x67\x58\x4A\x35\x2E\x54',
        ),
        (
            '010460043993125621JgXJ5.T\u001d8005112000\u001d930001\u001d923zbrLA==\u001d24014276281',
            '444D+010460043993125621JgXJ5.T112000',
            b'\x44\x4D\x04\x2F\x1F\x96\x81\x78\x4A\x67\x58\x4A\x35\x2E\x54\x31\x31\x32\x30\x30\x30',
        ),
        (
            # В Приказе этот пример указан со значением тега 21, равным `N 4N 57RSCBUZTQ`
            # При этом при конвертации в байты пробелы на самом деле пропущены.
            # Однако ни в Приказе, ни в спеке GS1 явно не указано, что пробелы в формате допустимы,
            #   либо что их можно игнорировать.
            '010460406000600021N4N57RSCBUZTQ\u001d2403004002910161218\u001d1724010191ffd0\u001d92tIAF/YVoU4roQS3M/m4z78yFq0fc/WsSmLeX5QkF/YVWwy8IMYAeiQ91Xa2z/fFSJcOkb2N+uUUmfr4n0mOX0Q==',
            '444D+010460406000600021N4N57RSCBUZTQ',
            b'\x44\x4D\x04\x2F\xF7\x5C\x76\x70\x4E\x34\x4E\x35\x37\x52\x53\x43\x42\x55\x5A\x54\x51',
        )
    ]
)
def test_item_code_gs1_format(item_code_text_input, item_code_binary, item_code_text_repr):
    assert item_code_text_to_binary(item_code_text_input) == (ItemCodeFormat.GS1, item_code_binary)
    assert item_code_binary_to_repr(item_code_binary) == (ItemCodeFormat.GS1, item_code_text_repr)


def test_empty_item_code():
    assert item_code_text_to_binary('') == (None, None)
    assert item_code_binary_to_repr(b'') == (None, None)
    assert item_code_binary_to_repr(b'\x00\x00') == (None, None)


def test_item_code_unformatted_data():
    unformatted_data = 'X' * 66
    unformatted_bytes = b'\x00\x00' + b'X' * 30
    assert item_code_text_to_binary(unformatted_data) == (ItemCodeFormat.Unrecognized, unformatted_bytes)
    assert item_code_binary_to_repr(unformatted_bytes) == (ItemCodeFormat.Unrecognized, '0000+' + 'X' * 30)


@pytest.mark.parametrize(
    ('reason', 'item_code'),
    [
        ('No GTIN', '01012345678901234'),
        ('No serial_number', '21abcdefgh'),
        ('Short GTIN which parses as another element', '010123456789'),
    ]
)
def test_item_code_semantically_illformed_gs1(reason, item_code):
    formatter, _ = item_code_text_to_binary(item_code)
    assert formatter == ItemCodeFormat.Unrecognized


@pytest.mark.parametrize(
    ('format_name', 'item_code'),
    [
        # Примеры взяты из Приказа ФНС https://normativ.kontur.ru/document?moduleId=1&documentId=370695
        ('Non-GS1, 44h 4Dh', '00000046198488X?io+qCABm8wAYa',),
        ('Меховое изделие, 52h 46h', 'RU-401301-AAA0277031',),
        ('ЕГАИС 2.0 @ PDF417, C5h 14h', '22N 00002NU5DBKYDOT17ID980726019019608CW1A4XR5EJ7JKFX50FHHGV92ZR2GZRZ',),
        ('ЕГАИС 3.0 @ DataMatrix, C5h 1Eh',
         '136222000058810918QWERDFEWT5123456YGHFDSWERT56YUIJHGFDSAERTUIOKJ8HGFVCXZSDLKJHGFDSAOIPLMNBGHJYTRDFGHJKIREWSDFGHJIOIUTDWQASDFRETYUIUYGTREDFG HUYTREWQWE',),
    ]
)
def test_item_code_not_implemented_formats(format_name, item_code):
    formatter, _ = item_code_text_to_binary(item_code)
    assert formatter == ItemCodeFormat.Unrecognized


@pytest.mark.parametrize(
    ('description', 'item_code', 'result'),
    [
        ('gs1m should convert to gs1',
         '010304109478744321tE%HqMa_lOQ4D\u001d93dGVz', '444D+010304109478744321tE%HqMa_lOQ4D'),
        ('should remain unchanged', '123', None),
    ]
)
def test_item_code_gs1m_conversion(description, item_code, result):
    assert convert_gs1m_to_gs1(item_code) == result


@pytest.mark.parametrize(
    ('description', 'item_code', 'formatter', 'result'),
    [
        ('gs1m should convert to gs1',
         '010304109478744321tE%HqMa_lOQ4D\u001d93dGVz', ItemCodeFormat.GS1, '444D+010304109478744321tE%HqMa_lOQ4D'),
        ('gs1 should be parsed ok', '010460043993125621JgXJ5.T', ItemCodeFormat.GS1, '444D+010460043993125621JgXJ5.T'),
        ('should remain unchanged', '123', ItemCodeFormat.Unrecognized, '0000+123'),
    ]
)
def test_item_code_to_repr(description, item_code, formatter, result):
    assert item_code_text_to_repr(item_code) == (formatter, result)
