/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import React from 'react';
import reactDOM from 'react-dom';
import { shallow } from 'enzyme';
import { act } from 'react-dom/test-utils';

import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import FilterBannerGarage from './FilterBannerGarage';

const Context = createContextProvider(contextMock);

it('рендерит правильный href и target', () => {
    const wrapper = shallow(
        <Context>
            <FilterBannerGarage/>
        </Context>,
    ).dive();

    expect(wrapper.find('.FilterBannerGarage')
        .prop('target'))
        .toBe('_blank');

    expect(wrapper.find('.FilterBannerGarage')
        .prop('href'))
        .toBe('link/garage/?proven_owner_qr=true&from=autoru_main_garage_otchety');
});

describe('метрика', () => {
    it('отправляет одну цель после монтирования про показ баннера', async() => {
        const container = document.createElement('div');

        await act(async() => {
            reactDOM.render(
                <Context>
                    <FilterBannerGarage/>
                </Context>,
                container,
            );
        });

        expect(contextMock.metrika.reachGoal).toHaveBeenCalledTimes(1);
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('view-banner_garage_otchet');
    });

    it('отправляет цель после клика по ссылке', async() => {
        const wrapper = shallow(
            <Context>
                <FilterBannerGarage/>
            </Context>,
        ).dive();

        wrapper.find('.FilterBannerGarage').simulate('click');
        expect(contextMock.metrika.reachGoal).toHaveBeenCalledWith('click-banner_garage_otchet');
    });
});
