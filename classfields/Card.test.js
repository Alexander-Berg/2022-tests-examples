/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import CardDumb from './CardDumb';

import salonMock from 'www-cabinet/react/components/Card/mocks/salon.mock.js';

it('должен провалидировать непустой url', () => {
    const tree = shallow(
        <CardDumb
            salon={ salonMock }
        />,
    );

    tree.find('CardAbout').simulate('inputChange', 'somesite', { name: 'url' });
    expect(tree.find('CardAbout').props().validateUrl()).toBe(false);

    expect(tree.instance().state.urlField).toEqual({
        name: 'url',
        value: 'somesite',
        error: 'Некорректный адрес cайта',
    });
});

it('должен отправить на сервер несколько номеров телефонов', () => {
    const submitForm = jest.fn(() => Promise.resolve(true));
    const tree = shallow(
        <CardDumb
            submitForm={ submitForm }
            salon={ salonMock }
        />,
    );
    const cardMapProps = tree.find('CardMap').props();
    cardMapProps.deletePhone(1);
    cardMapProps.addPhone();
    cardMapProps.changePhone(2, { countryCode: '8', cityCode: '915', phone: '1212277', callTill: '21', callFrom: '10' });
    tree.find('CardButtons').simulate('submitButtonClick');

    expect(submitForm.mock.calls[ 0 ][ 0 ].phones).toEqual([
        {
            id: '482872',
            title: 'Менеджер по продажам',
            delete_row: '',
            extention: '',
            city_code: '916',
            country_code: '8',
            phone: '4939956',
            call_from: '10',
            call_till: '19',
        },
        {
            id: '482874',
            delete_row: '1',
            extention: '2',
            city_code: '915',
            country_code: '8',
            phone: '1212277',
            call_from: '10',
            call_till: '19',
            title: '',
        },
        {
            id: '',
            city_code: '915',
            delete_row: '',
            extention: '',
            country_code: '8',
            phone: '1212277',
            call_from: '10',
            call_till: '21',
            title: '',
        },
    ]);
});

it('должен отправить на сервер дилерство', () => {
    const submitForm = jest.fn(() => Promise.resolve());
    const showInfoNotification = jest.fn();
    const open = jest.fn();
    const send = () => jest.fn();
    const append = jest.fn();
    const xhrMockObj = {
        open,
        send,
        response: JSON.stringify({
            data: {
                name: 'c5aa7eb9123290364a25e23fac6b1bb3',
                node_id: 4424,
            },
        }),
        readyState: 4,
    };
    global.XMLHttpRequest = jest.fn().mockImplementation(() => xhrMockObj);
    global.FormData = jest.fn().mockImplementation(() => ({
        append,
    }));

    const tree = shallow(
        <CardDumb
            showInfoNotification={ showInfoNotification }
            submitForm={ submitForm }
            salon={ salonMock }
            breadcrumbs={ [
                { id: 'AC', name: 'AC' },
                { id: 'ACURA', name: 'Acura' },
            ] }
        />,
    );

    const cardDealershipPorps = tree.find('CardDealership').props();

    cardDealershipPorps.removeDealership('3299-dd1c6c8aecaca0f027ff414deaa283e3');
    cardDealershipPorps.onMarkChange([ 'ACURA' ]);
    cardDealershipPorps.onDealershipFileChange({
        preventDefault: jest.fn(),
        stopPropagation: jest.fn(),
        target: {
            files: [ {
                size: 10,
            } ],
        },
    });

    xhrMockObj.onreadystatechange();
    tree.find('CardButtons').simulate('submitButtonClick');

    expect(submitForm.mock.calls[ 0 ][ 0 ].dealership).toEqual(
        {
            'delete': [ {
                file: '3299-dd1c6c8aecaca0f027ff414deaa283e3',
                fileId: '3299',
                fileName: 'dd1c6c8aecaca0f027ff414deaa283e3',
                id: '97732',
                mark_id: 'ADLER',
                mark_name: 'Adler',
                title: 'Adler',
                type: 'origin',
            } ],
            'new': [ {
                file: '4424-c5aa7eb9123290364a25e23fac6b1bb3',
                mark_id: 'ACURA',
                mark_name: 'Acura',
            } ],
            origin: [],
        },
    );
});
