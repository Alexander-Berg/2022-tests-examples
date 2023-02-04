import pytest
import sform

from staff.preprofile.forms.fields import official_fields
from staff.groups.models import GROUP_TYPE_CHOICES
from staff.lib.testing import GroupFactory


class FormWithField(sform.SForm):
    services = official_fields.abc_services()


@pytest.mark.django_db()
def test_field_accepts_services_slug_in_uppercase():
    # given
    GroupFactory(code='startrek', type=GROUP_TYPE_CHOICES.SERVICE, intranet_status=1)
    form = FormWithField(data={'services': ['STARTREK']})

    # when
    result = form.is_valid()

    # then
    assert result
