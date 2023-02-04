/* eslint-disable-next-line import-helpers/order-imports */
import mockLocalStatData from 'auto-core/react/lib/localStatData.mock';
jest.mock('auto-core/react/lib/localStatData', () => mockLocalStatData);
jest.mock('auto-core/appConfigClient', () => ({ pageRequestId: 'pageRequestId' }));

import React from 'react';
import { render, fireEvent } from '@testing-library/react';
import { Provider } from 'react-redux';

import mockStore from 'autoru-frontend/mocks/mockStore';

import '@testing-library/jest-dom';

import TabAndSearchIdDetector from './TabAndSearchIdDetector';

const referrer = 'https://auto.ru/';
const hidden = false;
const referrerGetter = jest.fn(() => referrer);
const hiddenGetter = jest.fn(() => hidden);

const store = mockStore({});

Object.defineProperty(document, 'referrer', {
    get: referrerGetter,
    set: jest.fn(),
});
Object.defineProperty(document, 'hidden', {
    get: hiddenGetter,
    set: jest.fn(),
});

beforeEach(() => {
    // в основном все действия делаются для только что открытого таба (то есть без id)
    mockLocalStatData.getTabId.mockImplementation(() => null);
    mockLocalStatData.getActiveTabId.mockImplementation(() => 'activeTabId');
    mockLocalStatData.getParentTabId.mockImplementation(() => 'parentTabId');
    mockLocalStatData.getSearchId.mockImplementation(() => ({
        searchId: 'searchId',
        parentSearchId: undefined,
    }));
    mockLocalStatData.getLastSearchId.mockImplementation(() => ({
        searchId: 'lastSearchId',
        parentSearchId: undefined,
    }));
});

it('устанавливает tabId, если его не было', () => {
    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setTabId).toHaveBeenCalledWith('pageRequestId');
});

it('устанавливает tabId, если его не было и пришли не с автору', () => {
    referrerGetter.mockImplementationOnce(() => '');

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setTabId).toHaveBeenCalledWith('pageRequestId');
});

it('НЕ устанавливает tabId, если он был', () => {
    mockLocalStatData.getTabId.mockImplementation(() => 'requestId');

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setTabId).not.toHaveBeenCalled();
});

it('устанавливает родительский таб, если еще не был установлен', () => {
    mockLocalStatData.getParentTabId.mockImplementation(() => null);

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setParentTabId).toHaveBeenCalledWith('activeTabId');
});

it('НЕ устанавливает родительский таб, если был установлен', () => {
    mockLocalStatData.getParentTabId.mockImplementation(() => 'parentTabId');

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setParentTabId).not.toHaveBeenCalled();
});

it('НЕ устанавливает ничего, если пришли не с авто', () => {
    referrerGetter.mockImplementationOnce(() => '');
    mockLocalStatData.getParentTabId.mockImplementation(() => null);

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setParentTabId).not.toHaveBeenCalled();
    expect(mockLocalStatData.setSearchId).not.toHaveBeenCalled();
    expect(mockLocalStatData.transferStoriesFromTabToTab).not.toHaveBeenCalled();
});

it('НЕ устанавливает ничего, если не первый заход', () => {
    mockLocalStatData.getTabId.mockImplementation(() => 'tabId');

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setParentTabId).not.toHaveBeenCalled();
    expect(mockLocalStatData.setSearchId).not.toHaveBeenCalled();
    expect(mockLocalStatData.transferStoriesFromTabToTab).not.toHaveBeenCalled();
});

it('НЕ устанавливает ничего, если нет активного таба', () => {
    mockLocalStatData.getActiveTabId.mockImplementation(() => null);

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setParentTabId).not.toHaveBeenCalled();
    expect(mockLocalStatData.setSearchId).not.toHaveBeenCalled();
    expect(mockLocalStatData.transferStoriesFromTabToTab).not.toHaveBeenCalled();
});

it('сохраняет id поиска из парента, если он был и текущего нет', () => {
    mockLocalStatData.getSearchId.mockImplementation(() => ({
        searchId: '',
        parentSearchId: undefined,
    }));

    mockLocalStatData.getLastSearchId.mockImplementation(() => ({
        searchId: 'lastSearchId',
        parentSearchId: 'parentSearchId',
    }));

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setSearchId).toHaveBeenCalledWith('lastSearchId', 'parentSearchId');
});

it('НЕ сохраняет id поиска из парента, если он был и есть текущий', () => {
    mockLocalStatData.getSearchId.mockImplementation(() => ({
        searchId: 'searchId',
        parentSearchId: undefined,
    }));

    mockLocalStatData.getLastSearchId.mockImplementation(() => ({
        searchId: 'lastSearchId',
        parentSearchId: 'parentSearchId',
    }));

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setSearchId).not.toHaveBeenCalled();
    expect(mockLocalStatData.transferStoriesFromTabToTab).not.toHaveBeenCalled();
});

it('НЕ сохраняет id поиска из парента, если нет никаких id', () => {
    mockLocalStatData.getSearchId.mockImplementation(() => ({
        searchId: '',
        parentSearchId: undefined,
    }));

    mockLocalStatData.getLastSearchId.mockImplementation(() => ({
        searchId: '',
        parentSearchId: undefined,
    }));

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setSearchId).not.toHaveBeenCalled();
    expect(mockLocalStatData.transferStoriesFromTabToTab).not.toHaveBeenCalled();
});

it('если есть парент, запускает транфер событий из парента', () => {
    mockLocalStatData.getSearchId.mockImplementation(() => ({
        searchId: '',
        parentSearchId: undefined,
    }));

    mockLocalStatData.getLastSearchId.mockImplementation(() => ({
        searchId: 'lastSearchId',
        parentSearchId: 'parentSearchId',
    }));

    mockLocalStatData.getParentTabId.mockImplementation(() => 'parentTab');

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.transferStoriesFromTabToTab).toHaveBeenCalledWith('pageRequestId', 'parentTab', referrer, 'lastSearchId');
});

it('устанавливает активный таб, если страница не скрыта и текущий таб не активный', () => {
    hiddenGetter.mockImplementationOnce(() => false);
    mockLocalStatData.getActiveTabId.mockImplementation(() => 'activeTab');
    mockLocalStatData.getTabId.mockImplementation(() => 'tabId');

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setActiveTabId).toHaveBeenCalledWith('tabId');
});

it('НЕ устанавливает активный таб, если страница скрыта и текущий таб не активный', () => {
    hiddenGetter.mockImplementationOnce(() => true);
    mockLocalStatData.getActiveTabId.mockImplementation(() => 'activeTab');
    mockLocalStatData.getTabId.mockImplementation(() => 'tabId');

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setActiveTabId).not.toHaveBeenCalled();
});

it('НЕ устанавливает активный таб, если страница не скрыта и текущий таб активный', () => {
    hiddenGetter.mockImplementationOnce(() => false);
    mockLocalStatData.getActiveTabId.mockImplementation(() => 'activeTab');
    mockLocalStatData.getTabId.mockImplementation(() => 'activeTab');

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    expect(mockLocalStatData.setActiveTabId).not.toHaveBeenCalled();
});

it('устанавливает активный таб и последний поиск, для на показе таба, если активный таб не текущий', () => {
    referrerGetter.mockImplementation(() => '');
    hiddenGetter.mockImplementation(() => false);

    mockLocalStatData.getActiveTabId.mockImplementation(() => 'activeTab');
    mockLocalStatData.getTabId.mockImplementation(() => 'tabId');
    mockLocalStatData.getSearchId.mockImplementation(() => ({
        searchId: 'searchId',
        parentSearchId: 'parentSearchId',
    }));

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    fireEvent.focus(document);

    expect(mockLocalStatData.setActiveTabId).toHaveBeenCalledWith('tabId');
    expect(mockLocalStatData.setLastSearchId).toHaveBeenCalledWith('searchId', 'parentSearchId');
});

it('НЕ устанавливает активный таб и последний поиск, для на скрытии таба, если активный таб не текущий', () => {
    referrerGetter.mockImplementation(() => '');
    hiddenGetter.mockImplementation(() => true);

    mockLocalStatData.getActiveTabId.mockImplementation(() => 'activeTab');
    mockLocalStatData.getTabId.mockImplementation(() => 'tabId');
    mockLocalStatData.getSearchId.mockImplementation(() => ({
        searchId: 'searchId',
        parentSearchId: 'parentSearchId',
    }));

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    fireEvent.focus(document);

    expect(mockLocalStatData.setActiveTabId).not.toHaveBeenCalled();
    expect(mockLocalStatData.setLastSearchId).not.toHaveBeenCalled();
});

it('НЕ устанавливает активный таб и последний поиск, для на показе таба, если активный таб текущий', () => {
    referrerGetter.mockImplementation(() => '');
    hiddenGetter.mockImplementation(() => true);

    mockLocalStatData.getActiveTabId.mockImplementation(() => 'activeTab');
    mockLocalStatData.getTabId.mockImplementation(() => 'activeTab');
    mockLocalStatData.getSearchId.mockImplementation(() => ({
        searchId: 'searchId',
        parentSearchId: 'parentSearchId',
    }));

    render(
        <Provider store={ store }>
            <TabAndSearchIdDetector>children</TabAndSearchIdDetector>
        </Provider>,
    );

    fireEvent.focus(document);

    expect(mockLocalStatData.setActiveTabId).not.toHaveBeenCalled();
    expect(mockLocalStatData.setLastSearchId).not.toHaveBeenCalled();
});
