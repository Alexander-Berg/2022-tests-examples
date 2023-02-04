const React = require('react');

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const VinReportHistoryAutoService = require('./VinReportHistoryAutoService');

it('должен нарисовать название автосервиса, если нет ссылки', () => {
    const wrapper = shallow(
        <VinReportHistoryAutoService
            record={{
                timestamp: '1556355600000',
                mileage: 100000,
                mileage_status: 'OK',
                partner_name: 'Какой-то автосервис',
                title: 'Визит в автосервис',
            }}
            opened={ true }
        />,
    );
    const autoserviceName = wrapper.dive().find('.VinReportHistoryRecord__value').last();

    expect(shallowToJson(autoserviceName)).toMatchSnapshot();
});

it('должен нарисовать ссылку на автосервис, если она есть', () => {
    const wrapper = shallow(
        <VinReportHistoryAutoService
            record={{
                timestamp: '1556355600000',
                mileage: 100000,
                mileage_status: 'OK',
                partner_name: 'Какой-то автосервис',
                partner_url: 'https://auto.ru',
                title: 'Визит в автосервис',
            }}
            opened={ true }
        />,
    );
    const autoserviceName = wrapper.dive().find('.VinReportHistoryRecord__value').last();

    expect(shallowToJson(autoserviceName)).toMatchSnapshot();
});

it('если работ более 4х, должен их спрятать под раскрывашку', () => {
    const wrapper = shallow(
        <VinReportHistoryAutoService
            record={{
                timestamp: '1556355600000',
                mileage: 100000,
                mileage_status: 'OK',
                partner_name: 'Какой-то автосервис',
                title: 'Визит в автосервис',
                works_names: [
                    'Защита картера - Снятие и установка',
                    'Защита картера - Снятие и установка',
                    'Защита картера - Снятие и установка',
                    'Сервис масла',
                    'Сервис масла',
                    'Сервис масла',
                ],
            }}
            opened={ true }
        />,
    );
    const counter = wrapper.dive().find('Link');

    expect(shallowToJson(counter)).toMatchSnapshot();
});
