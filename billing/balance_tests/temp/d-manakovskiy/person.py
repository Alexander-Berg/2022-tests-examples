from balance.snout_steps.api_steps import pull_handle
from btestlib.data.snout_constants import Handles

person_id = 19208044
status, data = pull_handle(Handles.PERSON, object_id=person_id)

assert 'purchase_order' in data['data']
