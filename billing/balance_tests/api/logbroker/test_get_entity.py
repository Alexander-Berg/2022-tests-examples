import pytest

from balance.api.logbroker import get_entity
from balance.exc import ENTITY_LOOKUP_ERROR


@pytest.fixture()
def set_logbroker_config(session):
    def _set_logbroker_config(config):
        session.config.set(
            "LOGBROKER_COMMON", config, column_name="value_json_clob", can_create=True
        )
        session.flush()

    return _set_logbroker_config


@pytest.mark.parametrize(
    "query,config,result",
    [
        (("Test", "test_entity_id"), None, ENTITY_LOOKUP_ERROR),
        (
            ("Test", "test_entity_id"),
            {"dev": {"Test": {"test_entity_id": 1}}},
            1,
        ),
        (("Topic", "contract"), None, "balance/dev/contract"),
        (
            ("Topic", "contract"),
            {"dev": {"Topic": {"contract": "balance/dev/contract_from_t_config"}}},
            "balance/dev/contract_from_t_config",
        ),
    ],
)
def test_get_entity(query, config, result, set_logbroker_config, session):
    set_logbroker_config(config)
    # the results of get_entity are cached
    get_entity.clear_cache()
    if isinstance(result, type) and issubclass(result, Exception):
        with pytest.raises(result):
            get_entity(*query)
    else:
        assert get_entity(*query) == result
