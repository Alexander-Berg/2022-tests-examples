import maps.carparks.libs.common.py as tested_module

import unittest


def test_is_parking_restricted():
    assert not tested_module.is_parking_restricted(tested_module.CarparkType.Free)
    assert tested_module.is_parking_restricted(tested_module.CarparkType.Prohibited)


if __name__ == "__main__":
    unittest.main()
