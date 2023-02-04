# coding: utf-8


from idm.api.frontend.forms import BaseForm
from idm.api.frontend import fields
from django.utils.translation import ugettext_lazy as _


class RoleRawUpdateForm(BaseForm):
    user = fields.UserField(label=_('Пользователи'), required=False)
    system = fields.SystemField(label=_('Система'), required=False)
    node = fields.RoleNodeSlugField(label=_('Данные роли'), required=False)

    def __init__(self, role, *args, **kwargs):
        self.role = role
        super(RoleRawUpdateForm, self).__init__(*args, **kwargs)
        self.fields['node'].system = role.system
