import datetime

from billing.apikeys.apikeys import mapper
from billing.apikeys.apikeys.startrek_wrapper.startrek import ServiceStartrekClient


class Fabric:

    def __init__(self):
        self.id = 0

    def __call__(self):
        self.id += 1


class UnitFabric(Fabric):

    def __call__(self):
        super().__call__()
        return mapper.Unit(id=self.id, cc='test_unit_' + str(self.id)).save()


class ReasonFabric(Fabric):

    def __call__(self, lock=True):
        super().__call__()
        tag = 'lock' if lock else 'unlock'
        return mapper.ReasonDict(id=self.id, text='test_reason_' + str(self.id), tag=tag).save()


class ServiceFabric(Fabric):

    def __init__(self, unit_fabric, reason_fabric):
        super().__init__()
        self._unit_fabric = unit_fabric
        self._reason_fabric = reason_fabric

        self._units = None

    @property
    def units(self):
        if not self._units:
            self._units = [self._unit_fabric().cc for _ in range(2)]
        return self._units

    def __call__(self, units=None, lock_reasons=None, unlock_reasons=None):
        super().__call__()

        if not units:
            units = self.units
        if not lock_reasons:
            lock_reasons = [self._reason_fabric().id]
        if not unlock_reasons:
            unlock_reasons = [self._reason_fabric(lock=False).id]

        return mapper.Service(
            id=self.id,
            cc='test_cc_' + str(self.id),
            token='test_token_' + str(self.id),
            name='[TEST] Simple Service ' + str(self.id),
            units=units,
            lock_reasons=lock_reasons,
            unlock_reasons=unlock_reasons,
        ).save()


class FakeReadPreferenceSettings(mapper.context.Settings):

    def __init__(self, r):
        super().__init__()


class FakeServiceStartrekClient(ServiceStartrekClient):
    def __init__(self, *args, **kwargs):
        pass


def to_midnight(date: datetime.datetime):
    return date.replace(hour=0, minute=0, second=0, microsecond=0)


def mock_datetime(new_now: datetime):
    class MockDatetime:
        @classmethod
        def now(cls, *args, **kwargs):
            return new_now
    return MockDatetime


def new_tarifficator_state_content(link, value, date_now, activated_and_paid_days_ago):
    tariff = mapper.Tariff.getone(cc=link.config.tariff)
    next_consume_date = tariff.switch_date(date_now + datetime.timedelta(days=activated_and_paid_days_ago))
    activated_date = date_now - datetime.timedelta(days=activated_and_paid_days_ago + 1)
    last_run = date_now - datetime.timedelta(days=1)

    tstate = mapper.TarifficatorState.get_for_link(link)
    tstate.state.update({
        'products': {
            '777': {
                'consumed': value,
                'credited': value,
                'autocharge_personal_account': True,
                'credited_deficit': value,
                'next_consume_date': next_consume_date,
                'next_consume_value': value
            }
        },
        "last_run": last_run,
        "is_active": True,
        "activated_date": activated_date,
        "ban_units": {}
    })
    tstate.save()


def new_add_next_state_to_tarifficator_state(link, value, discount):
    tstate = mapper.TarifficatorState.get_for_link(link)
    tstate.state['next_tariff_state'] = {
        "products": {
            "777": {
                "PrepayPeriodicallyDiscountedUnit_yearprepay_contractless": {
                    "prev_consume_value": value,
                    "consume_discount": discount
                }
            }
        }
    }
    tstate.save()
