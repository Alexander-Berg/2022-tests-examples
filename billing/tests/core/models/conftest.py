import pytest


@pytest.fixture
def assert_startrek_create_update(response_mock):
    """Создаёт или обновляет задачу в трекере по данным указаннойго объекта модели."""

    def assert_startrek_create_update_(*, obj, issue_key: str, bypass: bool = False, update_only: bool = False):

        assert not obj.issue

        if update_only:
            # Для отладки удобно не пложить множество тестовых задач,
            # а смотреть на изменения в рамках одной и той же.
            obj.issue = issue_key

        else:

            # Публикация новой задачи.
            with response_mock(
                    f'POST https://st-api.test.yandex-team.ru/v2/issues -> 200 :{{"key": "{issue_key}"}}',
                    bypass=bypass
            ):
                result = obj.startrek_issue.create_or_update()
                assert result

            obj.refresh_from_db()

            if bypass:
                assert obj.issue
                issue_key = obj.issue

            else:
                assert obj.issue == issue_key

        # Обновление существующей задачи.
        with response_mock(
                f'PATCH https://st-api.test.yandex-team.ru/v2/issues/{issue_key} -> 200 :{{"fake": "ok"}}',
                bypass=bypass
        ):
            result = obj.startrek_issue.create_or_update()
            assert result

        # Трекер не смог - обновление задачи.
        with response_mock(
                f'PATCH https://st-api.test.yandex-team.ru/v2/issues/{issue_key} -> 504 :',
                bypass=bypass
        ):
            result = obj.startrek_issue.create_or_update()
            assert result is None

        # Трекер не смог - получение данных задачи.
        with response_mock(
                f'GET https://st-api.test.yandex-team.ru/v2/issues/{issue_key} -> 504 :',
                bypass=bypass
        ):
            result = obj.startrek_issue.sync()
            assert not result

    return assert_startrek_create_update_
