from intranet.femida.src.core.controllers import update_list_of_instances

from intranet.femida.tests.models import Table


def run_update_list_of_instances(**params):
    params['model'] = Table
    params['queryset'] = Table.objects.all()
    update_list_of_instances(**params)


def test_empty(django_assert_num_queries):
    with django_assert_num_queries(1):  # get
        run_update_list_of_instances(
            data=[],
        )


def test_not_delete_missing(django_assert_num_queries):
    Table.objects.create(id=1, int_field=1)
    with django_assert_num_queries(1):  # get
        run_update_list_of_instances(
            data=[],
            delete_missing=False,
        )


def test_only_delete(django_assert_num_queries):
    Table.objects.create(id=1, int_field=1)  # delete
    with django_assert_num_queries(2):  # get
        run_update_list_of_instances(
            data=[],
        )
    assert not Table.objects.exists()


def test_only_create(django_assert_num_queries):
    with django_assert_num_queries(2):  # get
        run_update_list_of_instances(
            data=[
                {
                    'int_field': 1,
                },  # create
            ]
        )
    assert Table.objects.count() == 1


def test_only_update(django_assert_num_queries):
    Table.objects.create(id=1, int_field=1)
    with django_assert_num_queries(2):  # get
        run_update_list_of_instances(
            data=[
                {
                    'id': 1,
                    'int_field': 2,
                },  # update
            ]
        )
    assert Table.objects.count() == 1


def test_duplicated_data(django_assert_num_queries):
    Table.objects.create(id=1, int_field=1)
    with django_assert_num_queries(2):  # get
        run_update_list_of_instances(
            data=[
                {
                    'id': 1,
                    'int_field': 2,
                },  # update
                {
                    'id': 1,
                    'int_field': 3,
                },
            ]
        )
    assert Table.objects.count() == 1
    instance = Table.objects.get(id=1)
    assert instance.int_field == 2


def test_delete_update_create(django_assert_num_queries):
    Table.objects.create(id=1, int_field=1)
    Table.objects.create(id=2, int_field=2)  # delete
    with django_assert_num_queries(4):  # get
        run_update_list_of_instances(
            data=[
                {
                    'id': 1,
                    'int_field': 11,
                },  # update
                {
                    'int_field': 3,
                },  # create
            ]
        )
    assert Table.objects.count() == 2
    assert Table.objects.filter(int_field__in=[11, 3]).count() == 2


def test_bulk_delete_and_create(django_assert_num_queries):
    Table.objects.create(id=1, int_field=1, char_field='1')  #
    Table.objects.create(id=2, int_field=2, char_field='2')  #
    Table.objects.create(id=3, int_field=3, char_field='3')  # delete
    with django_assert_num_queries(3):  # get
        run_update_list_of_instances(
            identifier=('int_field', 'char_field'),
            data=[
                {
                    'id': 1,
                    'int_field': 1,
                    'char_field': '11',
                },  #
                {
                    'id': 2,
                    'int_field': 2,
                    'char_field': '22',
                },  #
                {
                    'id': 3,
                    'int_field': 3,
                    'char_field': '33',
                },  # create
            ]
        )
    assert Table.objects.count() == 3
    assert Table.objects.filter(char_field__in=['11', '22', '33']).count() == 3


def test_swap_ids(django_assert_num_queries):
    Table.objects.create(id=1, int_field=1, char_field='1')
    Table.objects.create(id=2, int_field=2, char_field='2')
    with django_assert_num_queries(1):  # get
        run_update_list_of_instances(
            identifier=('int_field', 'char_field'),
            data=[
                {
                    'id': 2,
                    'int_field': 1,
                    'char_field': '1',
                },
                {
                    'id': 1,
                    'int_field': 2,
                    'char_field': '2',
                },
            ]
        )
