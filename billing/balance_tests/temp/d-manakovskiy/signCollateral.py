from balance import balance_api as api

operator_uid = 0
contract_id = 6055395

api.medium().SignCollateral(
    operator_uid,
    contract_id,
    {
        'faxed_dt': '2021-11-21T00:00:00'
    }
)
