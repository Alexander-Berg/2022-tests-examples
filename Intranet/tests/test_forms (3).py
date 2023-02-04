import pytest

from django.core.urlresolvers import reverse
from django.core.files.uploadedfile import SimpleUploadedFile
from django.forms.widgets import ClearableFileInput
from django.forms.models import model_to_dict

from staff.lib.auth.utils import get_or_create_test_user

from staff.navigation.forms import NavigationEditForm, NavigationLink
from staff.navigation.models import Navigation
from staff.navigation.controller import encode_icon


ICON_SVG = b'''<svg
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns="http://www.w3.org/2000/svg"
    xmlns:cc="http://web.resource.org/cc/"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:svg="http://www.w3.org/2000/svg"
    id="svg2441"
    viewBox="0 0 143.85 236.94"
    version="1.0">
<line
    x1="0"
    y1="0"
    x2="200"
    y2="200"
    stroke-width="1"
    stroke="rgb(0,0,0)"></line>
</svg>
'''


@pytest.fixture
def navigation(db):
    navigation = Navigation(
        is_main=True,
        position=1,
        name='name_ru',
        name_en='name_en',
        is_active=True,
        url='http://somehost.net',
        title='title_ru',
        title_en='title_en',
        description='Test description Lorem Ipsum',
        application_id='name_en',
    )
    navigation.save()
    return navigation


def test_url(navigation):
    link = NavigationLink(instance=navigation)
    url = reverse('navigation:get_icon', args=[navigation.id])
    assert url == str(link)


def test_update_last_modifier(navigation, superuser_client):
    assert navigation.last_modifier is None
    post_data = model_to_dict(navigation)
    su = get_or_create_test_user()
    su.is_staff = True
    su.save()

    save_url = reverse(
        'admin:navigation_navigation_change',
        args=(navigation.id,)
    )

    superuser_client.post(save_url, post_data)
    navigation = Navigation.objects.get(pk=navigation.pk)

    assert navigation.last_modifier == get_or_create_test_user().get_profile()


def test_store_icon(navigation):
    icon_filename = 'icon.svg'
    navigation_dict = model_to_dict(navigation)
    icon = SimpleUploadedFile(icon_filename, ICON_SVG)
    form = NavigationEditForm(navigation_dict, instance=navigation, files={'icon': icon})

    assert form.is_valid(), form.errors
    assert isinstance(form.fields['icon'].widget, ClearableFileInput)

    result_instance = form.save()

    assert result_instance.icon == encode_icon(ICON_SVG)
