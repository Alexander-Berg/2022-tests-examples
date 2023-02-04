import React from 'react';
import { shallow } from 'enzyme';

import { Status } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_resolution_enums';

import vinMock from 'auto-core/models/garageCard/mocks/mock-vin';

import VinReportConstraints from './VinReportConstraints';

it('VinReportConstraints должен отрендериться', () => {
    const wrapper = shallow(
        <VinReportConstraints constraints={ vinMock.card.report?.report?.constraints }/>,
    );

    expect(wrapper).not.toBeEmptyRender();
});

it('VinReportConstraints не должен отрендериться, если status NOT_VISIBLE', () => {
    const wrapper = shallow(
        <VinReportConstraints constraints={ Object.assign({}, vinMock.card.report?.report?.constraints, { status: Status.NOT_VISIBLE }) }/>,
    );

    expect(wrapper).toBeEmptyRender();
});
