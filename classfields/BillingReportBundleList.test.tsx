import React from 'react';
import userEvent from '@testing-library/user-event';

import { VAS_EVENTS } from 'auto-core/lib/util/vas/dicts';

import { bundles } from 'auto-core/react/dataDomain/billing/mocks/reportBundles';

import { renderComponent } from 'www-billing/react/utils/testUtils';
import { billingContextMock, sendVasEvent } from 'www-billing/react/contexts/billing.mock';

import type { Props } from './BillingReportBundleList';
import BillingReportBundleList from './BillingReportBundleList';

let props: Props;

beforeEach(() => {
    props = {
        reportBundles: bundles,
        selectedBundle: bundles[2],
        onBundleChange: jest.fn(),
        isTokenFetching: false,
    };

    sendVasEvent.mockReset();
});

it('при маунте отправляет событие показа для невыбранных вкладок', async() => {
    await renderComponent(<BillingReportBundleList { ...props }/>);

    expect(billingContextMock.logVasEvent).toHaveBeenCalledTimes(1);
    expect(billingContextMock.logVasEvent).toHaveBeenCalledWith(VAS_EVENTS.show);
    expect(sendVasEvent).toHaveBeenCalledTimes(2);
    expect(sendVasEvent.mock.calls[0][0]).toEqual({ base_price: 197, effective_price: 59, service: 'offers-history-reports-1' });
    expect(sendVasEvent.mock.calls[1][0]).toEqual({ base_price: 990, effective_price: 990, service: 'offers-history-reports-10' });
});

it('при клике отправляет событие клика', async() => {
    const { findByText } = await renderComponent(<BillingReportBundleList { ...props }/>);
    const singleReportItem = await findByText(/Отчёт ПроАвто/i);
    userEvent.click(singleReportItem);

    expect(billingContextMock.logVasEvent).toHaveBeenCalledTimes(2);
    expect(billingContextMock.logVasEvent).toHaveBeenNthCalledWith(2, VAS_EVENTS.click);
    expect(sendVasEvent).toHaveBeenCalledTimes(3);
    expect(sendVasEvent.mock.calls[2][0]).toEqual({ base_price: 197, effective_price: 59, service: 'offers-history-reports-1' });
});
