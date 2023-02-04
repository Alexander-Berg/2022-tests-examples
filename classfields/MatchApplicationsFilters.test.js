const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const _ = require('lodash');

const MatchApplicationsFilters = require('./MatchApplicationsFilters');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ContextProvider = createContextProvider(contextMock);

const baseProps = {
    dateLimits: { from: '2015-01-03', to: '2015-01-29' },
    onChangeDates: _.noop,
    totalCount: 25,
    totalPrice: 1000,
};

it('должен сделать ссылку на страницу подписок с якорем на раздел заявок на подбор', () => {
    const tree = shallow(
        <ContextProvider>
            <MatchApplicationsFilters
                { ...baseProps }
            />
        </ContextProvider>,
    ).dive();

    const button = tree.find('.MatchApplicationsFilters__button');

    expect(shallowToJson(button)).toMatchSnapshot();
});

it('должен рендерить плейсхолдер "Нет заявок" вместо каунтера с суммой, если их нет', () => {
    const tree = shallow(
        <ContextProvider>
            <MatchApplicationsFilters
                { ...baseProps }
                totalCount={ 0 }
            />
        </ContextProvider>,
    ).dive();

    const info = tree.find('.MatchApplicationsFilters__info');

    expect(shallowToJson(info)).toMatchSnapshot();
});
