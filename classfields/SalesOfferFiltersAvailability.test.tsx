import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import SalesOfferFiltersAvailability from './SalesOfferFiltersAvailability';

const defaultProps = {
    router: {},
    routeParams: { availability: [ 'IN_TRANSIT', 'ON_ORDER' ] },
    hideGroupActions: () => {},
};

it('должен переделать из массива в строку значение фильтра', () => {
    const tree = shallow(
        <SalesOfferFiltersAvailability { ...defaultProps }/>,
        { context: { ...contextMock } },
    );

    expect(tree.find('RadioGroup').prop('value')).toEqual('IN_TRANSIT,ON_ORDER');
});
