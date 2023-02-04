from intranet.femida.src.notifications import problems as notifications

from intranet.femida.tests import factories as f


def test_problem_created():
    instance = f.create_problem_with_moderators()
    initiator = f.UserFactory()

    notification = notifications.ProblemCreatedNotification(instance, initiator)
    notification.send()


def test_problem_updated():
    old_instance = f.create_problem_with_moderators()
    instance = f.create_problem_with_moderators()
    initiator = f.UserFactory()

    notification = notifications.ProblemUpdatedNotification(
        instance=instance,
        initiator=initiator,
        old_instance=old_instance,
        old_categories=old_instance.categories.all(),
    )
    notification.send()


def test_complaint_created():
    problem = f.create_problem_with_moderators()
    instance = f.ComplaintFactory(problem=problem)
    initiator = f.UserFactory()

    notification = notifications.ComplaintCreatedNotification(instance, initiator)
    notification.send()
