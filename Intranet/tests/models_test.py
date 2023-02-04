from mock import patch

from django.conf import settings


@patch(
    'staff.emission.django.emission_master.models.settings.REPLICATED_MODELS',
    settings.PROD_EMISSION_MASTER_REPLICATED_MODELS
)
def test_init_logged_models():
    from staff.emission.django.emission_master.models import init_logged_models, logged_models, settings as em_settings

    _logged_models = set(logged_models)

    init_logged_models()

    assert len(em_settings.REPLICATED_MODELS) == len(logged_models)

    pop_items = logged_models - _logged_models
    for item in pop_items:
        logged_models.discard(item)
