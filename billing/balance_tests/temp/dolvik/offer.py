from temp.igogor.balance_objects import Contexts
from btestlib.constants import Firms, Services, PersonTypes, Paysyses
from balance.real_builders import common_defaults
from balance.real_builders.invoices.steps import create_base_invoice

CONTEXT = Contexts.MARKET_RUB_CONTEXT.new(
    firm=Firms.MARKET_111,
    service=Services.MARKET_ANALYTICS,
    person_params=common_defaults.PERSON_UR_PARAMS,
    paysys=Paysyses.BANK_UR_RUB
)

create_base_invoice(context=CONTEXT)