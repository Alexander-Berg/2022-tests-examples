# coding: utf-8


import logging

from django.conf.urls import url
from django.core import management
from tastypie.authorization import Authorization
from tastypie.exceptions import BadRequest
from tastypie.utils.urls import trailing_slash

from idm.api.frontend.base import FrontendApiResource
from idm.core.management.base import IdmBaseCommand
from idm.utils import json

log = logging.getLogger(__name__)


class CommandResource(FrontendApiResource):
    """
    Ручка, дергающая произвольную management команду на тестинге
    """
    class Meta:
        object_class = None
        resource_name = 'commands'
        allowed_methods = ('post',)
        authorization = Authorization()

    def prepend_urls(self):
        return [
            url(r'^(?P<resource_name>%s)%s$' %
                (self._meta.resource_name, trailing_slash()), self.wrap_view('run'),
                name='testapi_command'),
        ]

    def get_command(self, name):
        try:
            app_name = management.get_commands()[name]
        except KeyError:
            raise management.CommandError("Unknown command: '%s'" % name)

        if isinstance(app_name, management.BaseCommand):
            # If the command is already loaded, use it directly.
            command = app_name
        else:
            command = management.load_command_class(app_name, name)

        # Simulate argument parsing to get the option defaults (see #10080 for details).
        parser = command.create_parser('', name)
        return command, parser

    def run(self, request, **kwargs):
        data = json.loads(request.body)
        command_name = data.get('command', None)
        args = data.get('args', [])
        kwargs = data.get('kwargs', {})

        if not command_name:
            raise BadRequest('you must send command name in appropriate format')

        log.info('calling command "%s" with args "%s" and kwargs "%s"', command_name, args, kwargs)

        try:
            command, parser = self.get_command(command_name)
            actions = {action.dest: action for action in parser._actions}
            kwargs = {key: parser._get_value(actions[key], value) for key, value in kwargs.items()}
            if isinstance(command, IdmBaseCommand):
                kwargs['use_block_lock'] = True
            management.call_command(command_name, *args, **kwargs)
        except management.base.CommandError as e:
            if 'Unknown command' in str(e):
                return self.create_response(request, {'status': 'not found', 'message': str(e)}, status=400)
            else:
                return self.create_response(request, {'status': 'error', 'message': str(e)}, status=500)
        except Exception as e:
            log.exception('Command %s raised exception', command_name)
            return self.create_response(request, {'status': 'error', 'message': str(e)}, status=500)

        return self.create_response(request, {'status': 'ok'})
