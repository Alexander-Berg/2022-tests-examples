const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
const CallsNumbersFilters = require('./CallsNumbersFilters');
//https://github.com/facebook/jest/issues/3465#issuecomment-449007170
jest.mock('lodash', () => ({ debounce: fn => fn }));
const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

it('должен вернуть 2 фильтра: номер салона и подменный номер', () => {
    const ContextProvider = createContextProvider(contextMock);
    const tree = shallow(
        <ContextProvider>
            <CallsNumbersFilters
                changeRouteParams={ jest.fn() }
                routeParams={{
                    original: '+71111111111',
                    redirect: '+72222222222',
                }}
            />
        </ContextProvider>,
    ).dive();

    expect(shallowToJson(tree)).toMatchSnapshot();
});

it('onOriginalInputChange: должен установить state.originalPhone и вызвать this.submitOriginalPhone', () => {
    const ContextProvider = createContextProvider(contextMock);
    const tree = shallow(
        <ContextProvider>
            <CallsNumbersFilters
                routeParams={{}}
            />
        </ContextProvider>,
    ).dive();
    tree.instance().submitOriginalPhone = jest.fn();
    tree.instance().onOriginalInputChange('8 916 493 99 56');

    expect(tree.instance().state.originalPhone).toBe('8 916 493 99 56');
    expect(tree.instance().submitOriginalPhone).toHaveBeenCalled();
});

it('onRedirectInputChange: должен установить state.redirectPhone и вызвать this.submitRedirectPhone', () => {
    const ContextProvider = createContextProvider(contextMock);
    const tree = shallow(
        <ContextProvider>
            <CallsNumbersFilters
                routeParams={{}}
            />
        </ContextProvider>,
    ).dive();
    tree.instance().submitRedirectPhone = jest.fn();
    tree.instance().onRedirectInputChange('8 916 493 99 56');

    expect(tree.instance().state.redirectPhone).toBe('8 916 493 99 56');
    expect(tree.instance().submitRedirectPhone).toHaveBeenCalled();
});

describe('submitOriginalPhone', () => {
    it('должен сбросить сообщение об ошибке, если originalPhone = ""', () => {
        const changeRouteParams = jest.fn();
        const ContextProvider = createContextProvider(contextMock);
        const tree = shallow(
            <ContextProvider>
                <CallsNumbersFilters
                    changeRouteParams={ changeRouteParams }
                    routeParams={{}}
                />
            </ContextProvider>,
        ).dive();

        tree.instance().state = {
            originalPhone: '',
            originalPhoneError: 'ошибка',
        };

        tree.instance().submitOriginalPhone();

        expect(tree.instance().state.originalPhoneError).toBe('');
    });

    it('должен сбросить сообщение об ошибке, вызвать changeRouteParams и metrika.sendPageEvent, ' +
        'если originalPhone прошел валидацию', () => {
        const changeRouteParams = jest.fn();
        const ContextProvider = createContextProvider(contextMock);

        const tree = shallow(
            <ContextProvider>
                <CallsNumbersFilters
                    changeRouteParams={ changeRouteParams }
                    routeParams={{}}
                />
            </ContextProvider>,
        );
        const callsNumbersFiltersInstance = tree.dive().instance();

        callsNumbersFiltersInstance.state = {
            originalPhone: '8 916 493 99 56',
            originalPhoneError: 'ошибка',
        };

        callsNumbersFiltersInstance.submitOriginalPhone();

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'filters', 'original_phone_search' ]);
        expect(callsNumbersFiltersInstance.state.originalPhoneError).toBe('');
        expect(changeRouteParams).not.toHaveBeenCalledWith({ original: '8 916 493 99 56', page: '1' });
    });

    it('должен установить сообщение об ошибке, если originalPhone не прошел валидацию', () => {
        const changeRouteParams = jest.fn();
        const ContextProvider = createContextProvider(contextMock);

        const tree = shallow(
            <ContextProvider>
                <CallsNumbersFilters
                    changeRouteParams={ changeRouteParams }
                    routeParams={{}}
                />
            </ContextProvider>,
        ).dive();

        tree.instance().state = {
            originalPhone: '8 916',
            originalPhoneError: null,
        };

        tree.instance().submitOriginalPhone();

        expect(tree.instance().state.originalPhoneError).toBe('введите корректный номер телефона');
    });
});

describe('submitRedirectPhone', () => {
    it('должен сбросить сообщение об ошибке, если redirectPhone = ""', () => {
        const changeRouteParams = jest.fn();
        const ContextProvider = createContextProvider(contextMock);

        const tree = shallow(
            <ContextProvider>
                <CallsNumbersFilters
                    changeRouteParams={ changeRouteParams }
                    routeParams={{}}
                />
            </ContextProvider>,
        ).dive();

        tree.instance().state = {
            redirectPhone: '',
            redirectPhoneError: 'ошибка',
        };

        tree.instance().submitRedirectPhone();

        expect(tree.instance().state.redirectPhoneError).toBe('');
    });

    it('должен сбросить сообщение об ошибке, вызвать changeRouteParams и metrika.sendPageEvent, если redirectPhone прошел валидацию', () => {
        const changeRouteParams = jest.fn();
        const ContextProvider = createContextProvider(contextMock);
        const tree = shallow(
            <ContextProvider>
                <CallsNumbersFilters
                    changeRouteParams={ changeRouteParams }
                    routeParams={{}}
                />
            </ContextProvider>,
        );

        const callsNumbersFiltersInstance = tree.dive().instance();
        callsNumbersFiltersInstance.state = {
            redirectPhone: '8 916 493 99 56',
            redirectPhoneError: 'ошибка',
        };

        callsNumbersFiltersInstance.submitRedirectPhone();

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'filters', 'redirect_phone_search' ]);
        expect(callsNumbersFiltersInstance.state.redirectPhoneError).toBe('');
        expect(changeRouteParams).not.toHaveBeenCalledWith({ redirect: '8 916 493 99 56', page: '1' });
    });

    it('должен установить сообщение об ошибке, если redirectPhone не прошел валидацию', () => {
        const changeRouteParams = jest.fn();
        const ContextProvider = createContextProvider(contextMock);

        const tree = shallow(
            <ContextProvider>
                <CallsNumbersFilters
                    changeRouteParams={ changeRouteParams }
                    routeParams={{}}
                />
            </ContextProvider>,
        ).dive();

        tree.instance().state = {
            redirectPhone: '8 916',
            redirectPhoneError: '',
        };

        tree.instance().submitRedirectPhone();

        expect(tree.instance().state.redirectPhoneError).toBe('введите корректный номер телефона');
    });
});
