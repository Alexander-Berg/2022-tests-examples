/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import type { Props } from './SafeDealAttentionModal';
import SafeDealAttentionModal from './SafeDealAttentionModal';

const DEAL_ID = '744e4253-4c30-4bbc-929f-50f7f176a7a3';

const subjectMock = {
    autoru: {
        offer: {
            category: 'CARS',
            id: '1088195806-be7b8403',
        },
        vin: 'WVWZZZ3AZRB048662',
        mark: 'Volkswagen',
        model: 'Passat',
        car_info: {
            year: 1994,
            mileage: 300000,
            horse_power: 90,
            subcategory: 'Легковая',
            color: 'Зеленый',
        },
        pts_info: {
            displacement: 1781,
            body_number: 'WVWZZZ3AZRB048662',
            license_plate: 'М558НВ33',
        },
        sts_info: {
            series_number: '3308797455',
        },
    },
};

const offerMock = {
    id: '1088195806-be7b8403',
    state: {
        image_urls: [
            {
                name: '0',
                sizes: {
                    '832x624': '//avatars.mds.yandex.net/get-verba/787013/2a000001609d567e7edf49a2e7f799fad10a/image',
                },
            },
        ],
    },
};

const defaultProps = {
    isModalShown: true,
    onClose: jest.fn(),
    attention: {
        deal_id: DEAL_ID,
        created: '2021-10-28T16:27:42.024480Z',
        template: 'seller-accepting-deal-seller',
        title: 'Безопасная сделка',
        body: 'У вас есть новый запрос на сделку по Honda Accord.',
        url: 'https://test.avto.ru/deal/744e4253-4c30-4bbc-929f-50f7f176a7a3/',
        subject: subjectMock,
    },
    offers: [ offerMock ],
    approveDeal: jest.fn(),
    openDealCancelPopup: jest.fn(),
    isMobile: false,
};

it('должен закрыть модальное окно при клике на кнопку "Отклонить"', () => {
    const onCloseMock = jest.fn();
    const props = { ...defaultProps, onClose: onCloseMock } as unknown as Props;

    const wrapper = renderWrapper(props);
    wrapper.find('.SafeDealAttentionModal__declineButton').simulate('click');

    expect(onCloseMock).toHaveBeenCalled();
});

it('должен открыть модал с причиной отмены при клике на кнопку "Отклонить"', () => {
    const props = defaultProps as unknown as Props;

    const wrapper = renderWrapper(props);
    wrapper.find('.SafeDealAttentionModal__declineButton').simulate('click');

    expect(props.openDealCancelPopup).toHaveBeenCalledWith('SELLER', DEAL_ID);
});

it('должен закрыть модальное окно при клике на кнопку "Подтвердить"', () => {
    const onCloseMock = jest.fn();
    const props = { ...defaultProps, onClose: onCloseMock } as unknown as Props;

    const wrapper = renderWrapper(props);
    wrapper.find('.SafeDealAttentionModal__approveButton').simulate('click');

    expect(onCloseMock).toHaveBeenCalled();
});

it('должен принять запрос при клике на кнопку "Подтвердить"', () => {
    const approveDealMock = jest.fn();
    const props = { ...defaultProps, approveDeal: approveDealMock } as unknown as Props;

    const wrapper = renderWrapper(props);
    wrapper.find('.SafeDealAttentionModal__approveButton').simulate('click');

    expect(approveDealMock).toHaveBeenCalledWith(DEAL_ID, 'by_seller');
});

function renderWrapper(props: Props) {
    return shallow(
        <SafeDealAttentionModal { ...props }/>,
        { context: { ...contextMock } },
    );
}
