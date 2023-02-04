# coding: utf-8

import time
import xml.etree.ElementTree as et
from hamcrest import (
    assert_that,
    equal_to,
)

from testutils import (
    TestCase,
)

from intranet.yandex_directory.src.yandex_directory.core.models import (
    OrganizationModel,
    DomainModel,
)
from intranet.yandex_directory.src.yandex_directory.common.utils import to_punycode


class TestSODomainInfoViewCase(TestCase):
    def test_domain_info(self):
        org_id = self.organization['id']
        alias_name = 'тест-киррилицы.ru'
        alias_name_punycode = to_punycode(alias_name)
        # добавим алиас
        DomainModel(self.main_connection).create(alias_name, org_id, owned=True)

        xml_response = self.get_json(
            '/so/domaininfo/?domain={}'.format(self.organization_domain),
            headers={},
            raw=True,
        )

        expected_domains = list(map(to_punycode, DomainModel(self.main_connection).filter(
            org_id=org_id,
            owned=True
        ).fields('name').scalar()))
        assert alias_name_punycode in expected_domains

        org_info = OrganizationModel(self.main_connection).filter(id=org_id).one()
        unixtime_created = time.mktime(org_info['created'].timetuple())

        expected_domain_info = {
            'ip': '0.0.0.0',
            'karma': '50',
            'firsttime': str(unixtime_created),
            'mailboxcnt': str(org_info['user_count']),
            'admlogin': str(org_info['admin_uid']),
            'org_id': str(org_id),
        }
        root = et.XML(xml_response)
        for key, value in expected_domain_info.items():
            assert_that(
                root.findtext(key),
                equal_to(value),
            )

        domains_node = root.find('domains')
        domains_from_response = [d.text for d in domains_node.findall('domain')]
        assert set(domains_from_response) == set(expected_domains)

    def test_not_found_error(self):
        xml_response = self.get_json(
            '/so/domaininfo/?domain={}'.format('test.test'),
            headers={},
            raw=True,
        )

        root = et.XML(xml_response)
        assert_that(
            root.findtext('error'),
            equal_to('not_found')
        )

    def test_no_domain_error(self):
        xml_response = self.get_json(
            '/so/domaininfo/',
            headers={},
            raw=True,
        )

        root = et.XML(xml_response)
        assert_that(
            root.findtext('error'),
            equal_to('no_domain')
        )
