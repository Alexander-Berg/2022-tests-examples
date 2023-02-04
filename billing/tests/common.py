from datetime import datetime


class ContractJSONBuilder:
    def __init__(self, _type, client_id=111, external_id='111/11', id=112, passport_id=113, person_id=114, update_dt=None, version_id=0, **kwargs):
        if update_dt is None:
            update_dt = datetime.now().replace(microsecond=0).isoformat()

        self.json = {
            'type': _type,
            'client_id': client_id,
            'external_id': external_id,
            'id': id,
            'passport_id': passport_id,
            'person_id': person_id,
            'update_dt': update_dt,
            'version_id': version_id,
            'collaterals': {},
            **kwargs
        }

    def add_collateral(self, collateral_type_id=None, id=None, num=None, is_signed=None, is_faxed=None, is_cancelled=None, dt=None, attribute_batch_id=None, **kwargs):
        if dt is None:
            dt = datetime.now().replace(microsecond=0).isoformat()

        collateral = {
            'collateral_type_id': collateral_type_id,
            'id': id,
            'num': num,
            'is_signed': is_signed,
            'is_faxed': is_faxed,
            'is_cancelled': is_cancelled,
            'dt': dt,
            'attribute_batch_id': attribute_batch_id,
            **kwargs
        }

        collaterals = self.json['collaterals']
        next_col_num = len(collaterals.keys())
        collaterals[str(next_col_num)] = collateral
