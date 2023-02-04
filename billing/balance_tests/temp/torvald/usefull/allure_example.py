__author__ = 'torvald'

import btestlib.reporter as reporter

from balance import balance_steps as steps


def test_some_allure_stuff():
    print('before step1')
    with reporter.step('step onewwwww'):
        client_id = steps.ClientSteps.create()
    with reporter.step('step twawwww'):
        steps.PersonSteps.create(client_id, 'ur')
    print('tear_down')