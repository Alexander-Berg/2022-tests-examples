/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import 'jest-enzyme';

import React from 'react';
import { Provider } from 'react-redux';
import { shallow } from 'enzyme';
import MockDate from 'mockdate';

import mockStore from 'autoru-frontend/mocks/mockStore';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import contextMock from 'autoru-frontend/mocks/contextMock';

import offerMock from 'auto-core/react/dataDomain/card/mocks/card.cars.2.mock';
import cardVinReportFree from 'auto-core/react/dataDomain/vinReport/mocks/freeReport-mercedes.mock';

import ResellerSalesItemVinReportDumb from './ResellerSalesItemVinReportDumb';
import type { Props, State } from './ResellerSalesItemVinReportDumb';

let props: Props;
const defaultStore = mockStore({
    state: { paymentModalOpened: false },
    user: { data: {} },
});
const defaultOffer = cloneOfferWithHelpers(offerMock).value();

beforeEach(() => {
    props = {
        offer: defaultOffer,
        className: 'TestClassName',
        isPaymentModalOpened: false,
    };
});

afterEach(() => {
    MockDate.reset();
});

it('откроет попап по клику', () => {
    const page = shallowRenderComponent({ props });

    expect(page.find('Popup')).toHaveProp('visible', false);

    page.simulate('mouseEnter');

    expect(page.find('Popup')).toHaveProp('visible', true);
});

it('внутри попапа отрендерит VinHistory', () => {
    const page = shallowRenderComponent({ props });

    expect(page.find('Popup').dive().find('Connect(VinHistoryDumb)')).not.toExist();

    page.simulate('mouseEnter');

    // нет смысла тестить отправку запросов, так как VinHistory уже обвешан такими тестами
    // проверяем только, что при наведении внутри попапа рендерится VinHistory
    expect(page.find('Popup').dive().find('Connect(VinHistoryDumb)')).toExist();
});

it('попап не будет ичезать, если откроем PaymentModal', () => {
    const page = shallowRenderComponent({ props });
    page.setState({ vinReport: cardVinReportFree });

    page.simulate('mouseEnter');

    const actionButton = page.find('Popup').dive().find('Connect(VinHistoryDumb)').dive().dive().dive().find('VinReportActionButton');
    actionButton.simulate('click');

    expect((page.state() as State).shouldFreezePopup).toEqual(true);

    page.simulate('mouseLeave');

    expect((page.state() as State).isPopupVisible).toEqual(true);
});

it('попап исчезнет, если закрыли PaymentModal', () => {
    return new Promise<void>((done) => {
        const page = shallowRenderComponent({ props: { ...props, isPaymentModalOpened: true } });
        page.setState({ shouldFreezePopup: true });

        page.setProps({ isPaymentModalOpened: false });
        page.update();

        expect(page.state().shouldFreezePopup).toBe(false);

        // попап скрывается через таймаут в 100. Подождем-с
        setTimeout(() => {
            expect(page.state('isPopupVisible')).toBe(false);

            done();
        }, 150);
    });
});

function shallowRenderComponent({ props }: { props: Props }): any {
    const Context = createContextProvider(contextMock);

    const page = shallow(
        <Provider store={ defaultStore }>
            <Context>
                <ResellerSalesItemVinReportDumb { ...props }/>
            </Context>
        </Provider>,
        { context: contextMock },
    );

    return page.dive().dive();
}
