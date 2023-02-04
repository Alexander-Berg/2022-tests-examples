# coding: utf-8
import pytest

from review.notifications import views

from tests import helpers


@pytest.mark.parametrize('template', list(views.get_tpl_to_notification()))
def test_dev_template_rendered_ok(template, client):
    helpers.get(
        client,
        path='/notifications/%s/' % template,
    )
