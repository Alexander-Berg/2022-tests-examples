import React from 'react';
import { shallow } from 'enzyme';

import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

import vinMock from 'auto-core/models/garageCard/mocks/mock-vin';

import VinReportWanted from './VinReportWanted';

it('VinReportWanted должен отрендериться', () => {
    const wrapper = shallow(
        <VinReportWanted wanted={ vinMock.card.report?.report?.wanted }/>,
    );

    expect(wrapper).not.toBeEmptyRender();
});

it('VinReportWanted не должен отрендериться, если status NOT_VISIBLE', () => {
    const wrapper = shallow(
        <VinReportWanted wanted={ Object.assign({}, vinMock.card.report?.report?.wanted, { status: Status.NOT_VISIBLE }) }/>,
    );

    expect(wrapper).toBeEmptyRender();
});
