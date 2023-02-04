const _ = require('lodash');
const React = require('react');
const BillingFooter = require('./BillingFooter');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const Checkbox = require('auto-core/react/components/islands/Checkbox');

const DEFAULT_PROPS = {
    checkboxVisible: false,
    checkboxChecked: false,
    checkboxDisabled: false,
    licenseUrl: 'http://example.com',
    isMobile: false,
    onRememberCardToggle: () => {},
};

let props;

beforeEach(() => {
    props = _.cloneDeep(DEFAULT_PROPS);
});

it('правильно рисует компонент для десктопа', () => {
    props.isMobile = false;
    const page = shallow(<BillingFooter { ...props }/>);
    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент для мобилки', () => {
    props.isMobile = true;
    const page = shallow(<BillingFooter { ...props }/>);
    expect(shallowToJson(page)).toMatchSnapshot();
});

it('рисует чекбокс "запомнить карту"', () => {
    props.checkboxVisible = true;
    const page = shallow(<BillingFooter { ...props }/>);
    const checkbox = page.find(Checkbox);
    expect(checkbox).toHaveLength(1);
});

it('взводит чекбокс "запомнить карту"', () => {
    props.checkboxVisible = true;
    props.checkboxChecked = true;
    const page = shallow(<BillingFooter { ...props }/>);
    const checkbox = page.find(Checkbox);
    expect(checkbox.prop('checked')).toBe(true);
});

it('дизэблит чекбокс "запомнить карту"', () => {
    props.checkboxVisible = true;
    props.checkboxChecked = true;
    props.checkboxDisabled = true;
    const page = shallow(<BillingFooter { ...props }/>);
    const checkbox = page.find(Checkbox);
    expect(checkbox.prop('disabled')).toBe(true);
});
