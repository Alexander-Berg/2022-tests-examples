const React = require('react');
const { shallow } = require('enzyme');
jest.mock('auto-core/react/dataDomain/autoPopup/actions/openAutoPopup');
jest.mock('auto-core/react/dataDomain/autoPopup/actions/resetAutoPopup');
jest.mock('auto-core/react/dataDomain/cookies/actions/remove');

const DiscountPromo = require('./DiscountPromo');
const MockDate = require('mockdate');

const mockStore = require('autoru-frontend/mocks/mockStore').default;
const openAutoPopup = require('auto-core/react/dataDomain/autoPopup/actions/openAutoPopup').default;
const resetAutoPopup = require('auto-core/react/dataDomain/autoPopup/actions/resetAutoPopup').default;
const removeCookie = require('auto-core/react/dataDomain/cookies/actions/remove').default;
const { COOKIE_NAME } = require('auto-core/react/components/desktop/AutoPopupLoader/items/vas_discount');
const { AutoPopupNames } = require('auto-core/react/dataDomain/autoPopup/types');

openAutoPopup.mockImplementation(() => () => { });
resetAutoPopup.mockImplementation(() => () => { });
removeCookie.mockImplementation(() => () => { });

const props = {};
let initialState;

beforeEach(() => {
    initialState = {
        servicesDiscount: {
            discountValue: 70,
            // то есть таймзона UTC+3, в ней будет полночь
            discountExpiresDate: '2020-01-10T21:00:00Z',
            saleId: 'my-offer-id',
        },
    };
});

afterEach(() => {
    MockDate.reset();
});

describe('не покажет плашку', () => {
    it('если нет скидки', () => {
        // сидим в москве
        MockDate.set('2020-01-10T20:00:00.000+0300', -180);
        initialState.servicesDiscount.discountValue = undefined;
        const page = shallowRenderComponent({ initialState, props });

        expect(page.isEmptyRender()).toBe(true);
    });

    it('если нет оффера', () => {
        // сидим в москве
        MockDate.set('2020-01-10T20:00:00.000+0300', -180);
        initialState.servicesDiscount.saleId = undefined;
        const page = shallowRenderComponent({ initialState, props });

        expect(page.isEmptyRender()).toBe(true);
    });

    it('если время уже прошло', () => {
        // едем в магадан
        MockDate.set('2020-01-11T11:00:01.000+1100', -660);
        const page = shallowRenderComponent({ initialState, props });

        expect(page.isEmptyRender()).toBe(true);
    });
});

it('скроет плашку когда выйдет время', () => {
    MockDate.set('2020-01-10T20:00:00.000+0300', -180);
    const page = shallowRenderComponent({ initialState });

    expect(page.isEmptyRender()).toBe(false);

    const timer = page.find('Timer');
    timer.simulate('timerFinish');

    expect(page.isEmptyRender()).toBe(true);
});

describe('при клике', () => {
    it('отправит правильные экшены в стор', () => {
        MockDate.set('2020-01-10T20:00:00.000+0300', -180);
        const page = shallowRenderComponent({ initialState });

        page.simulate('click');

        expect(resetAutoPopup).toHaveBeenCalledTimes(1);
        expect(removeCookie).toHaveBeenCalledTimes(1);
        expect(removeCookie).toHaveBeenCalledWith(COOKIE_NAME);
        expect(openAutoPopup).toHaveBeenCalledTimes(1);
        expect(openAutoPopup).toHaveBeenCalledWith({
            id: AutoPopupNames.VAS_DISCOUNT,
            data: initialState.servicesDiscount,
        });
    });
});

function shallowRenderComponent({ initialState, props }) {
    const store = mockStore(initialState);

    return shallow(
        <DiscountPromo { ...props } store={ store }/>
        ,
    ).dive();
}
