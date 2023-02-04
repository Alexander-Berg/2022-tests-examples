/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

// Components
const SearchLineSuggest = require('./SearchLineSuggest');
const TextInput = require('auto-core/react/components/islands/TextInput');

// Mock data
const historyMock = require('auto-core/react/dataDomain/searchline/mocks/history.mock.json');
const searchlineFakePopularItems = require('auto-core/react/dataDomain/searchline/searchlineFakePopularItems');
const suggestDataMock = require('auto-core/react/dataDomain/searchline/mocks/suggest.mock.json');

// Context mocks
import contextMock from 'autoru-frontend/mocks/contextMock';

const enterKeyEvent = {
    key: 'Enter',
    preventDefault: () => {},
};

// Function mocks
function addToHistory() {
    return Promise.resolve({
        status: 'SUCCESS',
    });
}

function getHistory() {
    return Promise.resolve(historyMock);
}

function getPopularItems() {
    return searchlineFakePopularItems.slice(0, 5);
}

function getSuggestData({ query }) {
    const suggestData = {
        ...suggestDataMock,
        query,
    };

    return Promise.resolve(suggestData);
}

// Test cases
it('при выборе элемента из результатов поиска, должен добавить элемент в историю и вызвать props.onSelect', () => {
    const mockResolve = Promise.resolve({
        status: 'SUCCESS',
    });
    const addToHistorySpied = jest.fn(() => mockResolve);
    const onSelectSpied = jest.fn();

    const wrapper = shallow(
        <SearchLineSuggest
            addToHistory={ addToHistorySpied }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            onSelect={ onSelectSpied }
        />,
        { context: contextMock },
    );

    wrapper.setState({
        suggestLoading: false,
        inputValue: 'A4',
        suggestData: suggestDataMock,
    });

    wrapper.find(TextInput).simulate('focusChange', true);

    const itemClickEvent = {
        currentTarget: {
            getAttribute: () => 0,
        },
        nativeEvent: {
            stopImmediatePropagation: () => { },
        },
    };
    wrapper.find('.RichInput__suggest-item').at(0).simulate('click', itemClickEvent);

    return mockResolve.then(() => {
        expect(onSelectSpied).toHaveBeenCalledTimes(1);
        expect(onSelectSpied).toHaveBeenCalledWith(suggestDataMock.suggests[0]);
    });
});

it('должен запросить историю поиска после монтирования компонента, когда текст запроса пустой, а саджест не запрашивать', () => {
    const getHistorySpied = jest.fn(getHistory);
    const getSuggestDataSpied = jest.fn(getSuggestData);

    const wrapper = shallow(
        <SearchLineSuggest
            addToHistory={ addToHistory }
            getHistory={ getHistorySpied }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestDataSpied }
            initialValue=""
        />,
        { context: contextMock },
    );

    wrapper.instance().debouncedRequestSuggestData.flush();

    expect(getHistorySpied).toHaveBeenCalledTimes(1);
    expect(getSuggestDataSpied).toHaveBeenCalledTimes(0);
});

it('должен запросить результаты после монтирования компонента, когда текст запроса непустой, а историю не запрашивать', () => {
    const getHistorySpied = jest.fn(getHistory);
    const getSuggestDataSpied = jest.fn(getSuggestData);

    const wrapper = shallow(
        <SearchLineSuggest
            addToHistory={ addToHistory }
            getHistory={ getHistorySpied }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestDataSpied }
            initialValue="A4"
        />,
        { context: contextMock },
    );

    wrapper.instance().debouncedRequestSuggestData.flush();
    expect(getHistorySpied).toHaveBeenCalledTimes(0);
    expect(getSuggestDataSpied).toHaveBeenCalledTimes(1);
});

it('если запрос за историей зафейлится, должен вернуть популярные запросы', () => {
    const promiseRejected = Promise.reject('ooops');
    const getHistoryFailed = jest.fn(() => promiseRejected);

    const wrapper = shallow(
        <SearchLineSuggest
            addToHistory={ addToHistory }
            getHistory={ getHistoryFailed }
            getPopularItems={ getPopularItems }
        />,
        { context: contextMock },
    );

    return wrapper.instance().getSuggestData().then((res) => {
        const items = res.suggests.map(item => ({
        // Нас интересуют только эти два поля
            title: item.title,
            query: item.query,
        }));

        expect(items).toEqual([ {
            query: 'BMW дизель',
            title: 'Популярные запросы',
        },
        {
            query: 'Ауди ку 7',
            title: null,
        },
        {
            query: 'Механическая коробка до 500000 руб купе',
            title: null,
        },
        {
            query: 'Седан полный привод от 2015 года автомат',
            title: null,
        },
        {
            query: 'Белая Toyota Camry',
            title: null,
        } ]);
    });
});

it('при нажатии Enter должен вызвать newSearchWillBeApplied, если есть выполняющийся запрос', () => {
    const wrapper = shallow(
        <SearchLineSuggest
            addToHistory={ addToHistory }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
        />,
        { context: contextMock },
    );
    const instance = wrapper.instance();
    instance.newSearchWillBeApplied = jest.fn();

    wrapper.setState({
        suggestLoading: true,
    });
    wrapper.find(TextInput).simulate('keyDown', enterKeyEvent);

    expect(instance.newSearchWillBeApplied).toHaveBeenCalledTimes(1);
});

it('при нажатии Enter должен вызвать doSelectByIndex, если нет выполняющегося запроса', () => {
    const wrapper = shallow(
        <SearchLineSuggest
            addToHistory={ addToHistory }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
        />,
        { context: contextMock },
    );
    const instance = wrapper.instance();
    instance.doSelectByIndex = jest.fn();

    wrapper.setState({
        suggestLoading: false,
    });
    wrapper.find(TextInput).simulate('keyDown', enterKeyEvent);

    expect(instance.doSelectByIndex).toHaveBeenCalledTimes(1);
});

it('при нажатии Enter: если есть результаты поиска, должен добавить первый результат в историю', () => {
    const mockResolve = Promise.resolve({
        status: 'SUCCESS',
    });
    const addToHistorySpied = jest.fn(() => mockResolve);

    const wrapper = shallow(
        <SearchLineSuggest
            addToHistory={ addToHistorySpied }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
        />,
        { context: contextMock },
    );

    wrapper.setState({
        popupVisible: true,
        suggestLoading: false,
        inputValue: 'A4',
        suggestData: suggestDataMock,
    });

    wrapper.find(TextInput).simulate('keyDown', enterKeyEvent);

    return mockResolve.then(() => {
        expect(addToHistorySpied).toHaveBeenCalledTimes(1);
        expect(addToHistorySpied.mock.calls[0]).toMatchSnapshot();
    });
});

it('при нажатии Enter: если есть результаты поиска, должен вызвать onSelect с первым элементом результата', () => {
    const mockResolve = Promise.resolve({
        status: 'SUCCESS',
    });
    const addToHistorySpied = jest.fn(() => mockResolve);
    const onSelectSpied = jest.fn();

    const wrapper = shallow(
        <SearchLineSuggest
            addToHistory={ addToHistorySpied }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            onSelect={ onSelectSpied }
        />,
        { context: contextMock },
    );

    wrapper.setState({
        popupVisible: true,
        suggestLoading: false,
        inputValue: 'A001AA05',
        suggestData: suggestDataMock,
    });

    wrapper.find(TextInput).simulate('keyDown', enterKeyEvent);

    return mockResolve.then(() => {
        expect(onSelectSpied).toHaveBeenCalledTimes(1);
        expect(onSelectSpied).toHaveBeenCalledWith(suggestDataMock.suggests[0]);
    });
});

it('должен при добавлении результата в историю брать категорию из пропсов', () => {
    const CATEGORY = 'MOTO';
    const mockResolve = Promise.resolve({
        status: 'SUCCESS',
    });
    const addToHistorySpied = jest.fn(() => mockResolve);

    const wrapper = shallow(
        <SearchLineSuggest
            addToHistory={ addToHistorySpied }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            category={ CATEGORY }
        />,
        { context: contextMock },
    );

    wrapper.find(TextInput).simulate('change', 'Ducati');
    wrapper.find(TextInput).simulate('keyDown', enterKeyEvent);

    return mockResolve.then(() => {
        expect(addToHistorySpied.mock.calls[0][0]).toMatchObject({ category: CATEGORY });
    });
});

it('должен при добавлении результата в историю указывать категорию CARS, если категории нет в пропсах', () => {
    const mockResolve = Promise.resolve({
        status: 'SUCCESS',
    });
    const addToHistorySpied = jest.fn(() => mockResolve);

    const wrapper = shallow(
        <SearchLineSuggest
            addToHistory={ addToHistorySpied }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
        />,
        { context: contextMock },
    );

    wrapper.find(TextInput).simulate('change', 'BMW X7');
    wrapper.find(TextInput).simulate('keyDown', enterKeyEvent);

    return mockResolve.then(() => {
        expect(addToHistorySpied.mock.calls[0][0]).toMatchObject({ category: 'CARS' });
    });
});

describe('Эксперимент с ПроАвто', () => {
    const localContextMock = {
        ...contextMock,
        hasExperiment: (exp) => exp === 'AUTORUFRONT-18481_shapka_proauto',
    };

    let originalWindowLocation;

    beforeEach(() => {
        originalWindowLocation = global.window.location;
        delete global.window.location;

        global.window.location = {};
    });

    afterEach(() => {
        global.window.location = originalWindowLocation;
    });

    it('при нажатии Enter должен средиректить в ПроАвто, если введён госномер', () => {
        const mockResolve = Promise.resolve({
            status: 'SUCCESS',
        });
        const addToHistorySpied = jest.fn(() => mockResolve);
        const onSelectSpied = jest.fn();

        const wrapper = shallow(
            <SearchLineSuggest
                addToHistory={ addToHistorySpied }
                getHistory={ getHistory }
                getPopularItems={ getPopularItems }
                getSuggestData={ getSuggestData }
                onSelect={ onSelectSpied }
            />,
            { context: localContextMock },
        );

        wrapper.setState({
            popupVisible: true,
            suggestLoading: false,
            inputValue: 'A001AA05',
            suggestData: suggestDataMock,
        });

        wrapper.find(TextInput).simulate('keyDown', enterKeyEvent);

        return mockResolve.then(() => {
            expect(onSelectSpied).toHaveBeenCalledTimes(0);
            expect(addToHistorySpied).toHaveBeenCalledTimes(0);

            expect(global.window.location.href).toBe('link/proauto-report/?history_entity_id=A001AA05&from=searchline_suggest');
        });
    });

    it('при нажатии Enter должен средиректить в ПроАвто, если введён VIN', () => {
        const mockResolve = Promise.resolve({
            status: 'SUCCESS',
        });
        const addToHistorySpied = jest.fn(() => mockResolve);
        const onSelectSpied = jest.fn();

        const wrapper = shallow(
            <SearchLineSuggest
                addToHistory={ addToHistorySpied }
                getHistory={ getHistory }
                getPopularItems={ getPopularItems }
                getSuggestData={ getSuggestData }
                onSelect={ onSelectSpied }
            />,
            { context: localContextMock },
        );

        wrapper.setState({
            popupVisible: true,
            suggestLoading: false,
            inputValue: 'JHMGD18508S219366',
            suggestData: suggestDataMock,
        });

        wrapper.find(TextInput).simulate('keyDown', enterKeyEvent);

        return mockResolve.then(() => {
            expect(onSelectSpied).toHaveBeenCalledTimes(0);
            expect(addToHistorySpied).toHaveBeenCalledTimes(0);

            expect(global.window.location.href).toBe('link/proauto-report/?history_entity_id=JHMGD18508S219366&from=searchline_suggest');
        });
    });

    it('при нажатии Enter не должен вызывать методы выбора элементов или сохранения в историю, если введён госномер', () => {
        const mockResolve = Promise.resolve({
            status: 'SUCCESS',
        });
        const addToHistorySpied = jest.fn(() => mockResolve);
        const onSelectSpied = jest.fn();

        const wrapper = shallow(
            <SearchLineSuggest
                addToHistory={ addToHistorySpied }
                getHistory={ getHistory }
                getPopularItems={ getPopularItems }
                getSuggestData={ getSuggestData }
                onSelect={ onSelectSpied }
            />,
            { context: localContextMock },
        );
        const instance = wrapper.instance();
        instance.doSelectByIndex = jest.fn();

        wrapper.setState({
            popupVisible: true,
            suggestLoading: false,
            inputValue: 'JHMGD18508S219366',
            suggestData: suggestDataMock,
        });
        wrapper.find(TextInput).simulate('keyDown', enterKeyEvent);

        return mockResolve.then(() => {
            expect(instance.doSelectByIndex).toHaveBeenCalledTimes(0);
            expect(onSelectSpied).toHaveBeenCalledTimes(0);
            expect(addToHistorySpied).toHaveBeenCalledTimes(0);
        });
    });

    it('при нажатии Enter: если есть результаты поиска, должен вызвать onSelect с первым элементом результата', () => {
        const mockResolve = Promise.resolve({
            status: 'SUCCESS',
        });
        const addToHistorySpied = jest.fn(() => mockResolve);
        const onSelectSpied = jest.fn();

        const wrapper = shallow(
            <SearchLineSuggest
                addToHistory={ addToHistorySpied }
                getHistory={ getHistory }
                getPopularItems={ getPopularItems }
                getSuggestData={ getSuggestData }
                onSelect={ onSelectSpied }
            />,
            { context: localContextMock },
        );

        wrapper.setState({
            popupVisible: true,
            suggestLoading: false,
            inputValue: 'A4',
            suggestData: suggestDataMock,
        });

        wrapper.find(TextInput).simulate('keyDown', enterKeyEvent);

        return mockResolve.then(() => {
            expect(onSelectSpied).toHaveBeenCalledTimes(1);
            expect(onSelectSpied).toHaveBeenCalledWith(suggestDataMock.suggests[0]);
        });
    });

    it('при нажатии Enter: если есть результаты поиска, должен добавить первый результат в историю', () => {
        const mockResolve = Promise.resolve({
            status: 'SUCCESS',
        });
        const addToHistorySpied = jest.fn(() => mockResolve);

        const wrapper = shallow(
            <SearchLineSuggest
                addToHistory={ addToHistorySpied }
                getHistory={ getHistory }
                getPopularItems={ getPopularItems }
                getSuggestData={ getSuggestData }
            />,
            { context: localContextMock },
        );

        wrapper.setState({
            popupVisible: true,
            suggestLoading: false,
            inputValue: 'A4',
            suggestData: suggestDataMock,
        });

        wrapper.find(TextInput).simulate('keyDown', enterKeyEvent);

        return mockResolve.then(() => {
            expect(addToHistorySpied).toHaveBeenCalledTimes(1);
            expect(addToHistorySpied.mock.calls[0]).toMatchSnapshot();
        });
    });

    it('Если введён вин, вместо результатов должен отрисовать ссылку на ПроАвто', () => {
        const wrapper = shallow(
            <SearchLineSuggest
                addToHistory={ addToHistory }
                getHistory={ getHistory }
                getPopularItems={ getPopularItems }
                getSuggestData={ getSuggestData }
            />,
            { context: localContextMock },
        );

        wrapper.setState({
            popupVisible: true,
            suggestLoading: false,
            inputValue: 'JHMGD18508S219366',
            suggestData: suggestDataMock,
        });

        expect(wrapper.find('Link').prop('url')).toBe('link/proauto-report/?from=searchline_suggest&history_entity_id=JHMGD18508S219366');
    });
});
