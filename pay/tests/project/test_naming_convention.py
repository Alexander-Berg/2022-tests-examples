import warnings


def test_naming_convention(allowed_ids, warn_ids, id):
    """Stage id should be in allowed by naming convention"""

    if id in warn_ids:
        warnings.warn(UserWarning("Id {} not matched named convention").__format__(id))

    assert id in allowed_ids
