/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

// Mock functions
jest.mock('auto-core/react/lib/blockScroll', () => {
    return {
        blockScroll: jest.fn(),
        unblockScroll: jest.fn(),
    };
});

import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';

import contextMock from 'autoru-frontend/mocks/contextMock';

import TextInput from 'auto-core/react/components/islands/TextInput/TextInput';
import historyMock from 'auto-core/react/dataDomain/searchline/mocks/history.mock';
import searchlineFakePopularItems from 'auto-core/react/dataDomain/searchline/searchlineFakePopularItems';
import suggestDataMock from 'auto-core/react/dataDomain/searchline/mocks/suggest.mock.json';
import { blockScroll, unblockScroll } from 'auto-core/react/lib/blockScroll';

import type { TSearchLineSuggest } from 'auto-core/types/TSearchLineSuggest';

import SearchLineSuggestMobileItem from './SearchLineSuggestMobileItem/SearchLineSuggestMobileItem';
import SearchLineSuggestMobile from './SearchLineSuggestMobile';

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function addToHistory(_arg: unknown) {
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

function getSuggestData(query: string): Promise<TSearchLineSuggest> {
    const suggestData = {
        ...suggestDataMock,
        query,
    } as unknown as TSearchLineSuggest;

    return Promise.resolve(suggestData);
}

function noop() {}

let originalWindowLocation: Location;
beforeEach(() => {
    originalWindowLocation = global.window.location;
    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
    // @ts-ignore
    delete global.window.location;
    global.window.location = {
        ...originalWindowLocation,
        href: 'https://test.br',
    };
});
afterEach(() => {
    global.window.location = originalWindowLocation;
});

// Test cases
it('при выборе элемента из результатов поиска, должен перейти по урлу элемента', () => {
    const mockResolve = Promise.resolve({
        status: 'SUCCESS',
    });
    const addToHistorySpied = jest.fn(() => mockResolve);

    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistorySpied }
            onRequestHide={ noop }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            show={ true }
        />,
        { context: contextMock },
    );

    wrapper.setState({
        pending: false,
    });
    wrapper.find(SearchLineSuggestMobileItem).at(0).simulate('click', suggestDataMock.suggests[0]);

    return mockResolve.then(() => {
        expect(window.location.href).toEqual(suggestDataMock.suggests[0].url);
    });
});

it('при выборе элемента из результатов поиска, должен добавить элемент в историю', () => {
    const mockResolve = Promise.resolve({
        status: 'SUCCESS',
    });
    const addToHistorySpied = jest.fn(() => mockResolve);

    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistorySpied }
            onRequestHide={ noop }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            show={ true }
        />,
        { context: contextMock },
    );

    wrapper.setState({
        pending: false,
    });
    wrapper.find(SearchLineSuggestMobileItem).at(0).simulate('click', suggestDataMock.suggests[0]);

    return mockResolve.then(() => {
        expect(addToHistorySpied).toHaveBeenCalledTimes(1);
        expect(addToHistorySpied.mock.calls).toMatchSnapshot();
    });
});

it('должен запросить историю поиска, когда саджест становится видимым, а текст запроса пустой', () => {
    const getHistorySpied = jest.fn(getHistory);

    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistory }
            onRequestHide={ noop }
            getHistory={ getHistorySpied }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            show={ false }
        />,
        { context: contextMock },
    );

    getHistorySpied.mockClear();
    wrapper.setProps({
        show: true,
    });

    expect(getHistorySpied).toHaveBeenCalledTimes(1);
});

it('не должен запрашивать историю поиска, когда саджест становится видимым, а текст запроса не пустой', () => {
    const getHistorySpied = jest.fn(getHistory);

    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistory }
            onRequestHide={ noop }
            getHistory={ getHistorySpied }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            show={ false }
        />,
        { context: contextMock },
    );
    getHistorySpied.mockClear();
    wrapper.setState({
        query: 'A4',
    });
    wrapper.setProps({
        show: true,
    });

    expect(getHistorySpied).toHaveBeenCalledTimes(0);
});

it('не должен запрашивать историю поиска, когда саджест был и остался видимым, а текст запроса пустой', () => {
    const getHistorySpied = jest.fn(getHistory);

    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistory }
            onRequestHide={ noop }
            getHistory={ getHistorySpied }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            show={ true }
        />,
        { context: contextMock },
    );
    getHistorySpied.mockClear();
    wrapper.setProps({
        show: true,
    });

    expect(getHistorySpied).toHaveBeenCalledTimes(0);
});

it('должен заблокировать скролл, когда саджест стал видимым', () => {
    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistory }
            onRequestHide={ noop }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            show={ false }
        />,
        { context: contextMock },
    );

    wrapper.setProps({
        show: true,
    });

    expect(blockScroll).toHaveBeenCalledTimes(1);
    expect(unblockScroll).toHaveBeenCalledTimes(0);
});

it('должен разблокировать скролл, когда саджест стал невидимым', () => {
    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistory }
            onRequestHide={ noop }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            show={ true }
        />,
        { context: contextMock },
    );

    wrapper.setProps({
        show: false,
    });

    expect(blockScroll).toHaveBeenCalledTimes(0);
    expect(unblockScroll).toHaveBeenCalledTimes(1);
});

it('если запрос за историей зафейлится, должен показать популярные запросы', () => {
    const promiseRejected = Promise.reject('ooops');
    const getHistoryFailed = jest.fn(() => promiseRejected);

    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistory }
            onRequestHide={ noop }
            getHistory={ getHistoryFailed }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            show={ true }
        />,
        { context: contextMock },
    );

    wrapper.setProps({
        show: true,
    });

    return promiseRejected
        .catch(noop)
        .finally(() => {
            wrapper.setState({
                pending: false,
            });
            const suggestItems = wrapper.find(SearchLineSuggestMobileItem);
            const popularItems = suggestItems.filterWhere((item) => item.prop('type') === 'POPULAR');

            expect(suggestItems.length === popularItems.length).toBe(true);
        });
});

it('при нажатии Enter: если есть результаты поиска, должен добавить первый результат в историю', () => {
    const mockResolve = Promise.resolve(suggestDataMock as unknown as TSearchLineSuggest);

    const addToHistorySpied = jest.fn(addToHistory);
    const getSuggestDataSpied = jest.fn(() => mockResolve);

    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistorySpied }
            onRequestHide={ noop }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestDataSpied }
            show={ true }
        />,
        { context: contextMock },
    );
    const textInput = wrapper.find('TopSearchField').dive().find(TextInput);

    textInput.simulate('change', 'A4');

    return mockResolve.finally(() => {
        // Это костыль, из-за того, что этот блок выполняется первее резолва _requestSuggestData
        // и данные успевают прийти в стэйт
        wrapper.setState({
            suggestData: suggestDataMock,
        });

        textInput.simulate('keyPress', { key: 'Enter' });

        expect(addToHistorySpied).toHaveBeenCalledTimes(1);
        expect(addToHistorySpied.mock.calls).toMatchSnapshot();
    });
});

it('при нажатии Enter: если есть результаты поиска, должен перейти по урлу первого результата', () => {
    const mockResolve = Promise.resolve({
        status: 'SUCCESS',
    });
    const addToHistorySpied = jest.fn(() => mockResolve);

    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistorySpied }
            onRequestHide={ noop }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            show={ true }
        />,
        { context: contextMock },
    );
    const textInput = wrapper.find('TopSearchField').dive().find(TextInput);

    textInput.simulate('change', 'A4');
    // Это костыль, из-за того, что mockResolve.finally выполняется первее резолва _requestSuggestData
    // и данные успевают прийти в стэйт
    wrapper.setState({
        suggestData: suggestDataMock,
    });

    textInput.simulate('keyPress', { key: 'Enter' });

    return mockResolve.finally(() => {
        expect(window.location.href).toEqual(suggestDataMock.suggests[0].url);
    });
});

it('при нажатии Enter: если текст запроса пустой, не добавлять в историю', () => {
    const addToHistorySpied = jest.fn(addToHistory);

    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistorySpied }
            onRequestHide={ noop }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestData }
            show={ true }
        />,
        { context: contextMock },
    );

    wrapper.setState({
        pending: false,
        query: '',
    });
    const textInput = wrapper.find('TopSearchField').dive().find(TextInput);
    textInput.simulate('keyPress', { key: 'Enter' });
    expect(addToHistorySpied).toHaveBeenCalledTimes(0);
});

it('при нажатии Enter: если нет результатов поиска, должен запросить результаты и добавить текст в историю, если есть результаты', () => {
    const addToHistorySpied = jest.fn(addToHistory);

    const mockResolve = Promise.resolve(suggestDataMock as unknown as TSearchLineSuggest);
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const getSuggestDataSpied = jest.fn((_arg) => mockResolve);

    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistorySpied }
            onRequestHide={ noop }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestDataSpied }
            show={ true }
        />,
        { context: contextMock },
    );

    wrapper.setState({
        pending: false,
        query: 'A4',
        suggestData: null,
    });
    const textInput = wrapper.find('TopSearchField').dive().find(TextInput);
    textInput.simulate('keyPress', { key: 'Enter' });

    expect(addToHistorySpied).toHaveBeenCalledTimes(0);

    return mockResolve.finally(() => {
        expect(getSuggestDataSpied).toHaveBeenCalledTimes(1);
        expect(getSuggestDataSpied).toHaveBeenCalledWith('A4');

        expect(addToHistorySpied).toHaveBeenCalledTimes(1);
        expect(addToHistorySpied).toHaveBeenLastCalledWith({
            category: 'CARS',
            is_plain_text: true,
            params: {},
            query: 'A4',
        });
    });
});

it('при нажатии Enter: если нет результатов поиска, должен запросить результаты и не добавлять текст в историю, если нет результатов', () => {
    const addToHistorySpied = jest.fn(addToHistory);

    const mockResolve = Promise.resolve({
        query: 'A4',
        query_id: '10',
        suggests: [],
    } as TSearchLineSuggest);
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const getSuggestDataSpied = jest.fn((_arg) => mockResolve);

    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistorySpied }
            onRequestHide={ noop }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestDataSpied }
            show={ true }
        />,
        { context: contextMock },
    );

    wrapper.setState({
        pending: false,
        query: 'A4',
        suggestData: null,
    });
    const textInput = wrapper.find('TopSearchField').dive().find(TextInput);
    textInput.simulate('keyPress', { key: 'Enter' });

    expect(addToHistorySpied).toHaveBeenCalledTimes(0);

    return mockResolve.finally(() => {
        expect(getSuggestDataSpied).toHaveBeenCalledTimes(1);
        expect(getSuggestDataSpied).toHaveBeenCalledWith('A4');

        expect(addToHistorySpied).toHaveBeenCalledTimes(0);
    });
});

it('при нажатии Enter: если нет результатов поиска, запросить результаты и перейти по урлу первого результата', () => {
    const mockResolve = Promise.resolve(suggestDataMock as unknown as TSearchLineSuggest);

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const getSuggestDataSpied = jest.fn((_arg) => mockResolve);

    const wrapper = shallow(
        <SearchLineSuggestMobile
            addToHistory={ addToHistory }
            onRequestHide={ noop }
            getHistory={ getHistory }
            getPopularItems={ getPopularItems }
            getSuggestData={ getSuggestDataSpied }
            show={ true }
        />,
        { context: contextMock },
    );

    wrapper.setState({
        pending: false,
        query: 'A4',
        suggestData: null,
    });
    const textInput = wrapper.find('TopSearchField').dive().find(TextInput);
    textInput.simulate('keyPress', { key: 'Enter' });

    expect(getSuggestDataSpied).toHaveBeenCalledTimes(1);
    expect(getSuggestDataSpied).toHaveBeenCalledWith('A4');

    return mockResolve.finally(() => {
        expect(window.location.href).toEqual(suggestDataMock.suggests[0].url);
    });
});

describe('Эксперимент с ПроАвто', () => {
    const localContextMock = {
        ...contextMock,
        hasExperiment: (exp: string) => exp === 'AUTORUFRONT-18481_shapka_proauto',
    };

    it('Если введён вин, вместо результатов должен отрисовать ссылку на ПроАвто', () => {
        const wrapper = shallow(
            <SearchLineSuggestMobile
                addToHistory={ addToHistory }
                onRequestHide={ noop }
                getHistory={ getHistory }
                getPopularItems={ getPopularItems }
                getSuggestData={ getSuggestData }
                show={ true }
            />,
            { context: localContextMock },
        );

        wrapper.setState({
            pending: false,
            query: 'JHMGD18508S219366',
            suggestData: null,
        });

        expect(wrapper.find('Link').prop('url')).toBe('link/proauto-report/?from=searchline_suggest&history_entity_id=JHMGD18508S219366');
    });

    it('Если введён госномер, вместо результатов должен отрисовать ссылку на ПроАвто', () => {
        const wrapper = shallow(
            <SearchLineSuggestMobile
                addToHistory={ addToHistory }
                onRequestHide={ noop }
                getHistory={ getHistory }
                getPopularItems={ getPopularItems }
                getSuggestData={ getSuggestData }
                show={ true }
            />,
            { context: localContextMock },
        );

        wrapper.setState({
            pending: false,
            query: 'A001AA05',
            suggestData: null,
        });

        expect(wrapper.find('Link').prop('url')).toBe('link/proauto-report/?from=searchline_suggest&history_entity_id=A001AA05');
    });

    it('при нажатии Enter должен перейти на страницу ПроАвто, если введён вин', () => {
        const mockResolve = Promise.resolve(suggestDataMock as unknown as TSearchLineSuggest);

        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const getSuggestDataSpied = jest.fn((_arg) => mockResolve);

        const wrapper = shallow(
            <SearchLineSuggestMobile
                addToHistory={ addToHistory }
                onRequestHide={ noop }
                getHistory={ getHistory }
                getPopularItems={ getPopularItems }
                getSuggestData={ getSuggestDataSpied }
                show={ true }
            />,
            { context: localContextMock },
        );

        wrapper.setState({
            pending: false,
            query: 'JHMGD18508S219366',
            suggestData: null,
        });
        const textInput = wrapper.find('TopSearchField').dive().find(TextInput);
        textInput.simulate('keyPress', { key: 'Enter' });

        expect(getSuggestDataSpied).toHaveBeenCalledTimes(0);

        return mockResolve.finally(() => {
            expect(window.location.href).toBe('link/proauto-report/?from=searchline_suggest&history_entity_id=JHMGD18508S219366');
        });
    });

    it('при нажатии Enter должен перейти на страницу ПроАвто, если введён госномер', () => {
        const mockResolve = Promise.resolve(suggestDataMock as unknown as TSearchLineSuggest);

        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const getSuggestDataSpied = jest.fn((_arg) => mockResolve);

        const wrapper = shallow(
            <SearchLineSuggestMobile
                addToHistory={ addToHistory }
                onRequestHide={ noop }
                getHistory={ getHistory }
                getPopularItems={ getPopularItems }
                getSuggestData={ getSuggestDataSpied }
                show={ true }
            />,
            { context: localContextMock },
        );

        wrapper.setState({
            pending: false,
            query: 'A001AA05',
            suggestData: null,
        });
        const textInput = wrapper.find('TopSearchField').dive().find(TextInput);
        textInput.simulate('keyPress', { key: 'Enter' });

        expect(getSuggestDataSpied).toHaveBeenCalledTimes(0);

        return mockResolve.finally(() => {
            expect(window.location.href).toBe('link/proauto-report/?from=searchline_suggest&history_entity_id=A001AA05');
        });
    });

    it('при нажатии Enter: если нет результатов поиска, должен запросить результаты и добавить текст в историю, если есть результаты', () => {
        const addToHistorySpied = jest.fn(addToHistory);

        const mockResolve = Promise.resolve(suggestDataMock as unknown as TSearchLineSuggest);
        // eslint-disable-next-line @typescript-eslint/no-unused-vars
        const getSuggestDataSpied = jest.fn((_arg) => mockResolve);

        const wrapper = shallow(
            <SearchLineSuggestMobile
                addToHistory={ addToHistorySpied }
                onRequestHide={ noop }
                getHistory={ getHistory }
                getPopularItems={ getPopularItems }
                getSuggestData={ getSuggestDataSpied }
                show={ true }
            />,
            { context: localContextMock },
        );

        wrapper.setState({
            pending: false,
            query: 'A4',
            suggestData: null,
        });
        const textInput = wrapper.find('TopSearchField').dive().find(TextInput);
        textInput.simulate('keyPress', { key: 'Enter' });

        expect(addToHistorySpied).toHaveBeenCalledTimes(0);

        return mockResolve.finally(() => {
            expect(getSuggestDataSpied).toHaveBeenCalledTimes(1);
            expect(getSuggestDataSpied).toHaveBeenCalledWith('A4');

            expect(addToHistorySpied).toHaveBeenCalledTimes(1);
            expect(addToHistorySpied).toHaveBeenLastCalledWith({
                category: 'CARS',
                is_plain_text: true,
                params: {},
                query: 'A4',
            });
        });
    });
});
