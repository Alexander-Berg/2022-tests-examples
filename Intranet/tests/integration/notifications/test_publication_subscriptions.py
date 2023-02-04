from intranet.femida.src.notifications.publication_subscriptions import (
    PublicationSubscriptionNotification,
)

from intranet.femida.tests import factories as f


def test_publication_subscription_digest():
    subscription = f.PublicationSubscriptionFactory()
    vacancies = f.VacancyFactory.create_batch(3)

    notification = PublicationSubscriptionNotification(
        instance=subscription,
        vacancies=vacancies,
        query_params='',
    )
    notification.send()
