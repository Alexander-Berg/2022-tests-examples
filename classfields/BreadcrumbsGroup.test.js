const React = require('react');
const { shallow } = require('enzyme');
const contextMock = require('autoru-frontend/mocks/contextMock').default;
const { shallowToJson } = require('enzyme-to-json');
const querystring = require('querystring');

const BreadcrumbsGroup = require('./BreadcrumbsGroup');

const propsMock = require('./BreadcrumbsGroup.mock');

contextMock.link = (routeName, routeParams) => `${ routeName }/${ querystring.stringify(routeParams) }`;

let wrapper;
beforeEach(() => {
    const context = {
        ...contextMock,
    };
    wrapper = shallow(<BreadcrumbsGroup { ...propsMock }/>, { context: context });
});

describe('должен отрендерить хлебные крошки:', () => {
    it('есть ссылка на комплектацию', () => {
        wrapper.setProps({ lastLinkEnabled: true });
        expect(shallowToJson(wrapper.find('.BreadcrumbsGroup'))).toMatchSnapshot();
    });
    it('нет ссылки на комплектацию', () => {
        wrapper.setProps({ lastLinkEnabled: false });
        expect(shallowToJson(wrapper.find('.BreadcrumbsGroup'))).toMatchSnapshot();
    });
});

describe('должен отрендерить микроразметку:', () => {
    it('нет ссылки на комплектацию', () => {
        wrapper.setProps({ lastLinkEnabled: true });
        const microdata = wrapper.find('CardBreadcrumbsMicrodata').dive();
        expect(shallowToJson(microdata)).toMatchSnapshot();
    });
});
