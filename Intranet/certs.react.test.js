import React from 'react';
import Certs from 'b:certs';
import {mount} from 'enzyme';
import {Provider} from 'react-redux';
import {createMockStore} from 'redux-test-utils';
import {GET_STORE_PATH} from 'tools-access-react-redux-router/src/reducer';

it('should contain a proper content', () => {

    const store = createMockStore({
        certs: {
            items: [],
            count: 0,
            page: 0,
            perPage: 10,
            additionalFiltersName: [
                'ca_name',
                'requester',
                'host',
                'serial_number',
                'abc_service'
            ],
            mainFiltersName: [
                'type',
                'status',
                'user',
                'common_name'
            ]
        },
        [GET_STORE_PATH]: '/certificates'
    });

    const getFilters = jest.fn();

    getFilters.mockReturnValue({
        ca_name: 'ca_name',
        type: 'type',
        status: 'status',
        user: 'user',
        common_name: 'common_name'
    });

    const wrapper = mount(
        <Provider store={store}>
            <Certs
                additionalFiltersKeys={['a', 'b']}
                getFilters={getFilters}
                filtersKeys={['a']}
                path="/certificates/1"
                expr="/certificates/:certId"
            />
        </Provider>
    );

    expect(wrapper).toMatchSnapshot();
    wrapper.unmount();
});
