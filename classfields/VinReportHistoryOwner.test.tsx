import React from 'react';
import { shallow } from 'enzyme';

import type { HistoryBlock_OwnerHistory } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';
import { OwnerItem_RegistrationStatus } from '@vertis/schema-registry/ts-types-snake/auto/api/vin/vin_report_model';

import VinReportHistoryOwner from './VinReportHistoryOwner';

it('не должен рендерить без записей о владельце', () => {
    const owner = {} as HistoryBlock_OwnerHistory;

    const wrapper = shallow(<VinReportHistoryOwner owner={ owner }/>);

    expect(wrapper.type()).toBeNull();
});

it('для первого блока с UNKNOWN_REGISTRATION_STATUS должен отрендерить выпуск', () => {
    const owner = {
        owner: { registration_status: OwnerItem_RegistrationStatus.UNKNOWN_REGISTRATION_STATUS },
    } as HistoryBlock_OwnerHistory;

    const wrapper = shallow(<VinReportHistoryOwner owner={ owner } index={ 0 }/>);

    expect(wrapper.find('.VinReportHistoryOwner__title').first().text()).toEqual('Выпуск автомобиля');
});

it('не должен рендерить блок владельца с UNKNOWN_REGISTRATION_STATUS, если это не первая запись', () => {
    const owner = {
        owner: { registration_status: OwnerItem_RegistrationStatus.UNKNOWN_REGISTRATION_STATUS },
    } as HistoryBlock_OwnerHistory;

    const wrapper = shallow(<VinReportHistoryOwner owner={ owner } index={ 2 }/>);

    expect(wrapper.find('.VinReportHistoryOwner__title').first().text()).toEqual('Владелец 1');
});

it('не упадет, если придут кривоватые данные', () => {
    const owner = {
        owner: { registration_status: OwnerItem_RegistrationStatus.REGISTERED },
    } as HistoryBlock_OwnerHistory;

    const wrapper = shallow(<VinReportHistoryOwner owner={ owner }/>);

    expect(wrapper.type()).not.toBeNull();
});
