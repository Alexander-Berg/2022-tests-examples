import React from 'react';
import { shallow } from 'enzyme';

import VinReportHistoryEvacuationRecord from './VinReportHistoryEvacuationRecord';

it('должен написать Регион, если города совпадают', () => {
    const wrapper = shallow(
        <VinReportHistoryEvacuationRecord
            record={{
                order_type: 'Аварийный комиссар без справок',
                execution_date: '1544456460000',
                from_city: 'Териберка',
                to_city: 'Териберка',
                source: 'Все эвакуаторы',
                source_url: 'https://www.all-evak.ru/',
            }}
        />,
    );

    const itemName = wrapper.dive().find('.VinReportHistoryRecord__name').last();
    const itemValue = wrapper.dive().find('.VinReportHistoryRecord__value').last();

    expect(itemName.text()).toBe('Регион');
    expect(itemValue.text()).toBe('Териберка');
});

it('должен написать Откуда и Куда, если города различаются', () => {
    const wrapper = shallow(
        <VinReportHistoryEvacuationRecord
            record={{
                order_type: 'Аварийный комиссар без справок',
                execution_date: '1544456460000',
                from_city: 'Париж',
                to_city: 'Техас',
                source: 'Все эвакуаторы',
                source_url: 'https://www.all-evak.ru/',
            }}
        />,
    );

    const item1Name = wrapper.dive().find('.VinReportHistoryRecord__name').at(2);
    const item1Value = wrapper.dive().find('.VinReportHistoryRecord__value').at(2);
    const item2Name = wrapper.dive().find('.VinReportHistoryRecord__name').at(3);
    const item2Value = wrapper.dive().find('.VinReportHistoryRecord__value').at(3);

    expect(item1Name.text()).toBe('Откуда');
    expect(item1Value.text()).toBe('Париж');
    expect(item2Name.text()).toBe('Куда');
    expect(item2Value.text()).toBe('Техас');
});
