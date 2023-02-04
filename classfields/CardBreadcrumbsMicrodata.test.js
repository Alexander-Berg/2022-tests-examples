const React = require('react');

const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;

const CardBreadcrumbsMicrodata = require('./CardBreadcrumbsMicrodata');

let Context;
let getLinkParams;
beforeEach(() => {
    getLinkParams = () => ({ name: 'markName', route: 'routeName' });
    Context = createContextProvider(contextMock);
});

it('должен отрисовать доступные уровни', () => {
    const wrapper = shallow(
        <Context>
            <CardBreadcrumbsMicrodata
                getLinkParams={ getLinkParams }
                levels={ [ { name: 'MARK_LEVEL' } ] }
            />
        </Context>,
    ).dive();

    expect(shallowToJson(wrapper.find('.CardBreadcrumbsMicrodata'))).toMatchSnapshot();
});

it('не должен отрисовать уровни, если они ничего не вернули', () => {
    getLinkParams = (level) => {
        if (level === 'MARK_LEVEL') {
            return { name: 'markName', route: 'routeName' };
        }
        return null;
    };
    const wrapper = shallow(
        <Context>
            <CardBreadcrumbsMicrodata
                getLinkParams={ getLinkParams }
                levels={ [ { name: 'MARK_LEVEL' }, { name: 'MODEL_LEVEL' } ] }
            />
        </Context>,
    ).dive();

    expect(shallowToJson(wrapper.find('.CardBreadcrumbsMicrodata'))).toMatchSnapshot();
});
