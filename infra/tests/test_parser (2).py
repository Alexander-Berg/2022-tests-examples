import base64

import pytest

from infra.skylib.openssh_krl import (
    MAGIC,
    FORMAT_VERSION,
    load_header,
    load_section,
    loads,
    CertificatesSection,
    CertificatesSectionSerialList,
)


def get_krl_data():
    import yatest.common

    path = yatest.common.test_source_path('krl.lst')
    with open(path, 'rb') as f:
        return f.read()


def test_header():
    data = get_krl_data()
    header, _ = load_header(data)

    assert header.magic == MAGIC
    assert header.format_version == FORMAT_VERSION
    assert header.krl_version == 1637596800
    assert header.generated_date == 1637596800
    assert header.flags == 0
    assert header.reserved == b''
    assert header.comment == b''


@pytest.mark.parametrize('corrupted_byte', list(range(8 + 4)) + list(range(36, 40)))
def test_invalid_headers(corrupted_byte):
    data = list(get_krl_data())
    data[corrupted_byte] = 0xff
    data = bytes(data)

    with pytest.raises(ValueError):
        load_header(data)


def test_invalid_section():
    with pytest.raises(ValueError):
        load_section(b'\xff\xff')


def test_krl_parsing():
    data = get_krl_data()

    krl = loads(data)

    header = krl.header
    assert header.magic == MAGIC
    assert header.format_version == FORMAT_VERSION
    assert header.krl_version == 1637596800
    assert header.generated_date == 1637596800
    assert header.flags == 0
    assert header.reserved == b''
    assert header.comment == b''

    sections = krl.sections
    assert len(sections) == 3, sections
    assert all(isinstance(section, CertificatesSection) for section in sections)

    for section in sections:
        assert not section.reserved
        assert len(section.cert_sections) == 1
        assert isinstance(section.cert_sections[0], CertificatesSectionSerialList)

    assert len(sections[0].cert_sections[0].serials) == len(sections[1].cert_sections[0].serials)
    assert len(sections[0].cert_sections[0].serials) == len(sections[2].cert_sections[0].serials)

    sudo = base64.b64decode('AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBGu6MjOjHX9R4w/1EZ+0A938E5O63iTAc3HAM6ixrX0wx5XuS8WFm+xxZotYloqtS2LnZ1lWe6IHPh5jVIplCaY=')
    insecure = base64.b64decode('AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBP8w+sr7XJuSXHQ9OAOj0eRfv4fQi/qFnW185Ae5fkKX9VhtmkM7LpfIy7NeOOthxS9wUJmfEcAGUrSH5Pry/ZU=')
    secure = base64.b64decode('AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBJnF1UG50PejZLaFFAWTMOL7e4xy44Z/mDJyF6RTsQsIxFN2oC9E4cwOTKSf2Ko/jemdnDOWu+j8X6f4y2KTV5M=')

    assert sections[0].ca_key == sudo
    assert sections[1].ca_key == insecure
    assert sections[2].ca_key == secure
