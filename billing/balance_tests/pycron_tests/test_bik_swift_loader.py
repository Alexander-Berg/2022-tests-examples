# -*- coding: utf-8 -*-
import pytest
import mock
import json
from contextlib import contextmanager

from balance import mapper
from cluster_tools.bik_swift_loader import BikSwiftLoaderAppModel, RestrictionCodes

from tests import object_builder as ob


@pytest.fixture
def mock_cbrf():
    output_object = dict()

    def mock_banks_listing(self, fields=None):
        yield output_object

    @contextmanager
    def mocking_banks_listing():
        from refsclient.refs.cbrf import Cbrf
        with mock.patch.object(Cbrf, Cbrf.banks_listing.__name__, new=mock_banks_listing):
            yield None

    return {
        "remote_bank": output_object,
        "mocking": mocking_banks_listing
    }


@pytest.mark.parametrize("existence", ["new", "existing"])
def test_load(session, mock_cbrf, existence):
    is_hidding = (existence == "existing")

    remote_bank = mock_cbrf["remote_bank"]

    remote_bank["bic"] = "TEST_BIK_SWIFT_LOAD"
    remote_bank["swift"] = "NEWCODE"
    remote_bank["nameFull"] = "New name of the bank"
    remote_bank["place"] = "Yuzhno-Sakhalinsk"
    remote_bank["corr"] = None
    remote_bank["restricted"] = is_hidding
    remote_bank["accounts"] = []

    if existence == "existing":
        ob.BankBuilder.construct(session,
                                 bik=remote_bank["bic"],
                                 swift="OLDCODE",
                                 cor_acc=None,
                                 cor_acc_type=None,
                                 accounts="",
                                 name="Old name of the bank",
                                 city="Moscow",
                                 info="",
                                 hidden=False)
        session.flush()

    with mock_cbrf["mocking"]():
        BikSwiftLoaderAppModel().main()

    bank = session.query(mapper.Bank).getone(bik=remote_bank["bic"])

    assert bank.swift == remote_bank["swift"]
    assert bank.cor_acc is None
    assert bank.cor_acc_type is None
    assert bank.accounts == json.dumps(remote_bank['accounts'])
    assert bank.name == remote_bank['nameFull']
    assert bank.city == remote_bank['place']
    assert bank.hidden == is_hidding



@pytest.mark.parametrize("hiding_config", [None, "manual_control"])
@pytest.mark.parametrize("changed_visibility", ["hidding", "reshowing"])
def test_visibility_change(session, mock_cbrf, hiding_config, changed_visibility):
    initial_value = False if changed_visibility == "hidding" else True
    new_value = not initial_value
    expected_value = initial_value if hiding_config == "manual_control" else new_value

    remote_bank = mock_cbrf["remote_bank"]

    remote_bank["bic"] = "TEST_BIK_SWIFT_LOAD"
    remote_bank["swift"] = "CODE"
    remote_bank["nameFull"] = "Bank"
    remote_bank["place"] = "Moscow"
    remote_bank["corr"] = None
    remote_bank["restricted"] = new_value
    remote_bank["accounts"] = []

    ob.BankBuilder.construct(session,
                             bik=remote_bank["bic"],
                             swift=remote_bank["swift"],
                             cor_acc=None,
                             cor_acc_type=None,
                             accounts="",
                             name=remote_bank["nameFull"],
                             city=remote_bank["place"],
                             info="",
                             hidden=initial_value)
    if hiding_config == "manual_control":
        config = session.config.get(BikSwiftLoaderAppModel.MANUAL_HIDING_CONFIG, [])
        config.append(remote_bank["bic"])
        session.config.set(BikSwiftLoaderAppModel.MANUAL_HIDING_CONFIG, config, column_name='value_json')
    session.flush()

    with mock_cbrf["mocking"]():
        BikSwiftLoaderAppModel().main()

    bank = session.query(mapper.Bank).getone(bik=remote_bank["bic"])

    assert (bank.hidden != 0) == expected_value
