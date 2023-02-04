# -*- coding: utf-8 -*-
from unittest.mock import (
    Mock,
    patch,
)
from contextlib import contextmanager

from hamcrest import (
    assert_that,
    equal_to,
    equal_to_ignoring_whitespace,
    calling,
)

from intranet.yandex_directory.src.yandex_directory.common.models.base import (
    preprocess_fields,
    BaseModel,
    UnknownFieldError,
    grouper,
)
from intranet.yandex_directory.src.yandex_directory.core.models import (
    UserModel,
)

from testutils import TestCase, raises


class SimpleModel(BaseModel):
    db_alias = 'simple_model'
    table = 'simple_model'
    all_fields = [
        'id',
        'field_1',
        'field_2',
        'related_field',
    ]
    select_related_fields = {
        'related_field': None,
    }

    def get_filters_data(self, filters_data):
        distinct = False
        return distinct, [], [], []

    def get_select_related_data(self, select_related):
        if not select_related:
            return [self.default_all_projection], [], []

        select_related = select_related or []
        projections = set()
        joins = []

        if 'related_field' in select_related:
            projections.update([
                '%s.*' % self.table,
                'other_table.related_field AS "related_field"',
            ])
            sql = """
                LEFT OUTER JOIN other_table_data as other_table ON (
                    simple_model.other_id = other_table.id
                )
            """
            joins.append(sql)

        return projections, joins, []


class TestBaseModel(TestCase):
    def setUp(self, *args, **kwargs):
        self.connection = Mock()
        self.base_model = SimpleModel(connection=self.connection)
        super(TestBaseModel, self).setUp(*args, **kwargs)

    @contextmanager
    def fake_db_rows(self, rows):
        """Вспомогательная функция, чтобы фейкать результаты
        запроса к self.connection.execute('SELECT ...').fetchall()
        """
        mock = Mock()
        mock.fetchall.return_value = rows
        with patch.object(self.connection.execute, 'return_value', mock):
            yield

    def test_find_only_fields(self):
        # проверяем, что явное указание fields составит запрос,
        # в котором будет сделан SELECT с указанными полями
        find_fields = ['id', 'field_1']

        with self.fake_db_rows([]):
            self.base_model.find(fields=find_fields)

        exp_query = 'SELECT simple_model.field_1, simple_model.id\n       FROM simple_model\n       ORDER BY id ASC'

        assert_that(self.connection.execute.call_count, equal_to(1))
        assert_that(self.connection.execute.call_args[0][0], equal_to_ignoring_whitespace(exp_query))
        self.connection.execute().fetchall.assert_called_once_with()

    def test_find_with_only_projections_and_select_related(self):
        # Проверяем, что при упоминании в fields поля, требующего
        # select_related запроса будет сделан SELECT с указанными полями
        # вместе с select_related

        with self.fake_db_rows([]):
            self.base_model.find(fields=['related_field'])

        exp_query = '''
            SELECT other_table.related_field AS "related_field", simple_model.id FROM simple_model
                LEFT OUTER JOIN other_table_data as other_table ON ( simple_model.other_id = other_table.id )
            ORDER BY id ASC
        '''

        assert_that(self.connection.execute.call_count, equal_to(1))
        assert_that(self.connection.execute.call_args[0][0], equal_to_ignoring_whitespace(exp_query))
        self.connection.execute().fetchall.assert_called_once_with()

    def test_find_without_fields(self):
        # проверяем, что без указания fields запрос будет выбирать все простые поля
        with self.fake_db_rows([]):
            self.base_model.find()

        exp_query = """
SELECT simple_model.field_1,
       simple_model.field_2,
       simple_model.id
FROM simple_model
ORDER BY id ASC
""".strip()
        self.connection.execute.assert_called_once_with(exp_query)
        self.connection.execute().fetchall.assert_called_once_with()

    def test_fields_are_transformed_into_projections(self):
        # Если при вызове find указаны fields, то внутри они
        # трансформируются в projections
        with self.fake_db_rows([]):
            with patch.object(self.base_model, 'explain_fields') \
                 as explain_fields:

                explain_fields.return_value = ['field_1'], [], []
                self.base_model.find(fields=['field_1'])

                explain_fields.assert_called_once_with(
                    {'id': True, 'field_1': True}
                )

    def test_preprocess_fields(self):
        # пустой список полей должен расширяться до поля id по умолчанию, если не задан primary_key

        assert_that(
            preprocess_fields([], all_fields=[]),
            equal_to({'id': True})
        )

        # пустой список полей должен расширяться до primary_key
        assert_that(
            preprocess_fields(
                [],
                all_fields=['id', 'user_id'],
                primary_key='user_id'
            ),
            equal_to({'user_id': True})
        )

        assert_that(
            preprocess_fields(
                ['nickname'],
                all_fields=UserModel.all_fields,
            ),
            equal_to({'id': True, 'nickname': True})
        )

        assert_that(
            preprocess_fields(
                ['department'],
                nested_fields={
                    'department': 'DepartmentModel',
                },
                all_fields=UserModel.all_fields,
            ),
            equal_to({'id': True, 'department': {'id': True}})
        )

        assert_that(
            preprocess_fields(
                ['department.name'],
                nested_fields={
                    'department': 'DepartmentModel',
                },
                all_fields=UserModel.all_fields,
            ),
            equal_to(
                {
                    'id': True,
                    'department': {'id': True, 'name': True}
                }
            )
        )

        # Если в качестве вложенного поля передано что-то левое,
        # то это должно привести к исключению
        assert_that(
            calling(preprocess_fields).with_args(
                ['department.name;drop table users;'],
                nested_fields={
                    'department': 'DepartmentModel',
                },
                all_fields=UserModel.all_fields,
                model_class=UserModel,
            ),
            raises(
                UnknownFieldError,
                field='name;drop table users;',
            )
        )

        # Вложенные поля должны раскрываться в виде словаря
        assert_that(
            preprocess_fields(
                ['department.name',
                 'department.description'],
                nested_fields={
                    'department': 'DepartmentModel',
                },
                all_fields=UserModel.all_fields,
            ),
            equal_to(
                {
                    'id': True,
                    'department': {
                        'id': True,
                        'name': True,
                        'description': True,
                    }
                }
            )
        )

        # Проверяем как работают три уровня вложенности
        assert_that(
            preprocess_fields(
                ['department.name.last'],
                nested_fields={
                    'department': 'DepartmentModel',
                },
                all_fields=UserModel.all_fields,
            ),
            equal_to(
                {
                    'id': True,
                    'department': {
                        'id': True,
                        # Сейчас указание полей на третьем уровне вложенности не поддерживается
                        # поэтому тут не словарь, а просто True, что означает, что
                        # если в поле name будет словарь, то only_hierarchical_fields отдаст его целиком
                        'name': True,
                    }
                }
            )
        )

        # Звёздочка должна раскрываться во все "simple" поля
        assert_that(
            preprocess_fields(
                ['*'],
                simple_fields=['id', 'name'],
                nested_fields={
                    'department': 'DepartmentModel',
                },
                all_fields=UserModel.all_fields,
            ),
            equal_to(
                {
                    'id': True,
                    'name': True,
                }
            )
        )

        # Вместе со звёздочкой могут быть перечислены "тяжёлые" поля
        assert_that(
            preprocess_fields(
                ['*', 'department'],
                simple_fields=['id', 'name'],
                nested_fields={
                    'department': 'DepartmentModel',
                },
                all_fields=UserModel.all_fields,
            ),
            equal_to(
                {
                    'id': True,
                    'name': True,
                    'department': {
                        'id': True,
                    },
                }
            )
        )

        # А так же, звёздочка может быть использована для вложенных сущностей
        assert_that(
            preprocess_fields(
                ['department.*'],
                simple_fields=['id', 'name'],
                nested_fields={
                    'department': 'DepartmentModel',
                },
                all_fields=UserModel.all_fields,
            ),
            equal_to(
                {
                    # у самого объекта выводится только id
                    'id': True,
                    # а у отдела - все простые поля. Это потому что в
                    # списке полей было только "department.*". Поэтому в
                    # объекте есть только id и department. Аргумент
                    # simple_fields тут просто перечисляет какие вообще
                    # есть поля у объекта. Name есть, но его не
                    # запрашивали. Поэтому оно и не выдаётся.
                    'department': {
                        'heads_group_id': True,
                        'uid': True,
                        'description': True,
                        'id': True,
                        'parent_id': True,
                        'aliases': True,
                        'name': True,
                        'path': True,
                        'label': True,
                        'org_id': True,
                        'members_count': True,
                        'external_id': True,
                        'removed': True,
                        'created': True,
                        'maillist_type': True,
                        'description_plain': True,
                        'name_plain': True,
                    },
                }
            )
        )


        # Проверим, что если у какого-то из полей есть зависимости,
        # то они так же будут добавлены в список проекций.
        fields = preprocess_fields(
            ['email'],
            all_fields=['email', 'nickname', 'domain'],
            field_dependencies = {
                'email': ['nickname', 'domain']
            }
        )
        assert_that(
            fields,
            equal_to(
                {
                    'id': True,
                    'email': True,
                    # Эти поля мы явно не запрашивали,
                    # но они подтянуты по зависимостям
                    'nickname': False,
                    'domain': False,
                }
            )
        )

        # Проверим, что если поле есть в зависимостях, а мы его запросили явно,
        # то оно будет добавлено в выдачу. (По ключу 'nickname' дожно быть True)
        fields = preprocess_fields(
            ['email', 'nickname'],
            all_fields=['email', 'nickname', 'domain'],
            field_dependencies = {
                'email': ['nickname', 'domain']
            }
        )
        assert_that(
            fields,
            equal_to(
                {
                    'id': True,
                    'email': True,
                    'nickname': True,
                    # Эти поля мы явно не запрашивали,
                    # но они подтянуты по зависимостям
                    'domain': False,
                }
            )
        )

        # Проверим, что зависимости должны работать и для вложенных полей
        # В этом случае, мы запрашиваем у юзера поле departments, которое зависит
        # от org_if и department.path, поэтому на выходе мы должны получить:
        # {
        #     'department': {'id': False, 'path': False},
        #     'departments: True,
        #     'org_id': False,
        # }
        fields = preprocess_fields(
            ['departments',],
            all_fields=['org_id', 'department', 'departments'],
            nested_fields={
                'department': 'DepartmentModel',
            },
            field_dependencies = {
                'departments': ['department.path', 'org_id']
            }
        )
        assert_that(
            fields,
            equal_to(
                {
                    'id': True,
                    'departments': True,
                    # Эти поля мы явно не запрашивали,
                    # но они подтянуты по зависимостям
                    'department': {'id': False, 'path': False},
                    'org_id': False,
                }
            )
        )

    def test_grouper(self):
        iterable = [1, 2, 3, 4, 5, 6, 7, 8]
        batch_size = 3

        expected = [[1, 2, 3], [4, 5, 6], [7, 8]]
        for i, group in enumerate(grouper(batch_size, iterable)):
            assert_that(
                group,
                equal_to(expected[i]),
            )
