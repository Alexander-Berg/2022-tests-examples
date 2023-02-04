/**
 * @jest-environment jsdom
 */

import React from 'react';
import { mount } from 'enzyme';
import { Provider } from 'react-redux';
import { act } from 'react-dom/test-utils';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';

import sleep from 'auto-core/lib/sleep';

import bankMock from 'auto-core/react/dataDomain/credit/mocks/bank.mockchain';
import creditProductMock from 'auto-core/react/dataDomain/credit/mocks/creditProduct.mockchain';
import creditApplicationMock from 'auto-core/react/dataDomain/credit/mocks/creditApplication.mockchain';
import creditApplicationClaimMock from 'auto-core/react/dataDomain/credit/mocks/creditApplicationClaim.mockchain';

import { BankID, ClaimState } from 'auto-core/types/TCreditBroker';

import MyCreditsSravniSnippet from './MyCreditsSravniSnippet';

const Context = createContextProvider(contextMock);

const banks = [
    bankMock().withBankID(BankID.TINKOFF).value(),
    bankMock().withBankID(BankID.SRAVNIRU).value(),
];

const creditProduct = creditProductMock()
    .withBankID(BankID.SRAVNIRU)
    .withID('product-test-1')
    .value();

const creditApplicationClaimLoaded = creditApplicationClaimMock()
    .withProductID('product-test-1')
    .withState(ClaimState.APPROVED)
    .withBankPayload({
        sravni_ru: {
            redirect_url: 'url',
            short_redirect_url: 'url',
        },
    })
    .value();

const creditApplicationClaimNotLoaded = creditApplicationClaimMock()
    .withProductID('product-test-1')
    .withState(ClaimState.DRAFT)
    .value();

const store = mockStore({});

//
// при вызове onCloseHandler скрывает фрейм

it('рендерит лоадер сразу, если есть клейм без стейта, и начинает поллинг', async() => {
    const creditApplication = creditApplicationMock()
        .withClaim(creditApplicationClaimNotLoaded)
        .value();

    const creditApplicationWithLoadedClaim = creditApplicationMock()
        .withClaim(creditApplicationClaimLoaded)
        .value();

    let promiseResolver: (value: unknown) => void;
    const pollingPromise = new Promise((resolver => promiseResolver = resolver));

    let counter = 0;

    const onCreditApplicationReloadRequest = jest.fn(() => {
        if (counter === 1) {
            promiseResolver(true);
        } else {
            counter++;
        }
    });

    const onAddCreditProduct = jest.fn(() => Promise.resolve());

    const wrapper = mount(
        <Provider store={ store }>
            <Context>
                <MyCreditsSravniSnippet
                    creditApplication={ creditApplication }
                    creditProduct={ creditProduct }
                    banks={ banks }
                    onAddCreditProduct={ onAddCreditProduct }
                    onCreditApplicationReloadRequest={ onCreditApplicationReloadRequest }
                />
            </Context>
        </Provider>,
    );

    expect(wrapper.find('Loader')).not.toBeEmptyRender();

    await pollingPromise.then(async() => {
        await act(async() => {
            wrapper.setProps({
                children: (
                    <Context>
                        <MyCreditsSravniSnippet
                            creditApplication={ creditApplicationWithLoadedClaim }
                            creditProduct={ creditProduct }
                            banks={ banks }
                            onAddCreditProduct={ onAddCreditProduct }
                            onCreditApplicationReloadRequest={ onCreditApplicationReloadRequest }
                        />
                    </Context>
                ),
            });

            wrapper.update();
        });
    });

    expect(onCreditApplicationReloadRequest).toHaveBeenCalledTimes(2);
    expect(onCreditApplicationReloadRequest).toHaveBeenCalledWith({
        withOffers: true,
    });

    const button = wrapper.find('Button');

    const onClick = button.prop('onClick');

    if (onClick) {
        await act(async() => {
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            onClick();

            await sleep(0);
        });
    }

    expect(onAddCreditProduct).toHaveBeenCalledTimes(0);
});

it(`на клике в кнопку рендериит лоадер,
    добавляет к заявке продукт и начинает поллинг,
    по завершении которого показывает фрейм и больше не поллит`, async() => {
    const creditApplication = creditApplicationMock()
        .value();

    const creditApplicationWithLoadedClaim = creditApplicationMock()
        .withClaim(creditApplicationClaimLoaded)
        .value();

    let promiseResolver: (value: unknown) => void;
    const pollingPromise = new Promise((resolver => promiseResolver = resolver));

    let counter = 0;

    const onCreditApplicationReloadRequest = jest.fn(() => {
        if (counter === 2) {
            promiseResolver(true);
        } else {
            counter++;
        }
    });

    const onAddCreditProduct = jest.fn(() => Promise.resolve());

    const wrapper = mount(
        <Provider store={ store }>
            <Context>
                <MyCreditsSravniSnippet
                    creditApplication={ creditApplication }
                    creditProduct={ creditProduct }
                    banks={ banks }
                    onAddCreditProduct={ onAddCreditProduct }
                    onCreditApplicationReloadRequest={ onCreditApplicationReloadRequest }
                />
            </Context>
        </Provider>,
    );

    const button = wrapper.find('Button');

    const onClick = button.prop('onClick');

    if (onClick) {
        await act(async() => {
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            onClick();
        });
    }

    await pollingPromise.then(async() => {
        await act(async() => {
            wrapper.setProps({
                children: (
                    <Context>
                        <MyCreditsSravniSnippet
                            creditApplication={ creditApplicationWithLoadedClaim }
                            creditProduct={ creditProduct }
                            banks={ banks }
                            onAddCreditProduct={ onAddCreditProduct }
                            onCreditApplicationReloadRequest={ onCreditApplicationReloadRequest }
                        />
                    </Context>
                ),
            });

            wrapper.update();

            await sleep(20);

            wrapper.update();
        });
    });

    expect(onAddCreditProduct).toHaveBeenCalledTimes(1);
    expect(onAddCreditProduct).toHaveBeenCalledWith('product-test-1');

    expect(onCreditApplicationReloadRequest).toHaveBeenCalledTimes(3);

    const frameModal = wrapper.find('CreditFrameModal');

    expect(frameModal.prop('visible')).toEqual(true);
});

it('на клике в кнопку, когда клейм загружен, показывает фрейм', async() => {
    const creditApplicationWithLoadedClaim = creditApplicationMock()
        .withClaim(creditApplicationClaimLoaded)
        .value();

    const onCreditApplicationReloadRequest = jest.fn();
    const onAddCreditProduct = jest.fn(() => Promise.resolve());

    const wrapper = mount(
        <Provider store={ store }>
            <Context>
                <MyCreditsSravniSnippet
                    creditApplication={ creditApplicationWithLoadedClaim }
                    creditProduct={ creditProduct }
                    banks={ banks }
                    onAddCreditProduct={ onAddCreditProduct }
                    onCreditApplicationReloadRequest={ onCreditApplicationReloadRequest }
                />
            </Context>
        </Provider>,
    );

    const button = wrapper.find('Button');

    const onClick = button.prop('onClick');

    if (onClick) {
        await act(async() => {
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            onClick();

            await(20);
            wrapper.update();
        });
    }

    const frameModal = wrapper.find('CreditFrameModal');

    expect(onCreditApplicationReloadRequest).toHaveBeenCalledTimes(0);
    expect(onAddCreditProduct).toHaveBeenCalledTimes(0);
    expect(frameModal.prop('visible')).toEqual(true);
});

it('при вызове onCloseHandler скрывает фрейм', async() => {
    const creditApplicationWithLoadedClaim = creditApplicationMock()
        .withClaim(creditApplicationClaimLoaded)
        .value();

    const onCreditApplicationReloadRequest = jest.fn();
    const onAddCreditProduct = jest.fn(() => Promise.resolve());

    const wrapper = mount(
        <Provider store={ store }>
            <Context>
                <MyCreditsSravniSnippet
                    creditApplication={ creditApplicationWithLoadedClaim }
                    creditProduct={ creditProduct }
                    banks={ banks }
                    onAddCreditProduct={ onAddCreditProduct }
                    onCreditApplicationReloadRequest={ onCreditApplicationReloadRequest }
                />
            </Context>
        </Provider>,
    );

    const button = wrapper.find('Button');

    const onClick = button.prop('onClick');

    if (onClick) {
        await act(async() => {
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            onClick();

            await(20);
            wrapper.update();
        });
    }

    const onCloserClick = wrapper.find('CreditFrameModal').prop('onClose');

    if (onCloserClick) {
        await act(async() => {
            // eslint-disable-next-line @typescript-eslint/ban-ts-comment
            // @ts-ignore
            onCloserClick();

            await(20);
            wrapper.update();
        });
    }

    expect(wrapper.find('CreditFrameModal').prop('visible')).toEqual(false);
});
