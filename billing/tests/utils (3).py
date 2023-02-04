from datetime import datetime

CLIENT_ID = 1
CLIENT_SD_ID = 2
PERSON_ID = 2
GENERAL_CONTRACT_ID = 666
SPENDABLE_CONTRACT_ID = 667
DISTRIBUTION_CONTRACT_ID = 668
UR_PERSON = 'ur'
RU_FIRM = 13


def gen_general_contract(contract_id: int = GENERAL_CONTRACT_ID,
                         client_id: int = CLIENT_ID,
                         person_id: int = PERSON_ID,
                         services: list[int] = None,
                         person_type: str = UR_PERSON,
                         dt: datetime = datetime(2020, 1, 1),
                         signed: bool = True) -> dict:
    dt_iso = dt.isoformat()

    return {'client_id': client_id,
            'collaterals': {
                "0": {
                    "attribute_batch_id": 77072802,
                    "collateral_type_id": None,
                    "commission": 0,
                    "contract2_id": contract_id,
                    "create_dt": dt_iso,
                    "dt": dt_iso,
                    "finish_dt": dt_iso,
                    "id": 27287621,
                    "is_cancelled": None,
                    "is_faxed": None,
                    "is_signed": dt_iso if signed else None,
                    "manager_code": 43417126,
                    "num": None,
                    "passport_id": 793360492,
                    "payment_type": 3,
                    "services": {s: 1 for s in services} if services else [],
                    "update_dt": dt_iso
                }
            },
            'external_id': 'test_general',
            'id': contract_id,
            'person_id': person_id,
            'person_type': person_type,
            'type': 'GENERAL',
            'passport_id': 793360492,
            'update_dt': dt_iso,
            }


def gen_spendable_contract(contract_id: int = SPENDABLE_CONTRACT_ID,
                           client_id: int = CLIENT_ID,
                           person_id: int = PERSON_ID,
                           services: list[int] = None,
                           person_type: str = UR_PERSON,
                           dt: datetime = datetime(2020, 1, 1),
                           signed: bool = True,
                           currency: str = 'RUB',
                           firm: int = RU_FIRM,
                           nds: int = 18) -> dict:
    dt_iso = dt.isoformat()
    curr_to_iso_code = {'RUB': 643}

    return {'client_id': client_id,
            'collaterals': {
                '0': {
                    'collateral_type_id': None,
                    'contract2_id': contract_id,
                    'currency': curr_to_iso_code[currency],
                    'dt': dt_iso,
                    'firm': firm,
                    'id': contract_id,
                    'is_cancelled': None,
                    'is_faxed': None,
                    'is_offer': 1,
                    'is_signed': dt_iso if signed else None,
                    'nds': nds,
                    'num': None,
                    'pay_to': 1,
                    'payment_type': 1,
                    'service_start_dt': dt_iso,
                    'services': {s: 1 for s in services} if services else [],
                }
            },
            'external_id': 'test_spendable',
            'id': contract_id,
            'person_id': person_id,
            'person_type': person_type,
            'type': 'SPENDABLE',
            'passport_id': 793360492,
            'update_dt': dt_iso,
            }


def gen_distribution_contract(contract_id: int = DISTRIBUTION_CONTRACT_ID,
                              client_id: int = CLIENT_ID,
                              person_id: int = PERSON_ID,
                              services: list[int] = None,
                              person_type: str = UR_PERSON,
                              dt: datetime = datetime(2020, 1, 1)):
    dt_iso = dt.isoformat()

    return {
        "client_id": client_id,
        "collaterals": {
            "0": {
                "attribute_batch_id": 77084862,
                "collateral_type_id": None,
                "contract2_id": contract_id,
                "create_dt": dt_iso,
                "dt": dt_iso,
                "end_dt": dt,
                "id": 27292866,
                "is_cancelled": None,
                "is_faxed": None,
                "is_signed": dt_iso,
                "manager_code": 843434142,
                "num": None,
                "passport_id": -398464875,
                "payment_type": 3,
                "services": {s: 1 for s in services} if services else [],
                "tail_time": 6,
                "update_dt": dt_iso
            }
        },
        "external_id": "test/test",
        "id": contract_id,
        "passport_id": -398464875,
        "person_id": person_id,
        "person_type": person_type,
        "type": "DISTRIBUTION",
        "update_dt": dt_iso,
        "version_id": 0
    }
