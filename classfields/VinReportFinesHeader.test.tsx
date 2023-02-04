import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import VinReportFinesHeader from './VinReportFinesHeader';

const HEADER = {
    is_updating: false,
    timestamp_update: '1623425412801',
    title: '100 штрафов',
};

it(`VinReportFinesHeader показывает вопросик + количество неоплаченных штрафов на десктопе, когда есть записи`, async() => {
    const wrapper = shallow(<VinReportFinesHeader header={ HEADER } count={ 10 } onClick={ _.noop }/>);
    expect(wrapper.find('HoveredTooltip').exists()).toBe(true);
});

it(`VinReportFinesHeader не показывает вопросик + количество неоплаченных штрафов на десктопе, когда нет записей`, async() => {
    const wrapper = shallow(<VinReportFinesHeader header={ HEADER } count={ 0 } onClick={ _.noop }/>);
    expect(wrapper.find('HoveredTooltip').exists()).toBe(false);
});

it(`VinReportFinesHeader нет события клик на надписи с количеством неоплаченных штрафов, когда показываем неоплаченные штрафы`, async() => {
    const onClick = jest.fn();
    const wrapper = shallow(<VinReportFinesHeader header={ HEADER } count={ 10 } isDisabledButtonClick onClick={ onClick }/>);
    wrapper.find('.VinReportFinesHeader__amount').simulate('click');
    expect(onClick).toHaveBeenCalledTimes(0);
});

it(`VinReportFinesHeader есть событие клик на надписи с количеством неоплаченных штрафов, когда показываем все штрафы`, async() => {
    const onClick = jest.fn();
    const wrapper = shallow(<VinReportFinesHeader header={ HEADER } count={ 10 } onClick={ onClick }/>);
    wrapper.find('.VinReportFinesHeader__amount').simulate('click');
    expect(onClick).toHaveBeenCalledTimes(1);
});
