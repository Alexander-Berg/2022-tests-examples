from balance.tests.conftest import get_free_user
from balance import balance_steps as steps
from balance.real_builders import common_defaults

user = next(get_free_user(need_cleanup=False))(type='10405_manager_of_the_client_department_with_new_ui')

client_id = steps.ClientSteps.create(params=None)

steps.ClientSteps.link(client_id, user.login)

person_id = steps.PersonSteps.create(client_id, 'ur', common_defaults.FIXED_UR_PARAMS)

print user.login
print user.password
print client_id
