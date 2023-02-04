import type { FormEvent } from 'react';
import React, { useContext } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { shallow } from 'enzyme';

import mockStore from 'autoru-frontend/mocks/mockStore';

import { C2bAuctionSubmitStatus } from 'auto-core/react/dataDomain/c2bAuction/types';
import ButtonWithLoader from 'auto-core/react/components/islands/ButtonWithLoader/ButtonWithLoader';
import Checkbox from 'auto-core/react/components/islands/Checkbox/Checkbox';

import { C2bAuctionCarPriceAddress } from '../C2bAuctionCarPriceAddress/C2bAuctionCarPriceAddress';

import { C2bAuctionCarPriceForm } from './C2bAuctionCarPriceForm';

jest.mock('react-redux', () => {
    const ActualReactRedux = jest.requireActual('react-redux');
    return {
        ...ActualReactRedux,
        useSelector: jest.fn(),
        useDispatch: jest.fn(),
    };
});

jest.mock('react', () => {
    const ActualReact = jest.requireActual('react');
    return {
        ...ActualReact,
        useContext: jest.fn(),
    };
});

describe('C2bAuctionAutoForm', () => {
    it('кнопка задизейблена, если не заполнены все поля', async() => {
        const wrapper = renderComponent({
            state: {
                c2bAuction: {
                    submitStatus: C2bAuctionSubmitStatus.DEFAULT,
                },
            },
        });

        const submitButton = wrapper.find(ButtonWithLoader).at(0);
        expect(submitButton.prop('disabled')).toBe(true);
    });

    it('кнопка активна, если заполнены все поля, включен чекбокс и выбран правильный адрес', async() => {

        const wrapper = renderComponent({
            state: {
                c2bAuction: {
                    submitStatus: C2bAuctionSubmitStatus.DEFAULT,
                },
            },
            context: {
                date: [ '12-11-2021' ],
                place: { address: 'moscow' },
                time: '12:00',
            },
        });

        //заполненных инпутов не достаточно для активной кнопки
        let submitButton = wrapper.find(ButtonWithLoader).at(0);
        expect(submitButton.prop('disabled')).toBe(true);

        //включаем чекбокс
        const checkbox = wrapper.find(Checkbox).at(0);
        checkbox.prop('onCheck')?.(true, {});

        //выбираем правильный адрес
        const addressForm = wrapper.find(C2bAuctionCarPriceAddress).at(0);
        addressForm.prop('setAddressError')?.(false);

        submitButton = wrapper.find(ButtonWithLoader).at(0);
        expect(submitButton.prop('disabled')).toBe(false);
    });

    it('сабмитит форму', async() => {
        const submitMock = jest.fn();
        const wrapper = renderComponent({
            state: {
                c2bAuction: {
                    submitStatus: C2bAuctionSubmitStatus.DEFAULT,
                },
            },
            context: {
                date: [ '12-11-2021' ],
                place: { address: 'moscow' },
                time: '12:00',
                handleCreateApplication: submitMock,
            },
        });

        const form = wrapper.find('form').at(0);
        form.prop('onSubmit')?.({ preventDefault: () => {} } as FormEvent);

        expect(submitMock).toHaveBeenCalledTimes(1);
    });

    it('не сабмитит форму, если предыдущий запрос еще не ответил', async() => {
        const submitMock = jest.fn();
        const wrapper = renderComponent({
            state: {
                c2bAuction: {
                    submitStatus: C2bAuctionSubmitStatus.PENDING,
                },
            },
            context: {
                date: [ '12-11-2021' ],
                place: { address: 'moscow' },
                time: '12:00',
                handleCreateApplication: submitMock,
            },
        });

        const form = wrapper.find('form').at(0);
        form.prop('onSubmit')?.({ preventDefault: () => {} } as FormEvent);

        expect(submitMock).toHaveBeenCalledTimes(0);
    });
});

interface Params {
    props?: any;
    state?: any;
    context?: any;
}

const defaultContext = {
    date: [],
    time: '',
};

function renderComponent({
    props = {},
    state = {},
    context = defaultContext,
}: Params) {
    const store = mockStore(state);

    (useSelector as jest.MockedFunction<typeof useSelector>).mockImplementation(
        (selector) => selector(store.getState()),
    );

    (useDispatch as jest.MockedFunction<typeof useDispatch>).mockReturnValue(
        (...args) => store.dispatch(...args),
    );

    (useContext as jest.MockedFunction<typeof useContext>).mockReturnValue(
        context,
    );

    return shallow(
        <C2bAuctionCarPriceForm
            { ...props }
        />,
    );
}
