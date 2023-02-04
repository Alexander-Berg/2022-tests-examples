const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const createContextProvider = require('autoru-frontend/mocks/createContextProvider').default;
const contextMock = require('autoru-frontend/mocks/contextMock').default;

const ReviewsWidget = require('./ReviewsWidget');
const responseMock = require('./responseMock');

const props = {
    isMobile: false,
    options: {
        configuration: '7306610',
    },
};

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

const getResource = require('auto-core/react/lib/gateApi').getResource;

const getJournalReviewsWidget = jest.fn(() => Promise.resolve([]));
getResource.mockImplementation(getJournalReviewsWidget);

it('Не рендерит виджет отзывов без данных', () => {

    const pr1 = Promise.resolve([]);
    getJournalReviewsWidget.mockImplementationOnce(() => pr1);

    const wrapper = shallowRenderReviewsWidget();

    return pr1.then(() => {
        expect(getJournalReviewsWidget).toHaveBeenCalledTimes(1);
        expect(wrapper).toBeEmptyRender();
    });
});

it('Не рендерит виджет, если для тачки нет ни одного отзыва', () => {

    const pr2 = Promise.resolve(
        Object.assign({}, responseMock, { totalCount: 0 }),
    );
    getJournalReviewsWidget.mockImplementationOnce(() => pr2);

    const wrapper = shallowRenderReviewsWidget();

    return pr2.then(() => {
        expect(getJournalReviewsWidget).toHaveBeenCalledTimes(1);
        expect(wrapper).toBeEmptyRender();
    });
});

it('Рендерит виджет с данными об отзывах', () => {

    const pr3 = Promise.resolve(responseMock);
    getJournalReviewsWidget.mockImplementationOnce(() => pr3);

    const wrapper = shallowRenderReviewsWidget();

    return pr3.then(() => {
        expect(getJournalReviewsWidget).toHaveBeenCalledTimes(1);
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });
});

function shallowRenderReviewsWidget() {
    const Context = createContextProvider(contextMock);

    const wrapper = shallow(
        <Context>
            <ReviewsWidget { ...props }/>
        </Context>,
    ).dive();

    return wrapper;
}
