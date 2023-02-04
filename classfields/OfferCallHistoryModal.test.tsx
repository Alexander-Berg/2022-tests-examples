import { noop } from 'lodash';
import React from 'react';
import type { ShallowWrapper } from 'enzyme';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';

import gateApi from 'auto-core/react/lib/gateApi';

import type { Offer } from 'auto-core/types/proto/auto/api/api_offer_model';

import OfferCallHistoryModal from './OfferCallHistoryModal';

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResourcePublicApi: jest.fn(() => new Promise(() => {})),
    };
});

const getResourcePublicApi = gateApi.getResourcePublicApi as jest.MockedFunction<typeof gateApi.getResource>;

let offer: Partial<Offer>;
let wrapper: ShallowWrapper;
let goodResponse: any;
beforeEach(() => {
    offer = {
        category: 'cars',
        id: '123',
        hash: 'abc',
    };

    goodResponse = {
        items: [
            { source: '+79588319559', talk_duration: '17', call_result: 'SUCCESS', time: '2020-04-02T15:55:05.806Z' },
        ],
    };

    getResourcePublicApi.mockClear();

    wrapper = shallow(
        <OfferCallHistoryModal
            offer={ offer as Offer }
            onRequestHide={ noop }
            visible={ false }
        />,
    );
});

it('не должен запрашивать данные, если стал не виден', () => {
    expect(getResourcePublicApi).not.toHaveBeenCalled();
});

it('должен запросить данные, если модал стал виден', () => {
    wrapper.setProps({ visible: true });

    expect(getResourcePublicApi).toHaveBeenCalledWith('offerCallHistory', { category: 'cars', offer_id: '123-abc', page_size: 100 });
});

it('должен отрисовать лоадер пока данные грузятся', () => {
    wrapper.setProps({ visible: true });

    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен отрисовать контент, если данные загрузились', () => {
    const p = Promise.resolve(goodResponse);
    getResourcePublicApi.mockImplementation(() => p);

    wrapper.setProps({ visible: true });
    return p.then(() => {
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });
});

describe('ошибка загрузки', () => {
    it('должен отрисовать ошибку, если данные не загрузились', () => {
        expect.assertions(1);

        const p = Promise.reject();
        getResourcePublicApi.mockImplementation(() => p);

        wrapper.setProps({ visible: true });
        return p.then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            async() => {
                await new Promise((resolve) => setTimeout(resolve));
                expect(shallowToJson(wrapper)).toMatchSnapshot();
            },
        );
    });

    it('должен перезапросить данные, после клика на "повторить"', () => {
        expect.assertions(1);

        const p = Promise.reject();
        getResourcePublicApi.mockImplementation(() => p);

        wrapper.setProps({ visible: true });
        return p.then(
            () => Promise.reject('UNEXPECTED_RESOLVE'),
            async() => {
                await new Promise((resolve) => setTimeout(resolve));
                const p = Promise.resolve(goodResponse);
                getResourcePublicApi.mockImplementation(() => p);

                wrapper.find('.OfferCallHistoryModal__error-link').simulate('click');

                return p
                    .then(() => {
                        expect(shallowToJson(wrapper)).toMatchSnapshot();
                    });
            },
        );
    });
});
