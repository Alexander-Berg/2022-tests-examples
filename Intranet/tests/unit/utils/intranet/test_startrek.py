from plan.common.utils import startrek


def test_parse_filter():
    samples = (
        [
            'https://st.yandex-team.ru/filters/order:updated:false/filter?assignee=printsesso',
            {'filter': {'assignee': 'printsesso'}}
        ],
        [
            'https://st.yandex-team.ru/filters/filter?query=queue%3A%20TEST%20created%3A%20>%3D%2001.03.2018',
            {'query': 'queue: TEST created: >= 01.03.2018'}
        ],
        [
            'https://st.yandex-team.ru/filters/filter?assignee=uruz&query=a%3Db',
            {'query': 'a=b', 'filter': {'assignee': 'uruz'}}
        ],
        [
            'https://st.yandex-team.ru/filters/order:updated:false/filter?assignee=uruz&abcService=483',
            {'filter': {'abcService': '483', 'assignee': 'uruz'}}
        ],
        [
            'https://st.yandex-team.ru/filters/filter:28101',
            {'filter_id': 28101}
        ],
        [
            'https://st.yandex-team.ru/filters/filter:2?assignee=printsesso&query=queue%3A%20TEST',
            {'filter_id': 2, 'filter': {'assignee': 'printsesso'}, 'query': 'queue: TEST'}
        ],
        [
            'https://st.yandex-team.ru/filters/filter?components=36312%7C38374',
            {'filter': {'components': ['36312', '38374']}}
        ],
        [
            'https://st.yandex-team.ru/filters/filter?query=QUEUE%3A%20ABC%20TYPE%3A%D0%A0%D0%B5%D0%BB%D0%B8%D0%B7%20',
            {'query': 'QUEUE: ABC TYPE:Релиз '}
        ],
    )

    for filter, result in samples:
        assert startrek._parse_filter(filter) == result
