# -*- coding: utf-8 -*-
import hamcrest as hm

from balance import xmlizer as xr

from tests import object_builder as ob


def test_base_xmlize(session):
    """Т.к. ClientXmlizer по умолчанию использует все аттрибуты клиента,
    периодически ловим ошибки в проде типа BALANCE-35042.
    Так что этот тест должен проверять, что все аттрибуты xml отображаются корректно либо добавлены в исключения.
    """
    client = ob.ClientBuilder.construct(session)

    allowed_fields = [
        'acts-most-valuable-priority', 'agencies-printable-doc-types', 'agency', 'agency-id',
        'bad-debts-count', 'budget', 'can-be-subclient-in-self', 'can-issue-initial-invoice',
        'city', 'class-id', 'client-type-id',
        'country', 'creation-dt', 'creator-uid',
        'currency-payment', 'deny-cc', 'deny-overdraft',
        'direct25', 'discounts-2010', 'doc-delivery-type',
        'domain-check-comment', 'domain-check-status', 'dt',
        'email', 'fax', 'force-contractless-invoice',
        'force-direct-migration', 'fraud-status', 'full-repayment',
        'fullname', 'id', 'intercompany',
        'internal', 'is-acquiring', 'is-agency',
        'is-new', 'is-non-resident', 'is-wholesaler',
        'iso-currency-payment', 'manual-discount', 'manual-suspect',
        'manual-suspect-comment', 'name', 'only-manual-name-update',
        'oper-id', 'overdraft-ban', 'overdraft-limit',
        'partner-type', 'phone', 'private-key',
        'region-id', 'reliable-cc-payer', 'service-id',
        'single-account-number', 'skip-paystep-region-filter', 'sms-notify',
        'sms-passport-uid', 'subagency', 'subregion-id',
        'suspect', 'trust-anonymous', 'url',
    ]

    client_xml = xr.xmlize_tree(client, xr.XTree('client', None))
    hm.assert_that(
        client_xml.getchildren(),
        hm.contains_inanyorder(*[
            hm.has_property('tag', tag)
            for tag in allowed_fields
        ]),
    )
