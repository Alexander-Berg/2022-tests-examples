import pytest
from django.utils import timezone
from freezegun import freeze_time

from staff.kudos.controllers import ADD_DELAY, AddKudoController, filter_recipients


class TestAddKudo(object):

    @pytest.mark.django_db()
    def test_filter_recipients_query_count(self, django_assert_num_queries, bootstrap_users, generate_logs):

        tester, taker1, taker2 = bootstrap_users()

        generate_logs(issuer=tester, recipient=taker1)
        generate_logs(issuer=tester, recipient=taker2)

        with freeze_time(timezone.now() + ADD_DELAY):
            with django_assert_num_queries(2):
                filtered = filter_recipients(
                    recipients=['taker1', 'taker2'], issuer=tester)

        assert filtered

    @pytest.mark.django_db()
    def test_add_kudo_query_count(self, django_assert_num_queries, bootstrap_users):

        tester, taker1, taker2 = bootstrap_users()

        add_entry = AddKudoController.add_kudo

        with django_assert_num_queries(7):
            # Худший случай: вариант, когда у получателей ещё нет Stat.
            # 2 шт - фильтрация и получение пользователей
            # 1 шт - вычисление силы благодарения
            # 1 шт - вставка записей в журнал
            # 1 шт - получение неинициализированных пользователей
            # 1 шт - инициализация показателей благодарностей
            # 1 шт - обновление показателей благодарностей
            assert add_entry(issuer=tester, recipients=['taker1', 'taker2'])

        with freeze_time(timezone.now() + ADD_DELAY):
            with django_assert_num_queries(6):
                # Лучший случай (не нужно инициализировать показатели благодарностей).
                assert add_entry(issuer=tester, recipients=['taker1', 'taker2'])
