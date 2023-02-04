jest.mock('auto-core/react/lib/offer/getVehicleName', () => () => 'vehicleName');
jest.mock('auto-core/react/lib/offer/getVin', () => () => 'vin');
jest.mock('auto-core/react/lib/offer/getImageUrls', () => ({ 'default': jest.fn() }));
jest.mock('auto-core/react/lib/offer/getSection', () => jest.fn(() => 'section'));
jest.mock('auto-core/react/lib/offer/getIdHash', () => () => 'idHash');
jest.mock('auto-core/react/lib/offer/getMileAgeFormatted', () => () => 'mileAgeFormatted');
jest.mock('auto-core/react/lib/offer/getTechParamsName', () => () => 'techParamsName');
jest.mock('auto-core/react/lib/complectation/getHumanGearType', () => () => 'humanGearType');
jest.mock('www-cabinet/react/dataDomain/tradeIn/selectors/getUserPhoneNumber', () => () => 'userPhoneNumber');
jest.mock('www-cabinet/react/dataDomain/tradeIn/selectors/getUserName', () => () => 'userName');

const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const TradeInItem = require('./TradeInItem');
const MockDate = require('mockdate');

beforeEach(() => {
    MockDate.set('1988-01-03');

    const getImageUrls = require('auto-core/react/lib/offer/getImageUrls').default;
    getImageUrls.mockImplementation(() => [ 'imageUrls' ]);
});

describe('isBilled тесты', () => {
    it('должен вернуть false, если item.billing_cost === NEW', () => {
        const tree = shallowRenderComponent({
            item: {
                user_offer: {},
                client_offer: {},
                billing_status: 'NEW',
                create_date: '1988-03-01',
            },
        });

        expect(tree.instance().isBilled()).toEqual(false);
    });

    it('должен вернуть false, если item.billing_cost === PENDING', () => {
        const tree = shallowRenderComponent({
            item: {
                user_offer: {},
                client_offer: {},
                billing_status: 'PENDING',
                create_date: '1988-03-01',
            },
        });

        expect(tree.instance().isBilled()).toEqual(false);
    });

    it('должен вернуть true, если item.billing_cost === FREE', () => {
        const tree = shallowRenderComponent({
            item: {
                user_offer: {},
                client_offer: {},
                billing_status: 'FREE',
                create_date: '1988-03-01',
            },
        });

        expect(tree.instance().isBilled()).toEqual(true);
    });

    it('должен вернуть true, если item.billing_cost === PAID', () => {
        const tree = shallowRenderComponent({
            item: {
                user_offer: {},
                client_offer: {},
                billing_status: 'PAID',
                create_date: '1988-03-01',
            },
        });

        expect(tree.instance().isBilled()).toEqual(true);
    });

    it('должен вернуть false, если item.billing_cost === undefined', () => {
        const tree = shallowRenderComponent({
            item: {
                user_offer: {},
                client_offer: {},
                billing_status: undefined,
                create_date: '1988-03-01',
            },
        });

        expect(tree.instance().isBilled()).toEqual(false);
    });
});

describe('render tests', () => {
    it('должен вернуть TradeInItem', () => {
        const tree = shallowRenderComponent({
            hasSeparator: true,
            item: {
                client_offer: { section: 'new' },
                billing_cost: 500,
                user_offer: {},
                billing_status: 'NEW',
                create_date: '1988-01-03',
            },
        });

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен вернуть TradeInItem c Separator', () => {
        const tree = shallowRenderComponent({
            hasSeparator: true,
            item: {
                user_offer: {},
                client_offer: { section: 'new' },
                billing_status: 'PENDING',
                create_date: '1988-01-03',
            },
        });

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен вернуть TradeInItem c Separator и годом', () => {
        const tree = shallowRenderComponent({
            showYear: true,
            hasSeparator: true,
            item: {
                user_offer: {},
                client_offer: { section: 'new' },
                billing_cost: 500,
                billing_status: 'FREE',
                create_date: '1988-01-03',
            },
        });

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен вернуть Item с TradeInItem__imagePlaceholder вместо картинки', () => {
        const getImageUrls = require('auto-core/react/lib/offer/getImageUrls').default;
        getImageUrls.mockImplementation(() => []);

        const tree = shallowRenderComponent({
            item: {
                client_offer: { section: 'new' },
                billing_cost: 500,
                user_offer: {},
                billing_status: 'NEW',
                create_date: '1988-01-03',
            },
        });

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен вернуть Item для section = new', () => {
        const getImageUrls = require('auto-core/react/lib/offer/getImageUrls').default;
        const getSection = require('auto-core/react/lib/offer/getSection');
        getImageUrls.mockImplementation(() => []);
        getSection.mockImplementation(() => 'new');

        const tree = shallowRenderComponent({
            item: {
                client_offer: {
                    section: 'new',
                },
                billing_cost: 500,
                user_offer: {},
                billing_status: 'NEW',
                create_date: '1988-01-03',
            },
        });

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен вернуть Item для section = used', () => {
        const getImageUrls = require('auto-core/react/lib/offer/getImageUrls').default;
        const getSection = require('auto-core/react/lib/offer/getSection');
        getImageUrls.mockImplementation(() => []);
        getSection.mockImplementation(() => 'used');

        const tree = shallowRenderComponent({
            item: {
                client_offer: {
                    section: 'used',
                    state: {
                        mileage: 150,
                    },
                },
                billing_cost: 500,
                user_offer: {},
                billing_status: 'NEW',
                create_date: '1988-01-03',
            },
        });

        expect(shallowToJson(tree)).toMatchSnapshot();
    });

    it('должен вернуть заглушку для описания, если нет поля client_offer', () => {
        const tree = shallowRenderComponent({ item: {} });
        expect(tree.find('.TradeInItem__description').text()).toEqual('Заявка на трейд-ин');
    });
});

function shallowRenderComponent(props) {
    return shallow(
        <TradeInItem { ...props }/>,
        { context: { link: () => 'link to item' } },
    );
}
