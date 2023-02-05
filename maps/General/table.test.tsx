import * as React from 'react';
import {shallow} from 'enzyme';
import Table from './table';

const columns = [
    {
        title: 'Id',
        name: 'id',
        accessor: 'id',
        isSortable: true
    },
    {
        title: 'Name',
        name: 'name',
        accessor: 'name',
        isSortable: true
    }
];

const items = [
    {
        id: 1,
        name: 'first'
    },
    {
        id: 2,
        name: 'Second'
    }
];

describe('<Table />', () => {
    describe('sort', () => {
        const onSort = jest.fn(() => {});

        const table = shallow(
            <Table
                columns={columns}
                items={items}
                onSort={onSort}
                orderState={{
                    order: 'asc',
                    orderBy: 'id'
                }}
            />
        );

        const sortIcons = table.find('.table__sort .table__icon');

        [
            ['id', 'desc'],
            ['id', 'asc'],
            ['name', 'desc'],
            ['name', 'asc']
        ].forEach(([row, order], i) => {
            it(`should sort row: ${row}, order: ${order}`, () => {
                onSort.mockReset();
                sortIcons.at(i).simulate('click');
                expect(onSort).toHaveBeenCalledWith(row, order);
            });
        });
    });
});
