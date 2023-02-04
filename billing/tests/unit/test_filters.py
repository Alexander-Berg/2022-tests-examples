from billing.balance_yt_jobs.contract_notify.filters import ContractFilter, NotifyFilter
from billing.balance_yt_jobs.contract_notify.tests.common import ContractJSONBuilder

from billing.contract_iface import JSONContract
from billing.contract_iface.constants import ContractTypeId


class TestContractFilter:
    def get_contract_filter(self):
        return ContractFilter({
            'include': [
                {
                    'kind': 'GENERAL',
                    'exclude': {'type': ContractTypeId.PARTNERSHIP}
                },
                {'kind': 'DISTRIBUTION'},
                {
                    'kind': 'SPENDABLE',
                    'is_offer': True
                },
                {'kind': [{'eq': 'PARTNERS'}, {'like': '.?FISH.*'}, 'ACQUIRING']},
            ],
            'exclude': {'kind': 'ACQUIRING'}
        })

    def test_include_by_property(self):
        contract_filter = self.get_contract_filter()

        contract = ContractJSONBuilder('DISTRIBUTION')
        contract.add_collateral()

        assert contract_filter.check(JSONContract(contract_data=contract.json))

    def test_exclude_by_property(self):
        contract_filter = self.get_contract_filter()

        contract = ContractJSONBuilder('ACQUIRING')
        contract.add_collateral(commission=ContractTypeId.POWER_OF_ATTORNEY)

        assert not contract_filter.check(JSONContract(contract_data=contract.json))

    def test_deep_exclude(self):
        contract_filter = self.get_contract_filter()

        contract = ContractJSONBuilder('GENERAL')
        contract.add_collateral(commission=ContractTypeId.PARTNERSHIP)

        assert not contract_filter.check(JSONContract(contract_data=contract.json))

        contract = ContractJSONBuilder('GENERAL', version_id=1)
        contract.add_collateral(commission=ContractTypeId.POWER_OF_ATTORNEY)

        assert contract_filter.check(JSONContract(contract_data=contract.json))

    def test_deep_include(self):
        contract_filter = self.get_contract_filter()

        contract = ContractJSONBuilder('SPENDABLE')
        contract.add_collateral(is_offer=0)

        assert not contract_filter.check(JSONContract(contract_data=contract.json))

        contract = ContractJSONBuilder('SPENDABLE', version_id=1)
        contract.add_collateral(is_offer=1)

        assert contract_filter.check(JSONContract(contract_data=contract.json))

    def test_like_include(self):
        contract_filter = self.get_contract_filter()

        contract = ContractJSONBuilder('AFISHA')
        contract.add_collateral(is_offer=0)

        assert contract_filter.check(JSONContract(contract_data=contract.json))

    def test_equal(self):
        contract_filter = self.get_contract_filter()

        contract = ContractJSONBuilder('PARTNERS')
        contract.add_collateral(is_offer=0)

        assert contract_filter.check(JSONContract(contract_data=contract.json))

    def test_ne(self):
        contract_filter = ContractFilter({'kind': {'ne': 'PARTNERS'}})

        contract = ContractJSONBuilder('PARTNERS')
        contract.add_collateral(is_offer=0)

        assert not contract_filter.check(JSONContract(contract_data=contract.json))

        contract = ContractJSONBuilder('AFISHA')
        contract.add_collateral(is_offer=0)

        assert contract_filter.check(JSONContract(contract_data=contract.json))

    def test_gt(self):
        contract_filter = ContractFilter({'version_id': {'gt': 1}})

        contract = ContractJSONBuilder('PARTNERS', version_id=1)
        assert not contract_filter.check(JSONContract(contract_data=contract.json))

        contract = ContractJSONBuilder('PARTNERS', version_id=2)
        assert contract_filter.check(JSONContract(contract_data=contract.json))

    def test_ge(self):
        contract_filter = ContractFilter({'version_id': {'ge': 1}})

        contract = ContractJSONBuilder('PARTNERS', version_id=0)
        assert not contract_filter.check(JSONContract(contract_data=contract.json))

        contract = ContractJSONBuilder('PARTNERS', version_id=1)
        assert contract_filter.check(JSONContract(contract_data=contract.json))

    def test_lt(self):
        contract_filter = ContractFilter({'version_id': {'lt': 2}})

        contract = ContractJSONBuilder('PARTNERS', version_id=2)
        assert not contract_filter.check(JSONContract(contract_data=contract.json))

        contract = ContractJSONBuilder('PARTNERS', version_id=1)
        assert contract_filter.check(JSONContract(contract_data=contract.json))

    def test_le(self):
        contract_filter = ContractFilter({'version_id': {'le': 2}})

        contract = ContractJSONBuilder('PARTNERS', version_id=3)
        assert not contract_filter.check(JSONContract(contract_data=contract.json))

        contract = ContractJSONBuilder('PARTNERS', version_id=2)
        assert contract_filter.check(JSONContract(contract_data=contract.json))


class TestNotifyFilter:
    def get_notify_filter(self):
        return NotifyFilter([
            {
                'name': 'booked',
                'changed_to': {'is_booked': True}
            },
            {
                'name': 'faxed',
                'current': {'is_booked': False},
                'changed_to': {'is_faxed': True}
            },
            {
                'name': 'signed',
                'prev': {'is_signed': False},
                'current': {'is_signed': True}
            }
        ])

    def test_changed_to_notify(self):
        notify_filter = self.get_notify_filter()

        contract = ContractJSONBuilder('GENERAL')
        contract.add_collateral(is_booked=0, is_signed='2021-08-02T12:00:00')

        prev = JSONContract(contract_data=contract.json)

        contract = ContractJSONBuilder('GENERAL', version_id=1)
        contract.add_collateral(is_booked=1, is_signed='2021-08-02T12:10:00')

        last = JSONContract(contract_data=contract.json)

        assert notify_filter.check(last, prev) == [('0', 'booked')]

    def test_current_prev_notify(self):
        notify_filter = self.get_notify_filter()

        contract = ContractJSONBuilder('GENERAL')
        contract.add_collateral(is_booked=0)
        prev = JSONContract(contract_data=contract.json)

        contract = ContractJSONBuilder('GENERAL', version_id=1)
        contract.add_collateral(is_booked=0, is_signed='2021-08-02T12:10:00')
        last = JSONContract(contract_data=contract.json)

        assert notify_filter.check(last, prev) == [('0', 'signed')]

    def test_by_different_properties(self):
        notify_filter = self.get_notify_filter()

        contract = ContractJSONBuilder('GENERAL')
        contract.add_collateral(is_faxed=None)
        prev = JSONContract(contract_data=contract.json)

        contract = ContractJSONBuilder('GENERAL', version_id=1)
        contract.add_collateral(is_faxed='2021-08-02T12:00:00', is_booked=0)
        last = JSONContract(contract_data=contract.json)

        assert notify_filter.check(last, prev) == [('0', 'faxed')]

    def test_diff_collatarals_length(self):
        notify_filter = self.get_notify_filter()

        contract = ContractJSONBuilder('GENERAL')
        contract.add_collateral(id=100, is_booked=1)
        prev = JSONContract(contract_data=contract.json)

        contract = ContractJSONBuilder('GENERAL', version_id=1)
        contract.add_collateral(id=100, is_booked=0)
        contract.add_collateral(id=101, is_booked=1)
        last = JSONContract(contract_data=contract.json)

        assert notify_filter.check(last, prev) == [('1', 'booked')]

    def test_several_notifications(self):
        notify_filter = self.get_notify_filter()

        contract = ContractJSONBuilder('GENERAL')
        contract.add_collateral(is_booked=0)
        prev = JSONContract(contract_data=contract.json)

        contract = ContractJSONBuilder('GENERAL', version_id=1)
        contract.add_collateral(is_booked=1, is_signed='2021-08-02T12:10:00')
        last = JSONContract(contract_data=contract.json)

        assert notify_filter.check(last, prev) == [('0', 'booked'), ('0', 'signed')]

    def test_collaterals_without_id(self):
        notify_filter = self.get_notify_filter()

        contract = ContractJSONBuilder('GENERAL')
        contract.add_collateral(id=100, is_booked=1)
        prev = JSONContract(contract_data=contract.json)

        contract = ContractJSONBuilder('GENERAL', version_id=1)
        contract.add_collateral(id=100, is_booked=0)
        contract.add_collateral(num='Ф-12', is_booked=1)
        last = JSONContract(contract_data=contract.json)

        assert notify_filter.check(last, prev) == [('1', 'booked')]
