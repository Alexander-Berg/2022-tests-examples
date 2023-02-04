from btestlib.constants import Currencies, Firms, ContractCommissionType, Services, Collateral
from temp.igogor.balance_objects import Contexts
from balance import balance_steps as steps
from datetime import datetime, timedelta


def truncate_dt(dt):  # type: (datetime) -> datetime
    return dt.replace(hour=0, minute=0, second=0, microsecond=0)


context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(
    firm=Firms.YANDEX_1,
    service=Services.DIRECT,
    currency=Currencies.RUB,
    contract_type=ContractCommissionType.NO_AGENCY,
)

today = truncate_dt(datetime.today())
yesterday = today - timedelta(days=1)
tomorrow = today + timedelta(days=1)

_, _, contract_id, _ = steps.ContractSteps.create_general_contract_by_context(
    context,
    postpay=False,
    start_dt=tomorrow,
    additional_params={
        'IS_SIGNED': None,
        'IS_FAXED': None,
        'IS_BOOKED': 1,
        'IS_BOOKED_DT': steps.to_iso(yesterday),
        'MEMO': 'col0'
    }
)

# steps.ContractSteps.create_collateral_real(
#     contract_id,
#     collateral_type_id=Collateral.OTHER,
#     params={
#         'MEMO': 'col1',
#         'IS_FAXED': yesterday,
#         'DT': tomorrow,
#     }
# )
#
# steps.ContractSteps.create_collateral_real(
#     contract_id,
#     collateral_type_id=Collateral.OTHER,
#     params={
#         'MEMO': 'col2',
#         # 'IS_FAXED': yesterday,
#         'DT': tomorrow,
#     }
# )