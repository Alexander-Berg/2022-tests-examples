# -*- coding: utf-8 -*-

import uuid
import xmlrpclib
from datetime import datetime, timedelta

import pytest
from hamcrest import assert_that, contains_string, has_entries, has_item, has_items, has_properties
from butils import logger

from balance import mapper
from tests import object_builder as ob

log = logger.get_logger()


def nullify_time_of_date(date):
    return date.replace(hour=0, minute=0, second=0, microsecond=0) if date else None


START_DT = nullify_time_of_date(datetime.now() - timedelta(days=60))
END_DT = nullify_time_of_date(datetime.now() + timedelta(days=60))
MEMO = u"Credit is a system whereby a person who can`t pay gets another person who can`t pay to guarantee that he can pay. ~Charles Dickens"
PAYSYS_TYPE = "paypal"
PROCESSING_TARIFFS = "I owe my soul to the company store"

FIRM_ID_YANDEX = 1
OEBS_FIRM_ID_YANDEX = FIRM_ID_YANDEX
# -- Может быть странно, что эти ID совпадают, но у всех существующих договоров на эквайринг в продовой базе сейчас так
# SELECT
#   LISTAGG(a.code, ',') WITHIN GROUP (ORDER BY a.code),
#   LISTAGG(a.value_num, ',') WITHIN GROUP (ORDER BY a.value_num)
# FROM
#       bo.t_contract2 c
#   JOIN
#       bo.t_contract_collateral col
#       ON c.id = col.contract2_id
#   JOIN
#       bo.t_contract_attributes a
#       ON col.id = a.collateral_id
# WHERE 1=1
#   AND c.type = 'ACQUIRING'
#   AND a.code LIKE '%FIRM'
# GROUP BY
#   c.id
# ;


@pytest.mark.tickets('BALANCE-32091')
@pytest.mark.usefixtures("session")
@pytest.mark.usefixtures("xmlrpcserver")
class TestCreateCommonContractForAcquiring(object):

    @pytest.mark.parametrize("signed", [0, 1], ids=lambda s: "signed=%d" % s)
    def test_happy_path(self, signed):
        """
        Проверить заведение договоров на эквайринг через CreateCommonContract
        """
        # Внешний ID берётся из договора
        external_id = "unittest-%d" % uuid.uuid4()

        # В качестве клиента должна быть использована запись из t_client с флагом is_acquiring=1
        acquiring_client = ob.ClientBuilder(is_acquiring=True).build(self.session).obj

        # Создаём договор на эквайринг
        contract_data = self.xmlrpcserver.CreateCommonContract(
            self.session.oper_id,
            {
                "ctype": "ACQUIRING",
                "external_id": external_id,
                "client_id": acquiring_client.id,
                "firm_id": FIRM_ID_YANDEX,
                "oebs_firm": OEBS_FIRM_ID_YANDEX,
                "start_dt": START_DT,
                "end_dt": END_DT,
                "memo": MEMO,
                "paysys_type": PAYSYS_TYPE,
                "processing_tariffs": PROCESSING_TARIFFS,
                "signed": signed,

                # Полный список параметров CreateCommonContract пожалуйста смотрите на Wiki:
                # https://wiki.yandex-team.ru/balance/xmlrpc/#balance.createcommoncontract

            },
        )
        contract_id = contract_data["ID"]
        contract_eid = contract_data["EXTERNAL_ID"]
        assert_that(contract_eid, external_id)

        # Проверяем возвращает ли ручка GetClientContracts созданный нами договор
        contracts_data = self.xmlrpcserver.GetClientContracts({
            "ContractType": "ACQUIRING",
            "ClientID": acquiring_client.id,
            "Signed": signed,
        })
        assert_that(
            contracts_data,
            has_item(has_entries(ID=contract_id, EXTERNAL_ID=external_id, IS_SIGNED=signed)),
        )

        # Проверим что у созданного договора в базе всё норм
        contract = self.session.query(mapper.Contract).filter(mapper.Contract.id == contract_id).one()
        assert_that(contract.type == "ACQUIRING")

        collaterals = contract.collaterals
        assert_that(collaterals[0].dt == START_DT)

        attributes = collaterals[0].attributes
        assert_that(
            attributes,
            has_items(
                has_properties(code="FIRM", value_num=FIRM_ID_YANDEX),
                has_properties(code="OEBS_FIRM", value_num=OEBS_FIRM_ID_YANDEX),
                has_properties(code="END_DT", value_dt=END_DT),
                has_properties(code="MEMO", value_str=contains_string(MEMO)),
                has_properties(code="PAYSYS_TYPE", value_str=PAYSYS_TYPE),
                has_properties(code="PROCESSING_TARIFFS", value_str=PROCESSING_TARIFFS),
            ),
        )

    @pytest.mark.parametrize("signed", [0, 1], ids=lambda s: "signed=%d" % s)
    def test_error_on_non_acquiring_client(self, signed):
        """
        CreateCommonContract не должен разрешать создавать договоры на эквайринг если у клиента нет флага is_acquiring=1
        """
        external_id = "unittest-%d" % uuid.uuid4()
        builder = ob.ClientBuilder(

            is_acquiring=False,  # <-- такой клиент не годится

        )
        acquiring_client = builder.build(self.session).obj
        with pytest.raises(xmlrpclib.Fault) as excinfo:
            self.xmlrpcserver.CreateCommonContract(
                self.session.oper_id,
                {
                    "ctype": "ACQUIRING",
                    "external_id": external_id,
                    "client_id": acquiring_client.id,
                    "firm_id": FIRM_ID_YANDEX,
                    "oebs_firm": OEBS_FIRM_ID_YANDEX,
                    "start_dt": START_DT,
                    "end_dt": END_DT,
                    "memo": MEMO,
                    "paysys_type": PAYSYS_TYPE,
                    "processing_tariffs": PROCESSING_TARIFFS,
                    "signed": signed,
                },
            )
        assert_that(str(excinfo), contains_string("Invalid parameter"))
        assert_that(str(excinfo), contains_string("is_acquiring"))
        assert_that(str(excinfo), contains_string(str(acquiring_client.id)))

    @pytest.mark.parametrize("signed", [0, 1], ids=lambda s: "signed=%d" % s)
    def test_error_on_unknown_paysys(self, signed):
        """
        CreateCommonContract не должен разрешать создавать договоры на эквайринг если передана неизвестная платёжная система
        """
        external_id = "unittest-%d" % uuid.uuid4()
        acquiring_client = ob.ClientBuilder(is_acquiring=True).build(self.session).obj
        with pytest.raises(xmlrpclib.Fault) as excinfo:
            self.xmlrpcserver.CreateCommonContract(
                self.session.oper_id,
                {
                    "ctype": "ACQUIRING",
                    "external_id": external_id,
                    "client_id": acquiring_client.id,
                    "firm_id": FIRM_ID_YANDEX,
                    "oebs_firm": OEBS_FIRM_ID_YANDEX,
                    "start_dt": START_DT,
                    "end_dt": END_DT,
                    "memo": MEMO,

                    "paysys_type": "NO-SUCH-PAYSYS",  # <-- неизвестная платёжная система

                    "processing_tariffs": PROCESSING_TARIFFS,
                    "signed": signed,
                },
            )
        assert_that(str(excinfo), contains_string("Invalid parameter"))
        assert_that(str(excinfo), contains_string("paysys_type"))
        assert_that(str(excinfo), contains_string("NO-SUCH-PAYSYS"))
