import React from 'react';
import { shallow } from 'enzyme';

import type { RegionInfo } from '@vertis/schema-registry/ts-types-snake/auto/api/common_model';

import getVehicleInfoFixtures from 'auto-core/server/resources/publicApiGarage/methods/getVehicleInfo.fixtures';

import GarageAddCardPreview from './GarageAddCardPreview';

const baseProps = {
    onRegistrationRegionClick: () => {},
    renderGeoSelect: () => null,
    refRegistrationRegionName: {} as unknown as React.RefObject<HTMLSpanElement>,
    showGeoSelect: false,
};

it('должен отрендерить данные карточки', () => {
    const mock = getVehicleInfoFixtures.responseCard();
    const wrapper = shallow(
        <GarageAddCardPreview
            vehicleInfo={ mock }
            registrationRegion={ mock.suggested_registration_region as RegionInfo }
            { ...baseProps }
        />,
    );

    expect(wrapper).toMatchSnapshot();
});

it('должен отрендерить данные отчета', () => {
    const mock = getVehicleInfoFixtures.responseReport();
    const wrapper = shallow(
        <GarageAddCardPreview
            vehicleInfo={ getVehicleInfoFixtures.responseReport() }
            registrationRegion={ mock.suggested_registration_region as RegionInfo }
            { ...baseProps }
        />,
    );

    expect(wrapper).toMatchSnapshot();
});

it('должен отрендерить данные отчета без данных двигателя', () => {
    const mock = getVehicleInfoFixtures.responseReportWithoutDisplacement();
    const wrapper = shallow(
        <GarageAddCardPreview
            vehicleInfo={ mock }
            registrationRegion={ mock.suggested_registration_region as RegionInfo }
            { ...baseProps }
        />,
    );

    expect(wrapper).toMatchSnapshot();
});

it('не должен нарисовать цвет, если его нет в данных', () => {
    const response = getVehicleInfoFixtures.responseCard();
    delete response.card?.vehicle_info?.color;

    const wrapper = shallow(
        <GarageAddCardPreview
            vehicleInfo={ response }
            registrationRegion={ response.suggested_registration_region as RegionInfo }
            { ...baseProps }
        />,
    );

    expect(wrapper.find('.GarageAddCardPreview__item')).toHaveLength(4);
});
