jest.mock('auto-core/react/dataDomain/state/actions/paymentModalOpen');

import React from 'react';
import type { Action } from 'redux';
import { shallow } from 'enzyme';

import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import paymentModalOpen from 'auto-core/react/dataDomain/state/actions/paymentModalOpen';

import type { Props } from './SalesVasFresh';
import SalesVasFresh from './SalesVasFresh';

const paymentModalOpenMock = paymentModalOpen as jest.MockedFunction<typeof paymentModalOpen>;
paymentModalOpenMock.mockReturnValue((() => () => { }) as unknown as Action);

const Context = createContextProvider(contextMock);

const defaultOffer = cloneOfferWithHelpers(offerMock).withActiveVas([]).value();
const defaultProps = {
    offer: defaultOffer,
    openPaymentModal: paymentModalOpenMock,
    pageType: 'sales',
};

describe('модал оплаты по клику на ВАС', () => {
    it('откроет модал с нужными параметрами', () => {

        const page = renderShallowWrapper(defaultProps);

        page.find('Button').simulate('click');

        expect(paymentModalOpenMock).toHaveBeenCalledWith({
            category: 'cars',
            from: 'desktop-lk-tooltip',
            offerId: '1085562758-1970f439',
            platform: 'PLATFORM_DESKTOP',
            services: [
                {
                    service: 'all_sale_fresh',
                },
            ],
            shouldShowSuccessTextAfter: true,
            shouldUpdateOfferAfter: undefined,
            successText: 'Опция успешно активирована',
        });
    });

    it('откроет модал с from=desktop-lk-reseller в перекупе', () => {
        const page = renderShallowWrapper({ ...defaultProps, pageType: 'reseller-sales' });

        page.find('Button').simulate('click');

        expect(paymentModalOpenMock.mock.calls[0][0].from).toEqual('desktop-lk-reseller');
    });

    it('откроет модал с from=desktop-lk-reseller-tooltip с тултипа в перекупе', () => {
        const page = renderShallowWrapper({ ...defaultProps, pageType: 'reseller-sales', isFromTooltip: true });

        page.find('Button').simulate('click');

        expect(paymentModalOpenMock.mock.calls[0][0].from).toEqual('desktop-lk-reseller-tooltip');
    });

    it('откроет модал с from=desktop-lk-reseller-tooltip на карточке', () => {
        const page = renderShallowWrapper({ ...defaultProps, isCardPage: true });

        page.find('Button').simulate('click');

        expect(paymentModalOpenMock.mock.calls[0][0].from).toEqual('desktop-card-tooltip');
    });
});

function renderShallowWrapper(props: Props) {
    return shallow(
        <Context>
            <SalesVasFresh { ...props }/>
        </Context>,
    ).dive();
}
