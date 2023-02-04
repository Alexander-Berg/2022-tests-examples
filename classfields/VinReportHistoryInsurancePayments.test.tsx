import React from 'react';
import { shallow } from 'enzyme';

import insurancePaymentsMock from 'auto-core/react/dataDomain/vinReport/mocks/insurancePayments';

import VinReportHistoryInsurancePayments from './VinReportHistoryInsurancePayments';

const { payments: [ record ] } = insurancePaymentsMock;

it('VinReportHistoryInsurancePayments рендерит VinReportHistoryRecord с нужными пропсами', async() => {
    const wrapper = shallow(
        <VinReportHistoryInsurancePayments record={ record }/>,
    );

    const expectedProps = {
        date: '1 марта 2018',
        info: [ {
            key: '0',
            name: 'Страховая компания',
            value: 'ИНГОССТРАХ',
        }, {
            key: '1',
            name: 'Тип страховки',
            value: 'ОСАГО',
        }, {
            key: '2',
            name: 'Сумма выплат',
            value: '49 524 ₽',
        } ],
        preset: undefined,
        title: 'Страховая выпалата',
        mileageStatus: 'OK',
    };

    expect(wrapper.find('VinReportHistoryRecord').props()).toEqual(expectedProps);
});
