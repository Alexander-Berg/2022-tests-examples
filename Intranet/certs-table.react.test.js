import {mount} from 'enzyme';
import React from 'react';
import CertsTable from 'b:certs-table';

const genFirstItem = (id) => ({
    common_name: 'asd.asd.com',
    added: '2018-08-08T08:13:35+03:00',
    end_date: '2021-08-06T08:03:35+03:00',
    issued: '2018-08-07T08:13:35+03:00',
    serial_number: '3EDC1B0A00010024EFD1',
    requester: {
        username: 'nope',
        first_name: {ru: 'Тест', en: 'Test'},
        last_name: {ru: 'Тестовый', en: 'Testovii'},
        is_active: true,
        in_hiring: false
    },
    id,
    ca_name: 'InternalTestCA',
    status_human: {ru: 'Выдан', en: 'Issued'},
    type_human: {ru: 'Для web-сервера', en: 'For web servers'}
});

const genSecondItem = (id) => ({
    common_name: 'ads.ad.ru',
    added: '2018-08-07T08:13:03+03:00',
    end_date: '2021-08-06T08:03:03+03:00',
    issued: '2018-08-07T08:13:04+03:00',
    serial_number: '3EDB9F2300010024EFD0',
    requester: {
        username: 'yeah',
        first_name: {ru: 'Тест1', en: 'Test1'},
        last_name: {ru: 'Тестовый1', en: 'Testovii1'},
        is_active: false,
        in_hiring: true
    },
    id,
    ca_name: 'InternalTestCA',
    status_human: {ru: 'Выдан', en: 'Issued'},
    type_human: {ru: 'Для web-сервера', en: 'For web servers'}
});

const PAGER_NOT_ACTIVE_BUTTON = '.ta-pager__button_role_page[aria-pressed="false"]';
const FIRST_ROW_ITEM = '.certs-table__tr[data-id=1]';

describe('Certs Table', () => {

    let singlePageData;
    let multiPageData;
    let fetchingData;
    let noCertsData;
    let onPagerChange;
    let onTrClick;
    let getParamLast;

    beforeEach(() => {
        onPagerChange = jest.fn();
        onTrClick = jest.fn();
        getParamLast = jest.fn();

        getParamLast.mockReturnValue(null);

        fetchingData = {
            certs: {
                items: [],
                count: 0,
                page: 1,
                perPage: 10
            },
            isFetching: true,
            onPagerChange,
            onTrClick
        };

        noCertsData = {
            ...fetchingData,
            isFetching: false
        };

        singlePageData = {
            ...noCertsData,
            certs: {
                items: [
                    genFirstItem(1),
                    genSecondItem(2)
                ],
                count: 2,
                page: 1,
                perPage: 10
            },
            numberOfPages: 1
        };

        multiPageData = {
            ...singlePageData,
            certs: {
                items: Array.from({length: 10}) // eslint-disable-next-line no-extra-parens
                    .map((value, ind) => (ind % 2 ? genFirstItem(ind) : genSecondItem(ind))),
                count: 11,
                page: 1,
                perPage: 10
            },
            numberOfPages: 2
        };
    });

    describe('renders correctly', () => {
        it('when multi page', () => {
            const wrapper = mount(
                <CertsTable {...multiPageData} />
            );

            expect(wrapper).toMatchSnapshot();
            wrapper.unmount();
        });

        it('when single page', () => {
            const wrapper = mount(
                <CertsTable {...singlePageData} />
            );

            expect(wrapper).toMatchSnapshot();
            wrapper.unmount();
        });

        it('when empty', () => {
            const wrapper = mount(
                <CertsTable {...noCertsData} />
            );

            expect(wrapper).toMatchSnapshot();
            wrapper.unmount();
        });

        it('when second page is requested', () => {
            singlePageData.certs.count = 12;
            singlePageData.page = 2;
            const wrapper = mount(
                <CertsTable {...singlePageData} />
            );

            expect(wrapper).toMatchSnapshot();
            wrapper.unmount();
        });

        it('when fetching data', () => {
            const wrapper = mount(
                <CertsTable {...fetchingData} />
            );

            expect(wrapper).toMatchSnapshot();
            wrapper.unmount();
        });

        it('when date is invalid', () => {
            singlePageData.certs.items[0].added = 'INVALID DATE';
            const wrapper = mount(
                <CertsTable {...singlePageData} />
            );

            expect(wrapper).toMatchSnapshot();
            wrapper.unmount();
        });

        it('when path has valid cert id', () => {
            getParamLast = jest.fn();
            getParamLast.mockReturnValue('1');
            const wrapper = mount(
                <CertsTable {...singlePageData} />
            );

            expect(wrapper).toMatchSnapshot();
            wrapper.unmount();
        });
    });

    it('calls onPagerChange when user clicks on page Number', () => {
        const wrapper = mount(
            <CertsTable {...multiPageData} />
        );

        wrapper.find(PAGER_NOT_ACTIVE_BUTTON).simulate('click');

        expect(onPagerChange.mock.calls.length).toBe(1);

        expect(onPagerChange.mock.calls[0][0]).toMatchObject({page: 2});

        wrapper.unmount();
    });

    it('calls on Tr click when user clicks row', () => {
        const wrapper = mount(
            <CertsTable {...multiPageData} />
        );

        wrapper.find(FIRST_ROW_ITEM).simulate('click');

        expect(onTrClick.mock.calls.length).toBe(1);

        expect(onTrClick.mock.calls[0][0].target.dataset.id).toBe('1');

        wrapper.unmount();
    });
});
