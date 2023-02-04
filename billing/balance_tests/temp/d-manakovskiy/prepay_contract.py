from btestlib.constants import Currencies, Firms, ContractCommissionType, Services
from temp.igogor.balance_objects import Contexts
import balance.balance_steps as steps

context = Contexts.DIRECT_FISH_RUB_CONTEXT.new(
    firm=Firms.YANDEX_1,
    service=Services.DIRECT,  # not partner service
#   service=Services.SUBAGENCY_EVENTS_TICKETS3,  # partner service
    currency=Currencies.RUB,
    contract_type=ContractCommissionType.NO_AGENCY,
)

_, _, contract_id, _ = steps.ContractSteps.create_general_contract_by_context(
    context,
    postpay=False,
    is_signed=False,
)