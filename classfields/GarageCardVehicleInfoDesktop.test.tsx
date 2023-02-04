import { noop } from 'lodash';
import React from 'react';
import { shallow } from 'enzyme';
import { Provider } from 'react-redux';

import contextMock from 'autoru-frontend/mocks/contextMock';
import mockStore from 'autoru-frontend/mocks/mockStore';

import type { StateGarageCard } from 'auto-core/react/dataDomain/garageCard/types';

import garageCardMockOnlyMarkModel from 'auto-core/models/garageCard/mocks/mock-only_mark_model';

import GarageCardVehicleInfoDesktop from './GarageCardVehicleInfoDesktop';

let state: { garageCard: StateGarageCard };
beforeEach(() => {
    state = {
        garageCard: {
            pending: false,
            state: 'VIEW',
        },
    };
});

it('должен отрендериться без ошибок, если нет tech_param', async() => {
    const wrapper = shallow(
        <Provider store={ mockStore(state) }>
            <GarageCardVehicleInfoDesktop
                garageCard={ garageCardMockOnlyMarkModel.card }
                onSwitchToEdit={ noop }
                onSwitchToExpanded={ noop }
            />
        </Provider>,
        { context: contextMock },
    ).dive();

    const itemText = wrapper.find('.GarageCardVehicleInfoDesktop__item');

    expect(itemText).toHaveLength(1);
});
