const React = require('react');
const ClientsDumb = require('./ClientsDumb');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');
jest.mock('auto-core/react/actions/scroll', () => jest.fn());

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const ContextProvider = createContextProvider({
    linkCabinet: () => 'linkToCabinet',
});

const baseProps = {
    isLoading: false,
    listing: [ { clientData: { id: 'clientId' } } ],
    filterPresets: [
        {
            name: 'Все',
            facet: 'all',
            count: 155,
        },
    ],
    pagination: {
        page_num: 1,
        total_page_count: 10,
        page_size: 10,
    },
};

it('должен вернуть набор комопонетов: фильтры, сортировки, листинг, пагинацию и модал автопродления', () => {
    expect(shallowToJson(shallow(
        <ClientsDumb
            { ...baseProps }
        />,
    ))).toMatchSnapshot();
});

it('changeRouteParams: должен вызвать router.replace с параметрами', () => {
    const router = {
        replace: jest.fn(),
    };
    const ClientsDumbInstance = shallow(
        <ContextProvider>
            <ClientsDumb
                { ...baseProps }
                router={ router }
                routeName="cabinet"
            />
        </ContextProvider>,
    ).dive().instance();

    ClientsDumbInstance.changeRouteParams({});
    expect(router.replace).toHaveBeenCalledWith('linkToCabinet');
});

it('onPageButtonClick: должен вызвать changeRouteParams с параметрами', () => {
    const router = {
        replace: jest.fn(),
    };
    const scrollTo = require('auto-core/react/actions/scroll');
    scrollTo.mockClear();
    const clientsDumbInstance = shallow(
        <ContextProvider>
            <ClientsDumb
                { ...baseProps }
                router={ router }
                routeName="cabinet"
            />
        </ContextProvider>,
    ).dive().instance();
    clientsDumbInstance.changeRouteParams = jest.fn();

    clientsDumbInstance.onPageButtonClick(1);

    expect(scrollTo).toHaveBeenCalled();
    expect(clientsDumbInstance.changeRouteParams).toHaveBeenCalledWith({ page: 1 });
});

it('onSortClick: должен вызвать changeRouteParams с параметрами', () => {
    const router = {
        replace: jest.fn(),
    };
    const clientsDumbInstance = shallow(
        <ContextProvider>
            <ClientsDumb
                { ...baseProps }
                router={ router }
                routeName="cabinet"
            />
        </ContextProvider>,
    ).dive().instance();
    clientsDumbInstance.changeRouteParams = jest.fn();

    clientsDumbInstance.onSortClick({ sort: 'date', sort_dir: 'desc' });

    expect(clientsDumbInstance.changeRouteParams).toHaveBeenCalledWith({ sort: 'date', sort_dir: 'desc' });
});
