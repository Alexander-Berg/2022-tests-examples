import React from 'react';
import { Provider } from 'react-redux';
import _ from 'lodash';
import { shallow } from 'enzyme';

import { DealStep } from '@vertis/schema-registry/ts-types-snake/vertis/safe_deal/common';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';

import safeDealMock from 'auto-core/react/dataDomain/safeDeal/mocks/safeDeal.mock';
import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import type { Props } from 'auto-core/react/components/common/SafeDealBlockAbstract/SafeDealBlockAbstract';

import SafeDealBlockDesktop from './SafeDealBlockDesktop';

const store = mockStore({
    safeDeal: {
        ...safeDealMock,
        deal: {
            ...safeDealMock.deal,
            step: undefined,
        },
    },
    bunker: {
        'banners/index-marketing-banners': {
            dealMillion: {
                inUse: true,
            },
        },
    },
});

const defaultProps: Props = {
    offer: offerMock,
    offerId: offerMock.id,
    isAuth: true,
    isDealCreatePending: false,
    isDealMillionActive: true,
    dealStep: DealStep.DEAL_DECLINED,
    createDeal: jest.fn(),
    openAuthModalWithCallback: jest.fn(),
    safeDealInfo: undefined,
    category: 'cars',
};

const getInstance = (context = contextMock) => {
    const wrapper = shallow(
        <Provider store={ store }>
            <SafeDealBlockDesktop { ...defaultProps }/>
        </Provider>,
        { context });

    return wrapper.dive().dive();
};

it('отправит метрику при показе', () => {
    const instance = getInstance();
    instance.find('InView').simulate('change', true);

    expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'deal_block', 'show' ]);
});

describe('возвращает разный контент', () => {
    it('без экспа', () => {
        const context = _.cloneDeep(contextMock);
        context.hasExperiment.mockImplementation(() => false);

        const instance = getInstance(context);

        const title = instance.find('.SafeDealBlockDesktop__titleMillion').children().text();
        const text = instance.find('.SafeDealBlockDesktop__textMillion').children().text();
        const button = instance.find('Button').at(0).children().text();

        expect(title).toEqual('Купите этот автомобиль с «Безопасной сделкой»');
        expect(text).toEqual('Заключайте «Безопасную сделку» и получите шанс выиграть миллион');
        expect(button).toEqual('Начать сделку');
    });
    it('с экспом AUTORUFRONT-20673_deal-million-variant-long', async() => {
        const context = _.cloneDeep(contextMock);
        context.hasExperiment.mockImplementation(exp => exp === 'AUTORUFRONT-20673_deal-million-variant-long');

        const instance = getInstance(context);

        const title = instance.find('.SafeDealBlockDesktop__titleMillion').children().text();
        const text = instance.find('.SafeDealBlockDesktop__textMillion').children().text();
        const button = instance.find('Button').at(0).children().text();

        expect(title).toEqual('Разыгрываем миллион!');
        expect(text).toEqual('С «Безопасной сделкой» не беспокойтесь за сохранность ' +
            'ваших денег. Воспользуйтесь нашей новой услугой и участвуйте в розыгрыше 1 миллиона рублей');
        expect(button).toEqual('Хочу миллион!');
    });
    it('с экспом AUTORUFRONT-20673_deal-million-variant-short', async() => {
        const context = _.cloneDeep(contextMock);
        context.hasExperiment.mockImplementation(exp => exp === 'AUTORUFRONT-20673_deal-million-variant-short');

        const instance = getInstance(context);

        const title = instance.find('.SafeDealBlockDesktop__titleMillion').children().text();
        const text = instance.find('.SafeDealBlockDesktop__textMillion').children().text();
        const button = instance.find('Button').at(0).children().text();

        expect(title).toEqual('Плюс миллион!');
        expect(text).toEqual('Заключите «Безопасную сделку» с продавцом этого автомобиля и выиграйте 1 миллион рублей!');
        expect(button).toEqual('Начать сделку');
    });
});
