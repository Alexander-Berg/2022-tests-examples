import pytest

from smarttv.droideka.utils.chaining import RangedChunkDataSource, FilterableDataSource


@pytest.fixture
def test_data():
    return get_test_data()


def get_test_data():
    return {
        'name': 'top level object',
        'top_list': [
            {
                'name': 'nested object 1',
                'nested_list': [i for i in range(0, 60)]
            },
            {
                'name': 'nested object 2',
                'nested_list': [i for i in range(60, 70)]
            },
            {
                'name': 'nested object 3',
                'nested_list': [i for i in range(70, 75)]
            },
            {
                'name': 'nested object 4',
                'nested_list': [i for i in range(75, 120)]
            },
            {
                'name': 'nested object 5',
                'nested_list': [i for i in range(120, 170)]
            },
            {
                'name': 'nested object 6',
                'nested_list': [i for i in range(170, 210)]
            },
            {
                'name': 'nested object 7',
                'nested_list': [i for i in range(210, 240)]
            },
            {
                'name': 'nested object 8',
                'nested_list': [i for i in range(240, 260)]
            },
            {
                'name': 'nested object 8',
                'nested_list': [i for i in range(260, 270)]
            },
        ]
    }


def get_test_data_nested_item(index):
    return get_test_data().get('top_list')[index]


def get_items(*indexes):
    return [get_test_data_nested_item(index) for index in list(indexes)]


class TopListDataSource(RangedChunkDataSource):
    """
    Responsible for loading top-level items
    """

    def __init__(self, data, enable_filter=False, offset=0, limit=5):
        super().__init__(offset, limit)
        self.data = data
        self.enable_filter = enable_filter

    def get_limit(self) -> int:
        return self.limit

    def load_chunk(self, chunk_params) -> dict:
        limit = chunk_params['limit']
        offset = chunk_params['offset']
        lst = self.data.get('top_list')
        end = min(offset + limit, len(lst))
        if offset >= end:
            return {'top_list': []}
        data = self.data.copy()
        data['top_list'] = lst[offset:end]
        return data

    @property
    def root_list_field_name(self):
        return 'top_list'


class TopListFilterableDataSource(FilterableDataSource):
    def __init__(self, data, offset, limit, requested_amount, nested_filter_impl=None) -> None:
        super().__init__(offset, limit, requested_amount)
        self.data = data
        self.nested_filterable_source_implementation = nested_filter_impl

    def get_filters(self) -> list:
        return [
            # discard  items № 2, 3, 9
            lambda top_list: [item for item in top_list if len(item.get('nested_list')) > 15]
        ]

    def wrap_root_list(self, container, target_list, pagination_state_params):
        container['top_list'] = target_list
        return container

    def get_root_list(self, container) -> list:
        return container.get('top_list', [])

    def get_nested_list(self, nested_item):
        return nested_item.get('nested_list', [])

    def get_initial_additional_params(self):
        return None

    def extract_additional_params(self, page):
        return None

    def get_next_page(self, offset, limit, additional_params=None):
        return TopListDataSource(self.data, offset=offset, limit=limit).get_result()

    def create_nested_filterable_source(self, nested_item):
        return self.nested_filterable_source_implementation(nested_item)

    @property
    def has_nested_filterable_data_source(self):
        return False

    @property
    def max_empty_objects(self):
        return 999  # hacky fix for old tests. It's value bigger enoguh, than amount of filtered items in test data


class NumbersFilterableDataSource(FilterableDataSource):

    def get_filters(self) -> list:
        if self.number_filter:
            return [self.number_filter]
        return []

    def wrap_root_list(self, container, target_list, pagination_state_params):
        return {'list': target_list}

    def get_root_list(self, container) -> list:
        return container['list']

    def get_nested_list(self, nested_item):
        return None

    def get_initial_additional_params(self):
        return None

    def extract_additional_params(self, page):
        return None

    def get_next_page(self, offset, limit, additional_params=None):
        return {'list': self.number_list['list'][offset:offset + limit]}

    def create_nested_filterable_source(self, nested_item):
        return None

    @property
    def empty_object_threshold(self):
        return self.percentage or super().empty_object_threshold

    def __init__(self,
                 offset,
                 limit,
                 requested_amount,
                 auto_load_enabled=True,
                 number_list=None,
                 number_filter=None,
                 percentage=None):
        super().__init__(offset, limit, requested_amount, auto_load_enabled)
        self.number_list = number_list
        self.number_filter = number_filter
        self.percentage = percentage


# (offset, limit, size)
TOP_LIST_LOAD_TEST_PARAMS = [(0, 1, 1), (2, 3, 3), (2, 5, 5), (5, 5, 4), (7, 5, 2), (8, 5, 1), (9, 5, 0)]

# (offset, limit, requested_amount, [result items])
TOP_LIST_FILTER_TEST_PARAMS = [
    (0, 1, 1, get_items(0)),
    (7, 1, 1, get_items(7)),
    (8, 1, 1, []),
    (9, 1, 1, []),
    (1, 1, 1, get_items(3)),
    (0, 5, 1, get_items(0)),
    (0, 1, 5, get_items(0, 3, 4, 5, 6)),
    (5, 2, 5, get_items(5, 6, 7)),
]


def number_filter_less_than_4(root_list):
    return [item for item in root_list if item < 4]


def remove_any_non_negative_number_filter(root_list):
    return [item for item in root_list if item < 0]


# valid negative numbers are too far(more than 1 query with empty response) from valid numbers 1, 2, 3
# so negative number should not be included in result
NUMBER_LIST_1 = [1, 2, 3, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, -1, -2, -3, -4]
NUMBER_LIST_2 = [1, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3]
# (data, expected_resuylt, filter)
FINITE_EMPTY_RESPONSE_TEST_DATA = (
    ({'list': NUMBER_LIST_1}, [], remove_any_non_negative_number_filter),
    ({'list': NUMBER_LIST_1}, [1, 2, 3], number_filter_less_than_4),
    ({'list': NUMBER_LIST_2}, [1, 2, 3, 3, 3], number_filter_less_than_4)
)

NUMBER_LIST_WITH_SMALL_PERCENTAGE = [1, 4, 4, 4, 4, 4, 4, 4, 4, 2, 4, 3, 4, 4, 4, 0, 4, 4, 4, 4]
NUMBER_LIST_WITH_BIG_PERCENTAGE = [1, 1, 1, 4, 4, 2, 2, 4, 4, 2, 4, 3, 4, 4, 4, 0, 4, 4, 4, 4]

EMPTY_RESPONSE_PERCENTAGE_TEST_DATA = (
    # in this list there are responses with only 1 item
    # i.e. (1), (2), (3), (0) and result would be [1, 2, 3, 0]
    # but since percentage in every response is 30%-100% - then they will be considered as empty
    # so, the real result will be [1, 2] (limit = 3, requested_amount =5)
    ({'list': NUMBER_LIST_WITH_SMALL_PERCENTAGE}, [1, 2], number_filter_less_than_4, 0.60),
    # this is opposite situation
    # valid number slices: (1, 1, 1), (2, 2, 2), (3), (0)
    # (1, 1, 1) and (2, 2, 2) has percentage 66%, they will not be considered as empty
    # the result will be [1, 1, 1, 2, 2] (limit = 3, requested_amount =5)
    ({'list': NUMBER_LIST_WITH_BIG_PERCENTAGE}, [1, 1, 1, 2, 2], number_filter_less_than_4, 0.1)
)


@pytest.mark.parametrize('offset, limit, size', TOP_LIST_LOAD_TEST_PARAMS)
def test_load_top_list_with_limit_offset(test_data, offset, limit, size):
    result = TopListDataSource(test_data, offset=offset, limit=limit).get_result()
    assert len(result.get('top_list')) == size


@pytest.mark.parametrize('offset, limit, requested_amount, items', TOP_LIST_FILTER_TEST_PARAMS)
def test_filtering_top_list(test_data, offset, limit, requested_amount, items):
    result = TopListFilterableDataSource(test_data, offset=offset, limit=limit,
                                         requested_amount=requested_amount).get_result()
    assert result.get('top_list') == items


@pytest.mark.parametrize('input_data, expected_result, number_filter', FINITE_EMPTY_RESPONSE_TEST_DATA)
def test_finite_empty_response(input_data, expected_result, number_filter):
    ds = NumbersFilterableDataSource(offset=0,
                                     limit=5,
                                     requested_amount=5,
                                     number_list=input_data,
                                     number_filter=number_filter)
    actual_result = ds.get_result()
    assert actual_result['list'] == expected_result


@pytest.mark.parametrize('input_data, expected_result, number_filter, percentage', EMPTY_RESPONSE_PERCENTAGE_TEST_DATA)
def test_finite_empty_response_with_percentage(input_data, expected_result, number_filter, percentage):
    ds = NumbersFilterableDataSource(offset=0,
                                     limit=5,
                                     requested_amount=5,
                                     number_list=input_data,
                                     number_filter=number_filter,
                                     percentage=percentage)
    actual_result = ds.get_result()
    assert actual_result['list'] == expected_result
