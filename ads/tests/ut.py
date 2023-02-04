import json
import re

import pytest
from library.python import resource

from six.moves import urllib

from ads.emily.storage.models.tsars.lib.fabrics import get_tsar_model_by_id
from ads.emily.storage.models.tsars.lib.info import (
    user_vector_name,
    user_vector_name_impl,
    get_vector_ids_impl,
)
from ads.emily.storage.models.tsars.models import tsar_models


def test_tsar_model_id_fabric():
    model = get_tsar_model_by_id(15)
    assert model.tsar_model_id == 15
    assert model.name == "rsya_ctr_qce_alxmopo3ov_robdrynkin_ya_philya_15"


def is_valid_url(url):
    try:
        parsed = urllib.parse.urlparse(url)
        return all([parsed.scheme, parsed.netloc])
    except ValueError:
        return False


@pytest.mark.parametrize("model", tsar_models)
def test_links(model):
    assert (model.meta is not None) or (not model.enabled)
    if model.meta is None:
        return
    for name, link in model.meta.links.items():
        if len(link) == 0:
            continue
        assert is_valid_url(link), "Invalid {} url {} in {} model".format(name, link, model.name)


def test_unique_tsar_model_ids():
    ids = [model.tsar_model_id for model in tsar_models]
    for x in ids:
        assert ids.count(x) == 1, "Id {} occurs {} times in models list".format(x, ids.count(x))


def test_unique_names():
    names = [model.name for model in tsar_models]
    for x in names:
        assert names.count(x) == 1, "Name {} occurs {} times in models list".format(names, names.count(x))


@pytest.mark.parametrize("model", tsar_models)
def test_owners_format(model):
    pattern = re.compile(r"^@[A-Z0-9_-]+$", re.IGNORECASE)
    owners = model.owners
    assert len(owners) > 0
    for owner in owners:
        assert re.match(pattern, owner), "unexpected owner name {} in {} model".format(owner, model.name)


@pytest.fixture
def tsars_json():
    return json.loads(resource.find("/tsar_model.json"))


def test_check_tsar_model(tsars_json):
    expecteds_ids = set(tsar["TsarModelID"] for tsar in tsars_json)
    real_ids = set(model.tsar_model_id for model in tsar_models)

    tsar_without_meta_ids = expecteds_ids - real_ids
    def ids_info(ids):
        uv = ids["UserVersion"]
        ids["UserVersion"] = '{} ({})'.format(uv, user_vector_name_impl(uv))
        return str(ids)
    message = "\n".join(
        ids_info(get_vector_ids_impl(tsar_model_id))
        for tsar_model_id in sorted(tsar_without_meta_ids)
    )
    assert len(tsar_without_meta_ids) == 0, "Add meta info for tsars:\n{}".format(message)


def test_get_user_vector_names():
    model = get_tsar_model_by_id(1)
    assert user_vector_name(model) == "JAMSHID_DSSM_CTR"
