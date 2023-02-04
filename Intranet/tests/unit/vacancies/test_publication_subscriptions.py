import pytest

from intranet.femida.src.vacancies.tasks import send_publication_subscriptions_digest

from intranet.femida.tests import factories as f


@pytest.mark.parametrize('subscriptions_number, additional_queries_number', ((1, 1), (5, 5)))
def test_publication_subscription_task(django_assert_num_queries, subscriptions_number,
                                       additional_queries_number):
    f.PublicationSubscriptionFactory.create_batch(subscriptions_number)

    # запросы на получение subscriptions и prefetch связанных сущностей
    base_queries_number = 7
    with django_assert_num_queries(base_queries_number + additional_queries_number):
        send_publication_subscriptions_digest()
