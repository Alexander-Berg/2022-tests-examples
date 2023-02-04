from balance import balance_steps as steps
import balance.balance_db as db

contract_id = 3068373
c = db.balance().execute(
    'select client_id, person_id from t_contract2 where id = :contract_id',
    {'contract_id': contract_id},
    single_row=True
)
client_id, person_id = c['client_id'], c['person_id']

steps.ExportSteps.export_oebs(contract_id=contract_id, client_id=client_id, person_id=person_id)
