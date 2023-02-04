import React from 'react';
import { shallow } from 'enzyme';

import { DealStep } from '@vertis/schema-registry/ts-types-snake/vertis/safe_deal/common';

import context from 'autoru-frontend/mocks/contextMock';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.mock';
import getIdHash from 'auto-core/react/lib/offer/getIdHash';

import SafeDealBlockAbstract from './SafeDealBlockAbstract';
import type { Props } from './SafeDealBlockAbstract';

const createDeal = jest.fn().mockReturnValue(Promise.resolve());
const openAuthModalWithCallback = jest.fn();

const CATEGORY = 'cars';
const OFFER_ID = '1113843815';
const HASH = 'e229bc38';
const OFFER = { ...offerMock, id: OFFER_ID, hash: HASH };

const defaultProps: Props = {
    offer: OFFER,
    offerId: getIdHash(OFFER),
    isAuth: true,
    isDealCreatePending: false,
    isDealMillionActive: false,
    dealStep: DealStep.DEAL_DECLINED,
    createDeal,
    openAuthModalWithCallback,
    safeDealInfo: undefined,
    category: CATEGORY,
};

const getInstance = async(props: typeof defaultProps): Promise<SafeDealBlockAbstract> => {
    const wrapper = shallow(
        <SafeDealBlockAbstract { ...props }/>,
        { disableLifecycleMethods: true, context });

    return await wrapper.instance() as SafeDealBlockAbstract;
};

describe('isSafeDealBlockShown возвращает true', () => {
    it('dealStep: DEAL_DECLINED', async() => {
        const instance = await getInstance({ ...defaultProps });

        expect(instance.isSafeDealBlockShown()).toEqual(true);
    });
    it('dealStep: DEAL_CANCELLED', async() => {
        const instance = await getInstance({ ...defaultProps, dealStep: DealStep.DEAL_CANCELLED });

        expect(instance.isSafeDealBlockShown()).toEqual(true);
    });
    it('dealStep: undefined', async() => {
        const instance = await getInstance({ ...defaultProps, dealStep: undefined });

        expect(instance.isSafeDealBlockShown()).toEqual(true);
    });
});

describe('handleButtonClick', () => {
    it('всегда отправляет метрику', async() => {
        const instance = await getInstance({ ...defaultProps });

        instance.handleButtonClick();

        expect(context.metrika.sendPageEvent).toHaveBeenCalledWith([ 'deal_block, create_deal_btn', 'click' ]);
    });

    it('если не авторизован - открывает модалку с авторизацией', async() => {
        const instance = await getInstance({ ...defaultProps, isAuth: false });

        instance.handleButtonClick();

        expect(openAuthModalWithCallback).toHaveBeenCalled();
    });

    it('если авторизован - открывает модалку с подтверждением старта бсделку', async() => {
        const instance = await getInstance({ ...defaultProps, isAuth: true });

        instance.handleButtonClick();

        expect(instance.state.isConfirmModalVisible).toEqual(true);
    });
});

it('handleModalClose должен закрыть модалку и отправить метрику', async() => {
    const instance = await getInstance({ ...defaultProps, isAuth: true });

    instance.handleModalClose();

    expect(context.metrika.sendPageEvent).toHaveBeenCalledWith([ 'deal_modal', 'cancel_btn', 'click' ]);
    expect(instance.state.isConfirmModalVisible).toEqual(false);
});

it('handleModalConfirm должен отправить метрику и начать бсделку', async() => {
    const instance = await getInstance({ ...defaultProps, isAuth: true });
    const NEW_PRICE = '555000';

    instance.handleModalConfirm(NEW_PRICE);

    expect(context.metrika.sendPageEvent).toHaveBeenCalledWith([ 'deal_modal', 'ok_btn', 'click' ]);
    expect(createDeal).toHaveBeenCalledWith(`${ OFFER_ID }-${ HASH }`, CATEGORY, NEW_PRICE);
});
