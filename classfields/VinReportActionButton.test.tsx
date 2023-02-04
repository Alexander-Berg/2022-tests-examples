import React from 'react';
import { shallow } from 'enzyme';

import context from 'autoru-frontend/mocks/contextMock';

import cardVinReportFree from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';

import VinReportActionButton from './VinReportActionButton';

const REQUIRED_PROPS = {
    name: 'VinReportActionButton',
    vinReportPaymentParams: { offerId: '123-456' },
};

it('должен показать цены для пакета, если isBundleButton', () => {
    const wrapper = shallow(
        <VinReportActionButton
            { ...REQUIRED_PROPS }
            needAuth
            servicePrice={ cardVinReportFree.billing?.service_prices.find((service) => service.counter === '10') }
            vinReport={ cardVinReportFree }
            withSummary
        />, { context },
    );

    expect(wrapper.find('Price').at(0).prop('price')).toBe(50);
    expect(wrapper.find('Price').at(1).prop('price')).toBe(500);
    expect(wrapper.find('Price').at(2).prop('price')).toBe(999);
});

it('должен показать только полную цену пакета с withOnlySummary', () => {
    const wrapper = shallow(
        <VinReportActionButton
            { ...REQUIRED_PROPS }
            needAuth
            servicePrice={ cardVinReportFree.billing?.service_prices.find((service) => service.counter === '10') }
            vinReport={ cardVinReportFree }
            withSummary
            withOnlySummary
        />, { context },
    );
    expect(wrapper.find('Price').at(0).prop('price')).toBe(500);
    expect(wrapper.find('Price').at(1).prop('price')).toBe(999);
});

it('должен показать цены для одного отчёта, если не isPackageButton', () => {
    const wrapper = shallow(
        <VinReportActionButton
            { ...REQUIRED_PROPS }
            needAuth
            vinReport={ cardVinReportFree }
            withSummary
        />, { context },
    );
    expect(wrapper.find('Price').at(0).prop('price')).toBe(99);
    expect(wrapper.find('Price').at(1).prop('price')).toBe(99);
    expect(wrapper.find('Price').at(2).prop('price')).toBe(197);
});
