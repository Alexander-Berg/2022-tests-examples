import React from 'react';
import { act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { PaymentStatusResponse_Status } from '@vertis/schema-registry/ts-types-snake/auto/api/billing/billing_model_v2';

import { PaymentFrameMessageTypes } from 'auto-core/types/TBilling';

import { renderComponent } from 'www-billing/react/utils/testUtils';

import type { Props } from './BillingCloseConfirm';
import BillingCloseConfirm from './BillingCloseConfirm';

let props: Props;
const eventMap: Record<string, any> = {};
const confirmCallbackPromise = Promise.resolve();

beforeEach(() => {
    props = {
        status: PaymentStatusResponse_Status.NEW,
        onCloseConfirmed: jest.fn(() => confirmCallbackPromise),
    };

    jest.spyOn(global, 'addEventListener').mockImplementation((event, cb) => {
        eventMap[event] = cb;
    });

    jest.spyOn(global.parent, 'postMessage');
});

it('по-умолчанию закрыт', async() => {
    const { queryByText } = await renderComponent(<BillingCloseConfirm { ...props }/>);
    const text = queryByText(/Вы уверены, что хотите прекратить оплату?/i);

    expect(text).toBeNull();
});

describe('при получении сообщения от родителя', () => {
    it('если статус платежа NEW покажет сообщение', async() => {
        const { queryByText } = await renderComponent(<BillingCloseConfirm { ...props }/>);
        act(() => {
            eventMap.message({ data: { source: 'parent', type: PaymentFrameMessageTypes.CLOSE_REQUEST } });
        });
        const text = queryByText(/Вы уверены, что хотите прекратить оплату?/i);

        expect(text).not.toBeNull();
        expect(global.parent.postMessage).toHaveBeenCalledTimes(0);
    });

    it('если статус платежа PROCESS покажет сообщение', async() => {
        props.status = PaymentStatusResponse_Status.PROCESS;
        const { queryByText } = await renderComponent(<BillingCloseConfirm { ...props }/>);
        act(() => {
            eventMap.message({ data: { source: 'parent', type: PaymentFrameMessageTypes.CLOSE_REQUEST } });
        });
        const text = queryByText(/Вы уверены, что хотите прекратить оплату?/i);

        expect(text).not.toBeNull();
        expect(global.parent.postMessage).toHaveBeenCalledTimes(0);
    });

    it('при других статусах отправит подтверждение закрытия', async() => {
        props.status = PaymentStatusResponse_Status.PAID;
        const { queryByText } = await renderComponent(<BillingCloseConfirm { ...props }/>);
        act(() => {
            eventMap.message({ data: { source: 'parent', type: PaymentFrameMessageTypes.CLOSE_REQUEST } });
        });
        const text = queryByText(/Вы уверены, что хотите прекратить оплату?/i);

        expect(text).toBeNull();
        expect(global.parent.postMessage).toHaveBeenCalledTimes(1);
        expect(global.parent.postMessage).toHaveBeenCalledWith(
            { source: 'billing_frame', type: PaymentFrameMessageTypes.CLOSE_REQUEST },
            global.location.origin,
        );
    });
});

it('при нажатии на кнопку "Продолжить" скроет сообщение', async() => {
    const { findByText, queryByText } = await renderComponent(<BillingCloseConfirm { ...props }/>);
    act(() => {
        eventMap.message({ data: { source: 'parent', type: PaymentFrameMessageTypes.CLOSE_REQUEST } });
    });
    const button = await findByText(/Продолжить/i);
    userEvent.click(button);

    const text = queryByText(/Вы уверены, что хотите прекратить оплату?/i);
    expect(text).toBeNull();
    expect(global.parent.postMessage).toHaveBeenCalledTimes(0);
});

it('при нажатии на кнопку "Выйти" вызовет проп и отправит сообщение родителю', async() => {
    const { findByText, queryByText } = await renderComponent(<BillingCloseConfirm { ...props }/>);
    act(() => {
        eventMap.message({ data: { source: 'parent', type: PaymentFrameMessageTypes.CLOSE_REQUEST } });
    });
    const button = await findByText(/Выйти/i);
    userEvent.click(button);

    const text = queryByText(/Вы уверены, что хотите прекратить оплату?/i);
    expect(text).not.toBeNull();

    expect(global.parent.postMessage).toHaveBeenCalledTimes(0);
    expect(props.onCloseConfirmed).toHaveBeenCalledTimes(1);

    await confirmCallbackPromise;

    expect(global.parent.postMessage).toHaveBeenCalledTimes(1);
    expect(global.parent.postMessage).toHaveBeenCalledWith(
        { source: 'billing_frame', type: PaymentFrameMessageTypes.CLOSE_REQUEST },
        global.location.origin,
    );
});
