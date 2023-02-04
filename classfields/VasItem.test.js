const _ = require('lodash');
const React = require('react');
const VasItem = require('./VasItem');
const Checkbox = require('auto-core/react/components/islands/Checkbox');
const VasAutoProlongStatus = require('../VasAutoProlongStatus');
const DaysLeft = require('auto-core/react/components/common/DaysLeft').default;
const Price = require('auto-core/react/components/common/Price');

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const offer = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const TOP_SERVICE_ID = 'all_sale_toplist';

const DEFAULT_PROPS = {
    service: {
        is_active: false,
    },
    isSelected: false,
    offer: {
        saleId: '12345678-abcdef',
    },
    serviceInfo: offer.service_prices.find(({ service }) => service === TOP_SERVICE_ID),
    serviceID: TOP_SERVICE_ID,
    onToggle: () => {},
};

let props;

beforeEach(() => {
    props = _.cloneDeep(DEFAULT_PROPS);
});

it('правильно рисуется', () => {
    const page = shallow(<VasItem { ...props }/>);
    expect(shallowToJson(page)).toMatchSnapshot();
});

describe('если вас активен', () => {
    let page;

    beforeEach(() => {
        props.service.is_active = true;
        page = shallow(<VasItem { ...props }/>);
    });

    it('нарисует задизейбленный выбранный чекбокс', () => {
        const checkbox = page.find(Checkbox);

        expect(checkbox.prop('disabled')).toBe(true);
        expect(checkbox.prop('checked')).toBe(true);
    });

    it('нарисует статус автопродления', () => {
        const autoProlongationBlock = page.find(VasAutoProlongStatus);

        expect(autoProlongationBlock).toHaveLength(1);
    });

    it('нарисует DaysLeft, если !prolongationAllowed и !prolongation', () => {
        props.service.is_active = true;
        props.service.expire_date = 123;
        props.serviceInfo.prolongation_allowed = false;
        page = shallow(<VasItem { ...props }/>);
        const autoProlongationBlock = page.find(DaysLeft);

        expect(autoProlongationBlock).toHaveLength(1);
    });

    it('не нарисует цены', () => {
        const prices = page.find(Price);

        expect(prices).toHaveLength(0);
    });
});

describe('если вас неактивен', () => {
    let page;

    beforeEach(() => {
        props.service.is_active = false;
        props.serviceInfo.original_price = props.serviceInfo.price;
        props.onToggle = jest.fn();
        page = shallow(<VasItem { ...props }/>);
    });

    it('чекбокс будет активным', () => {
        const checkbox = page.find(Checkbox);

        expect(checkbox.prop('disabled')).toBe(false);
    });

    it('нарисует цену', () => {
        const prices = page.find(Price);

        expect(prices).toHaveLength(1);
    });

    it('напишет сколько опция действует', () => {
        const activePeriodBlock = page.find('.VasItem__activePeriod');

        expect(activePeriodBlock.text()).toBe('Действует 3 дня');
    });

    it('при клике на чекбокс вызовет коллбэк', () => {
        const checkbox = page.find(Checkbox);
        checkbox.simulate('check');

        expect(props.onToggle).toHaveBeenCalledTimes(1);
        expect(props.onToggle).toHaveBeenCalledWith({ isSelected: !props.isSelected, serviceID: props.serviceID });
    });

    it('при клике на текст вызовет коллбэк', () => {
        const itemBody = page.find('.VasItem__body');
        itemBody.simulate('click');

        expect(props.onToggle).toHaveBeenCalledTimes(1);
        expect(props.onToggle).toHaveBeenCalledWith({ isSelected: !props.isSelected, serviceID: props.serviceID });
    });
});

it('если есть скидка нарисует её', () => {
    const page = shallow(<VasItem { ...props }/>);
    const prices = page.find(Price);

    expect(shallowToJson(prices)).toMatchSnapshot();
});
