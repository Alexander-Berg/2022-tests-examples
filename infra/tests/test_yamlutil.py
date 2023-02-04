from infra.ya_salt.lib import yamlutil

EXPECTED_PRETTIFY = """saltversioninfo:
  - 2016
  - 3
  - 4
  - 0
"""


def test_prettify():
    assert yamlutil.prettify({'saltversioninfo': [2016, 3, 4, 0]}) == EXPECTED_PRETTIFY
