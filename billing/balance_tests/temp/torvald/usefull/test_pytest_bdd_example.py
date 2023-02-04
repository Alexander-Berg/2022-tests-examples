from pytest_bdd import scenario, given, when, then, parsers

from balance import balance_steps as steps


@scenario('pytest_bdd_example.feature', 'First try')
def test_arguments():
    pass


# @given(parsers.parse('there are TTT name'))
# # def specify_name(name):
# #     return dict(name=name)
# def specify_name():
#     return 'custom_name'


@given(parsers.parse('there are client'))
def client_id():
    return steps.ClientSteps.create()


@when(parsers.parse('I create new person'))
def person_id(client_id):
    return steps.PersonSteps.create(client_id, 'ur')


@then(parsers.parse('I should have new person'))
def check_person(client_id):
    assert client_id > 0