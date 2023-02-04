import pytest

import yp.data_model as data_model
from infra.rsc.src.model.validation import validate_pod_set_labels
import infra.rsc.src.lib.podutil as podutil


def test_validate_pod_set_labels():
    ps1 = data_model.TPodSet()
    r = ps1.labels.attributes.add()
    r.key = "environ"
    r.value = "dev"
    r = ps1.labels.attributes.add()
    r.key = "some_other_attribute"
    r.value = "value"

    ps2 = data_model.TPodSet()
    r = ps2.labels.attributes.add()
    r.key = "environ"
    r.value = "pre"

    match_labels = podutil.make_labels_from_dict({"environ": "dev"})

    validate_pod_set_labels(ps1, match_labels)

    with pytest.raises(ValueError):
        validate_pod_set_labels(ps2, match_labels)
