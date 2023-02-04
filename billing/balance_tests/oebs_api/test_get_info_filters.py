# -*- coding: utf-8 -*-
import balance.constants as cst
from balance.processors.oebs_api.handlers import ContractExportHandler
from tests import object_builder as ob
from tests.balance_tests.oebs_api.conftest import OEBS_CONFIG_OEBSAPI_FILTER


def set_filter_n_get_info(session, filter_config, contract):
    session.config.__dict__[OEBS_CONFIG_OEBSAPI_FILTER] = filter_config
    c_export = contract.exports['OEBS_API']
    handler = ContractExportHandler(c_export)
    info = handler.get_info()
    session.clear_cache()
    return info


def _test_filter_out_get_info(session):
    session.config.__dict__['CLASSNAMES_EXPORTED_WITH_OEBS_API'] = {'Contract': 1}
    contract = ob.ContractBuilder.construct(
        session,
        ctype='DISTRIBUTION',
        firm=cst.FirmId.YANDEX_OOO,
        contract_type=cst.DistributionContractType.FIXED,
    )
    session.flush()
    # from balance.application import getApplication
    # getApplication().components_cfg['oebs_api'] = {'Token': '666666', 'Url': 'http://ya.ru'}

    attr2filter = 'collaterals'

    info = set_filter_n_get_info(session, None, contract)
    assert attr2filter in info

    info = set_filter_n_get_info(session, [{"classname": "Contract",
                                            "filters": ["$.{}".format(attr2filter)],
                                            "commentary": "66666!"}], contract)
    assert attr2filter not in info


def test_stub(session):
    # oebs_config.oebsapi_filters требует пропатченного jsonpath_rw, поэтому тест пустышка,
    # если же в тестовый контейнер добавить модуль из аркадии, то _test_filter_out_get_info будет работать
    pass
