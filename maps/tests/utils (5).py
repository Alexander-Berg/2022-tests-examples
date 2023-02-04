from maps_adv.geosmb.scenarist.proto import scenarios_pb2
from maps_adv.geosmb.scenarist.server.lib.enums import ScenarioName, SubscriptionStatus

ENUM_MAPS_TO_PB = {
    "scenario_name": {
        ScenarioName.DISCOUNT_FOR_LOST: scenarios_pb2.ScenarioName.DISCOUNT_FOR_LOST,
        ScenarioName.ENGAGE_PROSPECTIVE: scenarios_pb2.ScenarioName.ENGAGE_PROSPECTIVE,
        ScenarioName.THANK_THE_LOYAL: scenarios_pb2.ScenarioName.THANK_THE_LOYAL,
        ScenarioName.DISCOUNT_FOR_DISLOYAL: scenarios_pb2.ScenarioName.DISCOUNT_FOR_DISLOYAL,  # noqa
    },
    "subscription_status": {
        SubscriptionStatus.ACTIVE: scenarios_pb2.SubscriptionStatus.ACTIVE,
        SubscriptionStatus.PAUSED: scenarios_pb2.SubscriptionStatus.PAUSED,
        SubscriptionStatus.COMPLETED: scenarios_pb2.SubscriptionStatus.COMPLETED,
    },
}
