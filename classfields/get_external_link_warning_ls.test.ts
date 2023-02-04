jest.mock('./local_storage', () => {
    return {
        get_item: jest.fn(),
        set_item: jest.fn(),
    };
});

import * as ls from './local_storage';
import get_external_link_warning_ls from './get_external_link_warning_ls';

const get_ls_item_mock = ls.get_item as jest.MockedFunction<typeof ls.get_item>;

it('распарсит данные из ls', () => {
    get_ls_item_mock.mockReturnValueOnce('[{"key":"333_111","is_hidden":true,"ts":1640995200000}]');

    expect(get_external_link_warning_ls()).toEqual([ { key: '333_111', is_hidden: true, ts: 1640995200000 } ]);
});

it('вернет [] если ls пустой', () => {
    get_ls_item_mock.mockReturnValueOnce(null);

    expect(get_external_link_warning_ls()).toEqual([]);
});
