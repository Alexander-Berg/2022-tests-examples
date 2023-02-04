import React from 'react';
import { shallow } from 'enzyme';

import DateMock from 'autoru-frontend/mocks/components/DateMock';

import historyMock from 'auto-core/react/dataDomain/defaultVinReport/mocks/historyMock';

import VinReportHistory from './VinReportHistory';

it('не падает, если вообще не пришли данные по истории', () => {
    const wrapper = shallow(
        <VinReportHistory/>,
    );

    expect(wrapper).toBeEmptyRender();
});

it('должен отрисовать владельцев, если они есть', () => {
    const wrapper = shallow(
        <VinReportHistory history={ historyMock.value() }/>,
    );

    expect(wrapper.find('.VinReportHistory__item')).toHaveLength(4);
});

it('должен отрисовать заглушку, если у всех владельцев нет записей', () => {
    const wrapper = shallow(
        <VinReportHistory history={ historyMock.withEmpty().value() }/>,
    );

    expect(wrapper.find('.VinReportHistory__fallback')).toHaveLength(1);
});

it('должен отрисовать только владельца, если нет записей', () => {
    const wrapper = shallow(
        <VinReportHistory history={ historyMock.withoutRecords().value() }/>,
    );

    expect(wrapper.find('.VinReportHistory__owner')).toHaveLength(1);
    expect(wrapper.find('.VinReportHistory__records')).not.toExist();
});

it('не должен отрисовать пункт без владельца, если промежуток from - to меньше трёх дней', () => {
    const wrapper = shallow(
        <VinReportHistory history={ historyMock.withoutRegistrationLessThan3Days().value() }/>,
    );

    expect(wrapper.find('.VinReportHistory__owner')).not.toExist();
});

it('не должен отрисовать пункт без владельца, если промежуток from - now меньше трёх дней', () => {
    const wrapper = shallow(
        <DateMock date="2020-04-22">
            <VinReportHistory history={ historyMock.withoutRegistrationMoreThan3Days().value() }/>
        </DateMock>,
    ).dive();

    expect(wrapper.find('.VinReportHistory__owner')).not.toExist();
});

it('должен отрисовать компонент про неготовый блок, если is_updating', () => {
    const wrapper = shallow(
        <DateMock date="2020-04-22">
            <VinReportHistory history={ historyMock.emptyIsUpdating().value() }/>
        </DateMock>,
    ).dive();

    expect(wrapper.find('VinReportLoading')).toHaveLength(1);
    expect(wrapper.find('.VinReportHistory__owner')).not.toExist();
    expect(wrapper.find('.VinReportHistory__records')).not.toExist();
});
