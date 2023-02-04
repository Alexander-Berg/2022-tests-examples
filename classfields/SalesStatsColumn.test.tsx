import 'jest-enzyme';

import type { ShallowWrapper } from 'enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import DateMock from 'autoru-frontend/mocks/components/DateMock';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';

import type { Props, State } from './SalesStatsColumn';
import SalesStatsColumn from './SalesStatsColumn';

const defaultProps: Props = {
    offer: cloneOfferWithHelpers(offerMock)
        .withCreationDate(String(new Date('2021-03-15T12:00:00Z').getTime()))
        .withSearchPosition(17)
        .value(),
    item: {
        views: 350,
        date: '2021-04-24',
        phone_views: 150,
        phone_calls: 50,
        favorite_total: 200,
        has_promo: true,
    },
    hasRedirectPhones: false,
    hasTooltip: false,
    isHovered: false,
    hasVasEvent: false,
    type: 'views',
    onMouseEnter: () => {},
    onMouseLeave: () => {},
    openPaymentModal: () => {},
    downfallValue: 27,
};
const today = '2021-04-24';

describe('SalesStatsColumn', () => {
    it('отрендерит тултип при снижении просмотров, оставит при наведении, а потом скроет', () => {
        const wrapper = renderComponent({
            props: defaultProps,
        });

        expect(wrapper.find('.SalesStatsColumn__downfallPopup')).toExist();

        wrapper.setProps({ isHovered: true });

        expect(wrapper.find('.SalesStatsColumn__downfallPopup')).toExist();

        wrapper.setProps({ isHovered: false });

        expect(wrapper.find('.SalesStatsColumn__downfallPopup')).not.toExist();
    });
});

function renderComponent({ props }: { props: Props }) {
    const Context = createContextProvider(contextMock);

    const wrapper: ShallowWrapper<Props, State, SalesStatsColumn> = shallow(
        <Context>
            <DateMock date={ today }>
                <SalesStatsColumn { ...props }/>
            </DateMock>
        </Context>,
    ).dive().dive();

    return wrapper;
}
