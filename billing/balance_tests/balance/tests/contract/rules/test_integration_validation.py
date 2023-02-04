# coding=utf-8
__author__ = 'borograam'

import collections

import pytest
import datetime
import json
from enum import Enum
from balance import balance_steps as steps
from btestlib.data.partner_contexts import PVZ_RU_CONTEXT_SPENDABLE, USLUGI_CONTEXT
from btestlib.constants import PersonTypes as pt, ContractSubtype, ContractPaymentType
from btestlib.data import person_defaults


contract_start_dt = datetime.datetime.today().replace(day=1)
INDIVIDUAL_INN = 190568076491

ctype_map = {
    "SPENDABLE": PVZ_RU_CONTEXT_SPENDABLE,
    "GENERAL": USLUGI_CONTEXT,
}


def create_client_person(context, person_type, ownership_type=None):
    client_id = steps.ClientSteps.create()
    is_partner = context.contract_type in (ContractSubtype.SPENDABLE, ContractSubtype.PARTNERS)
    person_attrs = {'is-partner': str(int(is_partner))}
    if ownership_type is not None:
        if ownership_type == 'INDIVIDUAL':
            person_attrs['inn'] = INDIVIDUAL_INN
        else:
            person_attrs['ownership_type'] = ownership_type

    person_id = steps.PersonSteps.create(client_id, person_type,
                                         params=person_attrs,
                                         inn_type=person_defaults.InnType.RANDOM,
                                         name_type=person_defaults.NameType.RANDOM,
                                         full=True)
    return client_id, person_id


def process_validate(context, client_id, person_id, contract_scheme, expected_error):
    if expected_error is None:
        create_contract(context, client_id, person_id, contract_scheme)
    else:
        with pytest.raises(Exception) as e:
            create_contract(context, client_id, person_id, contract_scheme)
        assert repr(expected_error)[2:-1] in str(e.value), u'Неверный текст ошибки\nПолучили: {}'.format(str(e.value))


def create_contract(context, client_id, person_id, integration_scheme, additional_params=None):
    ctype = ContractSubtype(context.contract_type).name
    if not additional_params:
        additional_params = {}
    additional_params.update(start_dt=contract_start_dt)

    return steps.ContractSteps.create_partner_contract(
        context,
        client_id=client_id,
        person_id=person_id,
        additional_params=additional_params,
        partner_integration_params=dict(
            steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT,
            create_configuration_args={
                'params': {
                    'scheme': get_configuration_json(integration_scheme, ctype),
                }
            }
        )
    )


def get_configuration_json(config_dict, ctype):
    contract_section = {
        "_params": {
            "enable_setting_attributes": 1,
            "enable_validating_attributes": 1,
        },
        'ctype': ctype
    }
    contract_section.update(config_dict)
    return json.dumps(dict(contracts=[contract_section]))


@pytest.mark.parametrize(
    "ctype",
    (
        "SPENDABLE",
        "GENERAL",
    ))
@pytest.mark.parametrize(
    "config_person_type, person_type, expected_error",
    (
        pytest.param(
            pt.UR.code,
            pt.PH.code,
            u"Выбран неправильный плательщик - необходим type in [ur]",
            id="neg_one_value"),
        pytest.param(
            [pt.UR.code, pt.PH.code],
            pt.UR.code,
            None,
            id="pos_array"),
        pytest.param(
            {"mandatory": pt.PH.code},
            pt.UR.code,
            u"Выбран неправильный плательщик - необходим type in [ph]",
            id="neg_mandatory_one_value"),
        pytest.param(
            {"forbidden": pt.PH.code},
            pt.UR.code,
            None,
            id="pos_forbidden_one_value"),
        pytest.param(
            {"mandatory": [pt.UR.code, pt.PH.code]},
            pt.PH.code,
            None,
            id="pos_mandatory_array"),
        pytest.param(
            {"forbidden": [pt.UR.code, pt.PH.code]},
            pt.PH.code,
            u'Выбран неправильный плательщик - запрещён type in [ur, ph]',
            id="neg_forbidden_array"),
        pytest.param(
            {"wrong": [pt.UR.code]},
            pt.UR.code,
            u'Ошибка конфигурации: person.type не содержит обязательные или запрещённые значения. Обратитесь в поддержку',
            id="negative_no_mandatory_or_forbidden"),
    ))
def test_person_type(ctype, config_person_type, person_type, expected_error):
    context = ctype_map[ctype]
    client_id, person_id = create_client_person(context, person_type)

    contract_scheme = {
        "person": {"type": config_person_type}
    }
    process_validate(context, client_id, person_id, contract_scheme, expected_error)


@pytest.mark.parametrize(
    "ctype",
    (
        "SPENDABLE",
        "GENERAL",
    )
)
@pytest.mark.parametrize(
    "config_val, person_type, ownership_type, expected_error",
    (
        pytest.param(
            ["INDIVIDUAL", "SELFEMPLOYED"], pt.UR.code, "INDIVIDUAL",
            None,
            id="positive_mandatory_multiple"),
        pytest.param(
            "SELFEMPLOYED", pt.UR.code, "SELFEMPLOYED",
            None,
            id="positive_mandatory_one"),
        # изменена логика валидации ownership_type_ui, теперь там не молча игнорируется, а кидается ошибка в медиуме
        # pytest.param(
        #     dict(mandatory=["INDIVIDUAL", "SELFEMPLOYED"]), pt.PH.code, "SELFEMPLOYED",
        #     u'Выбран неправильный плательщик - необходим ownership_type_ui in [INDIVIDUAL, SELFEMPLOYED]',
        #     id="not_ur"),
        pytest.param(
            dict(mandatory="INDIVIDUAL"), pt.UR.code, "SELFEMPLOYED",
            u'Выбран неправильный плательщик - необходим ownership_type_ui in [INDIVIDUAL]',
            id="negative_mandatory"),
        pytest.param(
            dict(forbidden="INDIVIDUAL"), pt.UR.code, "SELFEMPLOYED",
            None,
            id="positive_forbidden"),
        pytest.param(
            dict(forbidden=["SELFEMPLOYED"]), pt.UR.code, "SELFEMPLOYED",
            u'Выбран неправильный плательщик - запрещён ownership_type_ui in [SELFEMPLOYED]',
            id="negative_forbidden_one"),
        pytest.param(
            dict(forbidden=["SELFEMPLOYED", "INDIVIDUAL"]), pt.UR.code, "SELFEMPLOYED",
            u'Выбран неправильный плательщик - запрещён ownership_type_ui in [SELFEMPLOYED, INDIVIDUAL]',
            id="negative_forbidden_multiple"),
    )
)
def test_person_ownership_type_ui(ctype, config_val, person_type, ownership_type, expected_error):
    context = ctype_map[ctype]
    client_id, person_id = create_client_person(context, person_type, ownership_type)
    contract_scheme = {
        "person": {"ownership_type_ui": config_val}
    }
    process_validate(context, client_id, person_id, contract_scheme, expected_error)


class Attribute(object):
    def __init__(self, type_, name, val):
        self._type = type_
        self._name = name
        self._val = val

    def check_on_contract(self, contract_id, exists=True):
        res = steps.ContractSteps.get_attribute(contract_id, self._type, self._name, only_first=False)
        assert not (exists ^ bool(res)), u'Атрибут {} {} должен быть в договоре {}'.format(
            self._name, '' if exists else u'не', contract_id)
        if exists:
            assert res[0] == self._val, u'Атрибут {} должен иметь значение {} в договоре {}'.format(
                self._name, self._val, contract_id
            )


class Attr(Enum):
    PERSONAL_ACCOUNT = Attribute('value_num', 'PERSONAL_ACCOUNT', 1)
    PARTNER_CREDIT = Attribute('value_num', 'PARTNER_CREDIT', 1)
    PERSONAL_ACCOUNT_FICTIVE = Attribute('value_num', 'PERSONAL_ACCOUNT_FICTIVE', 1)


@pytest.mark.parametrize(
    "payment_type",
    (
            pytest.param(ContractPaymentType.PREPAY, id='prepay'),
            pytest.param(ContractPaymentType.POSTPAY, id='postpay'),
    )
)
def test_partner_contract(payment_type):  # only set in general
    expected_attrs = {
        'exist': (Attr.PERSONAL_ACCOUNT, Attr.PARTNER_CREDIT),
        # при создании договора по 710 с постоплатой, будет автоматически выбрана галочка personal_account_fictive
        'not exist': (Attr.PERSONAL_ACCOUNT_FICTIVE,),
    }
    context = ctype_map['GENERAL']

    client_id, person_id = create_client_person(context, context.person_type.code)

    contract_scheme = {
        "partner_contract": {"attributes": "common"},
        "payment_type": payment_type
    }

    _, _, contract_id, _ = create_contract(
        context, client_id, person_id, contract_scheme)

    for exists, attrs in ((k == 'exist', v) for k, v in expected_attrs.items()):
        for attr in attrs:
            attr.value.check_on_contract(contract_id, exists=exists)
