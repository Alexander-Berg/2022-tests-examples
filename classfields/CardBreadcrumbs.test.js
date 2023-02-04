const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const breadcrumbsPublicApiMock = require('auto-core/react/dataDomain/breadcrumbsPublicApi/mocks/breadcrumbsPublicApi.mock');
const cardMock = require('auto-core/react/dataDomain/card/mocks/card.cars.mock');
const context = require('autoru-frontend/mocks/contextMock').default;
const cloneOfferWithHelpers = require('autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers');

const CardBreadcrumbs = require('./CardBreadcrumbs');

const resetBreadcrumbsMock = jest.fn();

const defaultProps = {
    breadcrumbs: breadcrumbsPublicApiMock,
    offer: cloneOfferWithHelpers(cardMock).value(),
    resetBreadcrumbs: resetBreadcrumbsMock,
};

it('Должен отрендерить хлебные крошки', () => {
    const wrapper = shallowRenderComponent();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('Должен нарисовать хлебные крошки, даже если нет .vehicle_info.tech_param', () => {
    const offer = cloneOfferWithHelpers(cardMock).value();
    delete offer.vehicle_info.tech_param;
    const wrapper = shallowRenderComponent({ ...defaultProps, offer });

    expect(wrapper.find('Link')).toMatchSnapshot();
});

it('должен вызвать resetBreadcrumbs при изменении оффера', () => {
    const wrapper = shallowRenderComponent();
    const anotherOffer = cloneOfferWithHelpers({}).value();

    wrapper.setProps({ offer: anotherOffer });

    expect(resetBreadcrumbsMock).toHaveBeenCalled();
});

it('не должен вызвать resetBreadcrumbs, если не была передана эта функция', () => {
    const mockProps = {
        ...defaultProps,
        resetBreadcrumbs: undefined,
    };

    const wrapper = shallowRenderComponent(mockProps);
    const anotherOffer = cloneOfferWithHelpers({}).value();

    wrapper.setProps({ offer: anotherOffer });

    expect(resetBreadcrumbsMock).toHaveBeenCalledTimes(0);
});

function shallowRenderComponent(props = defaultProps) {
    return shallow(
        <CardBreadcrumbs { ...props }/>,
        { context },
    );
}
