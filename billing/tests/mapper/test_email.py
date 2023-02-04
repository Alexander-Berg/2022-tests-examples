import json

from billing.apikeys.apikeys import mapper


def test_enqueue_mail(mongomock, simple_link):
    user = simple_link.project.user
    contract_id = 123
    event_type = 'some_event'
    mapper.EventEmailTask.enqueue_mail(event_type, user, simple_link, contract_id=contract_id)

    task = mapper.Task.objects(_cls='Task.SenderTask.TransactionalSenderTask').first()
    params = {
        'template_names': [mapper.EventEmail.COMMON_TEMPLATE_NAME],
        'email_params': dict(user_uid=user.uid, link_id=simple_link.id, contract_id=contract_id, event_type=event_type),
    }
    assert json.dumps(task.context.params, sort_keys=True) == json.dumps(params, sort_keys=True) \
           and task.context.id is not None
