import pytest

from django.core.urlresolvers import reverse

from intranet.audit.src.core import models


@pytest.fixture
def control_step_two(db, author, control_test):
    return models.ControlStep.objects.create(
        author=author,
        step='some step two',
        control_test=control_test,
    )


@pytest.fixture
def control_step_three(db, author, control_test):
    return models.ControlStep.objects.create(
        author=author,
        step='some step three',
        control_test=control_test,
    )


@pytest.fixture
def control_step_four(db, author, control_test):
    return models.ControlStep.objects.create(
        author=author,
        step='some step four',
        control_test=control_test,
    )


def test_move_controlstep_after_success(db, client, control_test,
                                        control_step, control_step_two, control_step_three,
                                        ):
    url = reverse("api_v1:controlstep_move", kwargs={'pk': control_step.id,
                                                     'action': 'move-after',
                                                     'related_pk': control_step_three.id,
                                                     })
    response = client.post(url)
    assert response.status_code == 200

    order = control_test.get_controlstep_order()
    assert list(order) == [control_step_two.id, control_step_three.id, control_step.id]


def test_move_controlstep_before_success(db, client, control_test,
                                         control_step, control_step_two, control_step_three,
                                         ):
    url = reverse("api_v1:controlstep_move", kwargs={'pk': control_step.id,
                                                     'action': 'move-before',
                                                     'related_pk': control_step_three.id,
                                                     })
    response = client.post(url)
    assert response.status_code == 200

    order = control_test.get_controlstep_order()
    assert list(order) == [control_step_two.id, control_step.id, control_step_three.id]


def test_move_controlstep_in_middle_success(db, client, control_test,
                                            control_step, control_step_two,
                                            control_step_three, control_step_four,
                                            ):
    url = reverse("api_v1:controlstep_move", kwargs={'pk': control_step.id,
                                                     'action': 'move-before',
                                                     'related_pk': control_step_three.id,
                                                     })
    response = client.post(url)
    assert response.status_code == 200

    order = control_test.get_controlstep_order()
    assert list(order) == [control_step_two.id, control_step.id, control_step_three.id, control_step_four.id]


def test_move_controlstep_wrong_id_fail(db, client, control_test,
                                        control_step, control_step_two,
                                        ):
    url = reverse("api_v1:controlstep_move", kwargs={'pk': 500,
                                                     'action': 'move-before',
                                                     'related_pk': control_step_two.id,
                                                     })
    response = client.post(url)
    assert response.status_code == 409
    assert response.json()['message'] == ["Request with invalid pk was made"]


def test_move_controlstep_wrong_controltest_fail(db, client, control_test,
                                                 control_step, control_step_two,
                                                 ):
    control_step_two.control_test = None
    control_step_two.save()
    url = reverse("api_v1:controlstep_move", kwargs={'pk': control_step.id,
                                                     'action': 'move-before',
                                                     'related_pk': control_step_two.id,
                                                     })
    response = client.post(url)
    assert response.status_code == 409
    assert response.json()['message'] == [("Request with pk's related to different ControlTest "
                                           "was made or passed related_pk does not exists")]


def test_move_controlstep_wrong_action_fail(db, client, control_test,
                                            control_step, control_step_two,
                                            ):
    url = reverse("api_v1:controlstep_move", kwargs={'pk': control_step.id,
                                                     'action': 'move-away',
                                                     'related_pk': control_step_two.id,
                                                     })
    response = client.post(url)
    assert response.status_code == 409
    assert response.json()['message'] == ["Bad action was passed"]


def test_move_controlstep_no_controltest_fail(db, client, control_test,
                                              control_step, control_step_two,
                                              ):
    control_step.control_test = None
    control_step.save()
    url = reverse("api_v1:controlstep_move", kwargs={'pk': control_step.id,
                                                     'action': 'move-before',
                                                     'related_pk': control_step_two.id,
                                                     })
    response = client.post(url)
    assert response.status_code == 409
    assert response.json()['message'] == ["There is no related ControlTest"]
