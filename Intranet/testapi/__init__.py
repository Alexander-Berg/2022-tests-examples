# -*- coding: utf-8 -*-


from tastypie.api import Api

from idm.api.frontend import batch, rolerequest
from idm.api.testapi import command, role, system, user, debug

api = Api(api_name='testapi')

api.register(batch.BatchResource(api=api))
api.register(command.CommandResource())
api.register(role.RoleResource())
api.register(rolerequest.RoleRequestResource())
api.register(system.SystemResource())
api.register(user.UserResource())
api.register(debug.DebugResource())
