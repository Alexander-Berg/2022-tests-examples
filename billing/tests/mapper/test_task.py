from billing.apikeys.apikeys import mapper


def test_sender_task(mongomock, simple_link):
    task = mapper.EventEmailTask.enqueue_mail('test_event', simple_link.project.user, simple_link)
    assert mapper.Task.objects(_cls='Task.SenderTask.TransactionalSenderTask', context__id=task.context.id).count() == 1
