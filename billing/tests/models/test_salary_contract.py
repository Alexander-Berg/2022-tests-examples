from bcl.core.models import SalaryRegistry, states


class TestRegistry:

    def test_status_to_oebs(self):
        # arrange
        r_new = SalaryRegistry(status=states.NEW)
        r_exported = SalaryRegistry(status=states.NEW)

        # act
        status_from_new = r_new.status_to_oebs()
        status_from_exported = r_exported.status_to_oebs()

        # assert
        assert status_from_new == states.NEW
        assert status_from_exported == states.NEW
