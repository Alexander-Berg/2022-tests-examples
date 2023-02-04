const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const PlacementAutoProlongationInactiveNotice = require('./PlacementAutoProlongationInactiveNotice');

const defaultProps = {
    offer: {
        service_prices: [ { service: 'all_sale_activate', prolongation_forced_not_togglable: true } ],
        services: [ { service: 'all_sale_activate', prolongable: false, is_active: true } ],
        additional_info: { expire_date: 1561965602587 },
        status: 'ACTIVE',
    },
    isMobile: false,
};

it('правильно рисует компонент', () => {
    const props = _.cloneDeep(defaultProps);
    const page = shallowRenderComponent(props);

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент для мобилки', () => {
    const props = _.cloneDeep(defaultProps);
    props.isMobile = true;
    const page = shallowRenderComponent(props);

    expect(shallowToJson(page)).toMatchSnapshot();
});

describe('ничего не нарисует', () => {
    it('если активация неактивна', () => {
        const props = _.cloneDeep(defaultProps);
        props.offer.services[0].is_active = false;
        const page = shallowRenderComponent(props);

        expect(page.html()).toBeNull();
    });

    it('если автопродление подключено', () => {
        const props = _.cloneDeep(defaultProps);
        props.offer.services[0].prolongable = true;
        const page = shallowRenderComponent(props);

        expect(page.html()).toBeNull();
    });

    it('если автопродление не форсируется', () => {
        const props = _.cloneDeep(defaultProps);
        props.offer.service_prices[0].prolongation_forced_not_togglable = false;
        const page = shallowRenderComponent(props);

        expect(page.html()).toBeNull();
    });

    it('если статус объявления INACTIVE', () => {
        const props = _.cloneDeep(defaultProps);
        props.offer.status = 'INACTIVE';
        const page = shallowRenderComponent(props);

        expect(page.html()).toBeNull();
    });
});

it('при клике на кнопку вызовет коллбэк из пропсов', () => {
    const props = _.cloneDeep(defaultProps);
    props.onProlongationButtonClick = jest.fn();
    const page = shallowRenderComponent(props);
    const button = page.find('Button');

    expect(props.onProlongationButtonClick).not.toHaveBeenCalled();
    button.simulate('click');
    expect(props.onProlongationButtonClick).toHaveBeenCalledTimes(1);
});

function shallowRenderComponent(props) {
    return shallow(
        <PlacementAutoProlongationInactiveNotice { ...props }/>,
    );
}
