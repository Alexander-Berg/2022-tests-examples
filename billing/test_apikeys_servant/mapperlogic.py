from mongoengine import Q
from pymongo.read_preferences import ReadPreference

from billing.apikeys.apikeys import mapper, rpcutil
from billing.apikeys.apikeys.http_core import HttpError
from billing.apikeys.apikeys.mapper import context as ctx


class Logic:

    @ctx.context_deco(ctx.ReadPreferenceSettings(ReadPreference.PRIMARY_PREFERRED))
    @ctx.context_deco(ctx.NoCacheSettings())
    @rpcutil.call_description({
        'user_uid': rpcutil.arg_long(mandatory=True),
    })
    def run_user_contractor(self, params):
        """
        :type params: dict
        """
        contractor = mapper.Contractor.get_new_or_exists(mapper.User.getone(uid=params['user_uid']))
        task = mapper.ContractorTask(contractor=contractor)
        with mapper.DBLock(task.extract_task_unique_name(task.entity),
                           timeout=task.EXECUTION_DELAY, tries_count=1, tries_timeout=0):
            task._do_task()

    @ctx.context_deco(ctx.ReadPreferenceSettings(ReadPreference.PRIMARY_PREFERRED))
    @ctx.context_deco(ctx.NoCacheSettings())
    @rpcutil.call_description({
        'link_id': rpcutil.arg_str(mandatory=True),
        'on_date': rpcutil.arg_date(default=None),
    })
    def run_tarifficator(self, params):
        """
        :type params: dict
        """
        link = mapper.ProjectServiceLink.getone(id=params['link_id'])
        task = mapper.TarifficatorTask(link=link)
        with mapper.DBLock(task.extract_task_unique_name(task.entity),
                           timeout=task.EXECUTION_DELAY, tries_count=1, tries_timeout=0):
            task.run_tarifficator(on_date=params['on_date'])

    @ctx.context_deco(ctx.ReadPreferenceSettings(ReadPreference.PRIMARY_PREFERRED))
    @ctx.context_deco(ctx.NoCacheSettings())
    @rpcutil.call_description({
        'key': rpcutil.arg_str(),
        'link_id': rpcutil.arg_str(),
        'service_id': rpcutil.arg_int(),
        'force': rpcutil.arg_bool(default=False),
    })
    def run_limit_checker(self, params):
        """
        :type params: dict
        """
        if not (params.get('key') or params.get('link_id')):
            raise HttpError(400, 'Fields key or link_id is mandatory.')
        if params.get('key') and not params.get('service_id'):
            raise HttpError(400, 'Fields key or service_id together is mandatory.')
        q = Q()
        if params.get('link_id'):
            q &= Q(link_id=params.get('link_id'))
        else:
            q &= Q(service_id=params.get('service_id'), key=params.get('key'))
        limit_checker = mapper.ProjectLinkLimitChecker.getone(q)
        result = limit_checker.check(force=params.get('force'))
        return {'result': result, 'limit_checker': limit_checker.to_mongo()}
