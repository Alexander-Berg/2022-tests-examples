import hamcrest as hm
import pytest
import yaml

from library.python import resource


@pytest.mark.parametrize('manifests', ['processor-manifests-test.yml', 'processor-manifests-prod.yml'])
def test_act_row_and_act_row_check_actions_similarity(manifests):
    manifests_text = resource.find(manifests)
    manifests_conf = yaml.safe_load(manifests_text)['manifests']
    plus_manifest = [m for m in manifests_conf if m['namespace'] == 'plus'][0]
    act_row_actions = plus_manifest['endpoints']['act-row']['actions']
    act_row_check_actions = plus_manifest['endpoints']['act-row-check']['actions']
    print(act_row_actions)
    print(act_row_check_actions)
    hm.assert_that(act_row_check_actions, hm.equal_to([a for a in act_row_actions if a.get('name') != 'write_transactions_batch']))
