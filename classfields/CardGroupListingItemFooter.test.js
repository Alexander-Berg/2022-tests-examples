const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');

const CardGroupListingItemFooter = require('./CardGroupListingItemFooter');

jest.mock('auto-core/react/components/common/TradeinViewButton', () => {
    const tradeInMock = () => <span>TradeIn</span>;
    // TODO: при случае надо бы эти тесты переписать так, чтобы снепшотов было как можно меньше,
    // и не пришлось бы мокать компоненты
    return {
        COLOR: {
            GRAY: 'gray',
            WHITE: 'white',
        },
        'default': tradeInMock,
    };
});

const OFFER = {
    documents: {
        year: 2019,
    },
    category: 'cars',
    additional_info: {},
    price_info: {
        RUR: 2260700,
    },
    color_hex: 200204,
    vehicle_info: {
        availability: 'IN_STOCK',
    },
    seller: {
        name: 'Автомир MITSUBISHI Крылатское',
        location: {
            region_info: {
                name: 'Москва',
            },
        },
    },
    salon: {
        is_official: true,
    },
};

const DEALER_PAGE_URL = 'https://auto.ru/diler-oficialniy/cars/all/avtomir_moskva_mitsubishi_103/?from=auto-snippet';

it('должен корректно отрендерится', () => {
    const tree = shallow(
        <CardGroupListingItemFooter
            offer={ OFFER }
            dealerPageUrl={ DEALER_PAGE_URL }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('должен корректно отрендерится с кнопкой обратного звонка', () => {
    const offer = { ...OFFER, seller_type: 'COMMERCIAL' };
    const tree = shallow(
        <CardGroupListingItemFooter
            offer={ offer }
            dealerPageUrl={ DEALER_PAGE_URL }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('не должен рендерить кнопку обратного звонка, если для этого салона обратные звонки запрещены', () => {
    const offer = _.cloneDeep(OFFER);

    offer.seller_type = 'COMMERCIAL';
    offer.salon.phone_callback_forbidden = true;

    const tree = shallow(
        <CardGroupListingItemFooter
            offer={ offer }
            dealerPageUrl={ DEALER_PAGE_URL }
        />,
    );
    expect(shallowToJson(tree)).toMatchSnapshot();
});
