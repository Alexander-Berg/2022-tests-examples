const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const InfoBubble = require('./InfoBubble');
const mockStore = require('autoru-frontend/mocks/mockStore').default;
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const setCookieToRoot = require('auto-core/react/dataDomain/cookies/actions/setToRoot').default;
jest.mock('auto-core/react/dataDomain/cookies/actions/setToRoot');
setCookieToRoot.mockImplementation(() => () => {});

const TEXT = `Купите историю автомобиля: она содержит полный VIN и госномер (при наличии). ` +
`Во время осмотра проверьте, что фактические VIN и госномер совпадают с указанными в истории.`;
const COOKIE_NAME = 'foo';

let initialState;
let props;
let context;

beforeEach(() => {
    initialState = {
        cookies: { [COOKIE_NAME]: '' },
    };

    props = {
        title: 'Проверяйте VIN и госномер перед покупкой',
        withCloser: false,
        cookieSettings: { name: COOKIE_NAME, expires: 1 },
    };

    context = _.cloneDeep(contextMock);
    context.metrika.sendParams.mockClear();
});

it('правильно рисует компонент с заголовком', () => {
    const page = shallowRenderComponent(props, initialState);

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('правильно рисует компонент без заголовка', () => {
    props.title = undefined;
    const page = shallowRenderComponent(props, initialState);

    expect(shallowToJson(page)).toMatchSnapshot();
});

it('если есть нужная кука то ничего не нарисует', () => {
    props.withCloser = true;
    initialState.cookies[COOKIE_NAME] = 'true';
    const page = shallowRenderComponent(props, initialState);

    expect(page.find('InfoBubble')).toHaveLength(0);
});

describe('если есть крест', () => {
    let page;
    beforeEach(() => {
        props.withCloser = true;
        props.metrikaParams = 'foo,bar';
        page = shallowRenderComponent(props, initialState);
    });

    it('правильно нарисует компонент', () => {
        expect(shallowToJson(page)).toMatchSnapshot();
    });

    describe('при клике на крест', () => {
        beforeEach(() => {
            page.find('CloseButton').simulate('click');
        });

        it('скроет компонент', () => {
            expect(page.find('InfoBubble')).toHaveLength(0);
        });

        it('установит куку', () => {
            expect(setCookieToRoot).toHaveBeenCalledTimes(1);
            expect(setCookieToRoot).toHaveBeenCalledWith(COOKIE_NAME, true, { expires: 1 });
        });

        it('отправит метрику', () => {
            expect(context.metrika.sendParams).toHaveBeenCalledTimes(1);
            expect(context.metrika.sendParams).toHaveBeenCalledWith([ 'foo', 'bar' ]);
        });
    });
});

function shallowRenderComponent(props, initialState) {
    const store = mockStore(initialState);
    const ContextProvider = createContextProvider(context);

    return shallow(
        <ContextProvider>
            <InfoBubble { ...props } store={ store }>{ TEXT }</InfoBubble>
        </ContextProvider>,
    ).dive().dive();
}
