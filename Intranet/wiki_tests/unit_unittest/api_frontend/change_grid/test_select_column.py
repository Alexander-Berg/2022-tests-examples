from ujson import loads

from intranet.wiki.tests.wiki_tests.unit_unittest.api_frontend.base import BaseGridsTest

GRID_WITHOUT_SELECT_STRUCTURE = """
{
  "title" : "Список покупочек",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "name",
      "title" : "Название покупки"
    }
  ]
}
"""

GRID_WITH_SELECT_STRUCTURE = """
{
  "title" : "Список покупочек",
  "width" : "100%",
  "sorting" : [],
  "fields" : [
    {
      "name" : "listy",
      "type" : "select",
      "multiple" : true,
      "title" : "Списочек",
      "options": ["Кефир", "Катапульта", "Доблесть"]
    }
  ]
}
"""


class ChangeSelectColumnTest(BaseGridsTest):
    def _prepare_grid(self, with_select=True):
        self._create_grid(
            'pokupo4ki',
            GRID_WITH_SELECT_STRUCTURE if with_select else GRID_WITHOUT_SELECT_STRUCTURE,
            self.user_thasonic,
        )
        response = self.client.get('/_api/frontend/pokupo4ki/.grid')
        version = loads(response.content)['data']['version']

        return version

    def test_add_option_column_simple(self):
        # Простое создание колонки с одной опцией.

        version = self._prepare_grid(with_select=False)

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_column=dict(
                            type='multiple_select',
                            name='listy',
                            title='Списочек',
                            required=True,
                            options=dict(
                                changes=[
                                    dict(add_option=dict(value='Поребрик')),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        content = loads(response.content)

        columns = content['data']['grid']['structure']['fields']

        self.assertEqual(['Поребрик'], columns[1]['options'])

    def test_add_option_column_with_non_add_option_first_change(self):
        # Создание колонки, когда первая операция – не add_option, не допустимо.

        version = self._prepare_grid(with_select=False)

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_column=dict(
                            type='multiple_select',
                            name='listy',
                            title='Списочек',
                            required=True,
                            options=dict(
                                changes=[
                                    dict(remove_option=dict(index=7)),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(409, response.status_code)

        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'NO_SUCH_OPTION')

    def test_add_option(self):
        # Добавляем опцию в существующую колонку.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            options=dict(
                                changes=[
                                    dict(add_option=dict(value='Обезжиренный творог')),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        content = loads(response.content)

        columns = content['data']['grid']['structure']['fields']

        self.assertEqual(['Кефир', 'Катапульта', 'Доблесть', 'Обезжиренный творог'], columns[0]['options'])

    def test_add_duplicate_option(self):
        # Добавление дубликата в список опций игнорируется.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            title='Списочек',
                            options=dict(
                                changes=[
                                    dict(add_option=dict(value='Радость')),
                                    dict(add_option=dict(value='Отчаяние')),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            title='Списочек',
                            options=dict(
                                changes=[
                                    dict(add_option=dict(value='Радость')),
                                    dict(add_option=dict(value='Отчаяние')),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        data = loads(response.content)
        columns = data['data']['grid']['structure']['fields']
        self.assertEqual(['Кефир', 'Катапульта', 'Доблесть', 'Радость', 'Отчаяние'], columns[0]['options'])

    def test_whitespace_option(self):
        # Проверим, что опция, состоящая из пробела, недопустима.
        # Дело в том, что пробелы обрезаются, а пустая строка недопустима.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_column=dict(
                            type='multiple_select',
                            name='listy',
                            title='Списочек',
                            required=True,
                            options=dict(
                                changes=[
                                    dict(add_option=dict(value=' ')),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(409, response.status_code)

        content = loads(response.content)

        self.assertEqual('value: This field may not be blank.', content['error']['message'][0])

    def test_rename_option(self):
        # Переименовывем опцию, проверяем, что она переименовалась в данных.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change',
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                listy=['Катапульта'],
                            ),
                        )
                    )
                ],
            ),
        )
        version = loads(response.content)['data']['version']

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change',
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                listy=['Катапульта', 'Кефир'],
                            ),
                        )
                    )
                ],
            ),
        )
        version = loads(response.content)['data']['version']

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            options=dict(
                                changes=[
                                    dict(rename_option=dict(index=1, new_value='Требушет')),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        content = loads(response.content)

        columns = content['data']['grid']['structure']['fields']

        self.assertEqual(['Кефир', 'Требушет', 'Доблесть'], columns[0]['options'])

        rows = content['data']['grid']['rows']

        self.assertEqual(2, len(rows))

        self.assertEqual('2', rows[0][0]['row_id'])
        self.assertEqual(['Требушет', 'Кефир'], rows[0][0]['raw'])

        self.assertEqual('1', rows[1][0]['row_id'])
        self.assertEqual(['Требушет'], rows[1][0]['raw'])

    def test_rename_option_to_duplicate(self):
        # Нельзя переименовать опцию, если новое имя уже есть в списке опций.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            options=dict(
                                changes=[
                                    dict(rename_option=dict(index=1, new_value='Доблесть')),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(409, response.status_code)

        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'DUPLICATE_OPTION')

    def test_rename_nonexistent_index(self):
        # Нельзя переименовать несуществующую опцию.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            options=dict(
                                changes=[
                                    dict(rename_option=dict(index=4, new_value='Втулка')),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(409, response.status_code)

        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'NO_SUCH_OPTION')

    def test_remove_option(self):
        # Удаляем опцию, проверяем, что она удалилась из данных.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change',
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                listy=['Катапульта'],
                            ),
                        )
                    )
                ],
            ),
        )
        version = loads(response.content)['data']['version']

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change',
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_row=dict(
                            after_id='-1',
                            data=dict(
                                listy=['Катапульта', 'Кефир'],
                            ),
                        )
                    )
                ],
            ),
        )
        version = loads(response.content)['data']['version']

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            title='Списочек',
                            options=dict(
                                changes=[
                                    dict(remove_option=dict(index=1)),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        content = loads(response.content)

        columns = content['data']['grid']['structure']['fields']
        self.assertEqual(['Кефир', 'Доблесть'], columns[0]['options'])

        rows = content['data']['grid']['rows']
        self.assertEqual(2, len(rows))

        self.assertEqual('2', rows[0][0]['row_id'])
        self.assertEqual(['Кефир'], rows[0][0]['raw'])

        self.assertEqual('1', rows[1][0]['row_id'])
        self.assertEqual([], rows[1][0]['raw'])

        # ещё раз с той же версией, при этом на сервере уже нет этой опции
        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            title='Списочек',
                            options=dict(
                                changes=[
                                    dict(remove_option=dict(index=1)),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        version = loads(response.content)['data']['version']
        # ещё раз с со свежей версией, при этом на сервере уже нет этой опции
        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            title='Списочек',
                            options=dict(
                                changes=[
                                    dict(remove_option=dict(index=1)),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

    def test_move_option(self):
        # Переставляем опцию в другое место.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            title='Списочек',
                            options=dict(
                                changes=[
                                    dict(
                                        move_option=dict(
                                            old_index=1,
                                            new_index=0,
                                        )
                                    ),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        content = loads(response.content)
        columns = content['data']['grid']['structure']['fields']
        self.assertEqual(['Катапульта', 'Кефир', 'Доблесть'], columns[0]['options'])
        version = content['data']['version']

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            title='Списочек',
                            options=dict(
                                changes=[
                                    dict(
                                        move_option=dict(
                                            old_index=0,
                                            new_index=1,
                                        )
                                    ),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        content = loads(response.content)
        columns = content['data']['grid']['structure']['fields']
        self.assertEqual(['Кефир', 'Катапульта', 'Доблесть'], columns[0]['options'])
        version = content['data']['version']

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            title='Списочек',
                            options=dict(
                                changes=[
                                    dict(
                                        remove_option=dict(
                                            index=2,
                                        ),
                                    ),
                                    dict(
                                        remove_option=dict(
                                            index=1,
                                        ),
                                    ),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        # переносим 0->2 но теперь это out of bounds
        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            title='Списочек',
                            options=dict(
                                changes=[
                                    dict(
                                        move_option=dict(
                                            old_index=0,
                                            new_index=2,
                                        )
                                    ),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(200, response.status_code)

        content = loads(response.content)
        columns = content['data']['grid']['structure']['fields']
        self.assertEqual(['Кефир'], columns[0]['options'])

    def test_move_nonexistent_index(self):
        # Нельзя переместить несуществующую опцию или переместить ее на несуществующее место.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            options=dict(
                                changes=[
                                    dict(move_option=dict(old_index=4, new_index=0)),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )
        self.assertEqual(409, response.status_code)

        data = loads(response.content)
        self.assertEqual(data['error']['error_code'], 'NO_SUCH_OPTION')

    def test_multiple_changes_while_creating_column(self):
        # Проверяем более сложную ситуацию с множеством изменений при добавлении колонки.

        version = self._prepare_grid(with_select=False)

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_column=dict(
                            type='multiple_select',
                            name='listy',
                            title='Списочек',
                            required=True,
                            options=dict(
                                changes=[
                                    dict(
                                        # [] ->
                                        # ['Топор']
                                        add_option=dict(
                                            value='Топор',
                                        )
                                    ),
                                    dict(
                                        # ['Топор'] ->
                                        # ['Топор', 'Рубанок']
                                        add_option=dict(
                                            value='Рубанок',
                                        )
                                    ),
                                    dict(
                                        # ['Топор', 'Рубанок'] ->
                                        # ['Рубанок', 'Топор']
                                        move_option=dict(
                                            old_index=0,
                                            new_index=1,
                                        )
                                    ),
                                    dict(
                                        # ['Рубанок', 'Топор'] ->
                                        # ['Рубанок']
                                        remove_option=dict(index=1)
                                    ),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        content = loads(response.content)

        columns = content['data']['grid']['structure']['fields']

        self.assertEqual(['Рубанок'], columns[1]['options'])

    def test_multiple_changes_while_editing_column(self):
        # Проверяем более сложную ситуацию с множеством изменений при изменении колонки.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            options=dict(
                                changes=[
                                    dict(
                                        # ['Кефир', 'Катапульта', 'Доблесть'] ->
                                        # ['Кефир', 'Катапульта', 'Люберцы']
                                        rename_option=dict(
                                            index=2,
                                            new_value='Люберцы',
                                        )
                                    ),
                                    dict(
                                        # ['Кефир', 'Катапульта', 'Люберцы'] ->
                                        # ['Кефир', 'Катапульта', 'Люберцы', 'Йогурт']
                                        add_option=dict(
                                            value='Йогурт',
                                        )
                                    ),
                                    dict(
                                        # ['Кефир', 'Катапульта', 'Люберцы', 'Йогурт'] ->
                                        # ['Катапульта', 'Люберцы', 'Кефир', 'Йогурт']
                                        move_option=dict(
                                            old_index=0,
                                            new_index=2,
                                        )
                                    ),
                                    dict(
                                        # ['Катапульта', 'Люберцы', 'Кефир', 'Йогурт'] ->
                                        # ['Катапульта', 'Кефир', 'Йогурт']
                                        remove_option=dict(index=1)
                                    ),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(200, response.status_code)
        content = loads(response.content)

        columns = content['data']['grid']['structure']['fields']

        self.assertEqual(['Катапульта', 'Кефир', 'Йогурт'], columns[0]['options'])

    def test_empty_changes_while_adding_column(self):
        # Пустой список операций при добавлении колонки приводит к пустым options.

        version = self._prepare_grid(with_select=False)

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_column=dict(
                            type='multiple_select',
                            name='listy',
                            title='Списочек',
                            required=True,
                            options=dict(changes=[]),
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(409, response.status_code)

        content = loads(response.content)

        self.assertEqual('grids.Edit:emptySelectOptions', content['error']['message'][0])

    def test_add_and_remove_option_while_adding_column(self):
        # Добавление и удаление опции при добавлении колонки приводит к пустым options.

        version = self._prepare_grid(with_select=False)

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        added_column=dict(
                            type='multiple_select',
                            name='listy',
                            title='Списочек',
                            required=True,
                            options=dict(
                                changes=[
                                    dict(
                                        some_field='some',
                                        add_option=dict(
                                            value='Йогурт',
                                        ),
                                    ),
                                    dict(
                                        remove_option=dict(
                                            index=0,
                                        )
                                    ),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(409, response.status_code)

        content = loads(response.content)

        self.assertEqual('grids.Edit:emptySelectOptions', content['error']['message'][0])

    def test_remove_all_options_while_editing_column(self):
        # Проверяем более сложную ситуацию с множеством изменений при изменении колонки.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            options=dict(
                                changes=[
                                    dict(
                                        remove_option=dict(
                                            index=0,
                                        )
                                    ),
                                    dict(
                                        remove_option=dict(
                                            index=0,
                                        )
                                    ),
                                    dict(
                                        remove_option=dict(
                                            index=0,
                                        )
                                    ),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(409, response.status_code)

        content = loads(response.content)

        self.assertEqual('grids.Edit:emptySelectOptions', content['error']['message'][0])

    def test_multiple_changes_at_one_time(self):
        # Каждое изменение может включать в себя лишь одну операцию.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            options=dict(
                                changes=[
                                    dict(
                                        add_option=dict(
                                            value='Канава',
                                        ),
                                        rename_option=dict(index=0, new_value='Болото'),
                                    ),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(409, response.status_code)

        content = loads(response.content)

        self.assertEqual('Please, apply only one change at a time', content['error']['message'][0])

    def test_empty_change_unit(self):
        # Каждое изменение должно содержать хотя бы одну операцию.

        version = self._prepare_grid()

        response = self.client.post(
            '/_api/frontend/pokupo4ki/.grid/change_and_get_document',
            dict(
                version=str(version),
                changes=[
                    dict(
                        edited_column=dict(
                            name='listy',
                            options=dict(
                                changes=[
                                    dict(),
                                ]
                            ),
                        ),
                    )
                ],
            ),
        )

        self.assertEqual(409, response.status_code)

        content = loads(response.content)

        self.assertEqual('You should not pass empty changes', content['error']['message'][0])
