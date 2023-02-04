/**
 * @jest-environment @vertis/allure-report/build/circus/allure-jsdom-environment
 */

jest.mock('auto-core/react/lib/gateApi', () => {
    return {
        getResource: jest.fn(),
    };
});

jest.mock('auto-core/react/dataDomain/drafts/actions/deleteDraft', () => ({
    'default': jest.fn(),
}));

const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const getResource = require('auto-core/react/lib/gateApi').getResource;
const mockStore = require('autoru-frontend/mocks/mockStore').default;

const contextMock = require('autoru-frontend/mocks/contextMock').default;
const deleteDraftMock = require('auto-core/react/dataDomain/drafts/actions/deleteDraft').default;

const SalesItemFoldedMenu = require('./SalesItemFoldedMenu');

let store;

const offer = {
    category: 'CARS',
    link: '/offer.url',
    id: '123456',
    hash: 'aaa',
};

beforeEach(() => {
    jest.spyOn(global, 'confirm').mockImplementation(() => { });
});

afterEach(() => {
    jest.restoreAllMocks();
});

it('должен правильно отрендерить меню для активного оффера cars', () => {
    store = mockStore();
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemFoldedMenu offer={{
        ...offer,
        actions: { edit: true },
        category: 'cars',
    }}/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно отрендерить меню для неактивного оффера', () => {
    store = mockStore();
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemFoldedMenu offer={{
        ...offer,
        actions: { archive: true, edit: true },
    }}/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно отрендерить меню для протухшего оффера', () => {
    store = mockStore();
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemFoldedMenu offer={{
        ...offer,
        actions: { archive: true, edit: true },
        status: 'EXPIRED',
    }}/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно отрендерить меню для забаненного оффера', () => {
    store = mockStore();
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemFoldedMenu offer={{
        ...offer,
        actions: { edit: true },
        status: 'BANNED',
    }}/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно отрендерить меню для забаненного юзера', () => {
    store = mockStore();
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemFoldedMenu userIsBanned={ true } offer={{
        ...offer,
        actions: { edit: true },
        status: 'BANNED',
    }}/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен открывать чат по ссылке в забаненном оффере', () => {
    window.vertis_chat = {
        open_tech_support_chat: jest.fn(),
    };
    store = mockStore();
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemFoldedMenu userIsBanned={ true } offer={{
        ...offer,
        actions: { edit: true },
        status: 'BANNED',
    }}/>, { context: context }).dive();
    wrapper.find('Link[children="Написать в поддержку"]').simulate('click');
    expect(window.vertis_chat.open_tech_support_chat).toHaveBeenCalled();
});

it('должен правильно отрендерить меню для активного оффера moto', () => {
    store = mockStore();
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemFoldedMenu offer={{
        ...offer,
        actions: { edit: true },
        category: 'moto',
    }}/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно обработать клик на удаление оффера', () => {
    window.confirm = jest.fn(() => true);
    const mockResponse = Promise.resolve({ status: 'SUCCESS' });
    getResource.mockImplementation(() => mockResponse);
    const expectedAtions = [
        { payload: { offerID: '123456-aaa', isLoading: true }, type: 'SALES_SET_LOADING_STATE' },
        { payload: { message: 'Объявление удалено', view: 'success' }, type: 'NOTIFIER_SHOW_MESSAGE' },
        { payload: { category: 'testCategory', offerID: '123456-aaa' }, type: 'SALES_DELETE_OFFER' },
    ];
    store = mockStore();
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemFoldedMenu offer={{
        ...offer,
        actions: { archive: true, edit: true },
        category: 'testCategory',
        saleId: '123456-aaa',
    }}/>, { context: context }).dive();

    wrapper.find('Link[children="Удалить"]').simulate('click');
    expect(window.confirm).toHaveBeenCalled();
    return mockResponse.then(() => {
        expect(store.getActions()).toEqual(expectedAtions);
    });
});

it('не должен удалять оффер, еслил юзер не подтвердил', () => {
    window.confirm = jest.fn(() => false);
    store = mockStore();
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemFoldedMenu offer={{
        ...offer,
        actions: { archive: true, edit: true },
        saleId: '123456-aaa',
    }}/>, { context: context }).dive();
    wrapper.find('Link[children="Удалить"]').simulate('click');
    expect(window.confirm).toHaveBeenCalled();
    expect(store.getActions()).toEqual([]);
});

it('должен правильно отрендерить меню для забаненного оффера cars, который нельзя редактировать', () => {
    store = mockStore();
    const context = {
        ...contextMock,
        store,
    };
    const wrapper = shallow(<SalesItemFoldedMenu offer={{
        ...offer,
        actions: { edit: false },
        status: 'BANNED',
        category: 'cars',
    }}/>, { context: context }).dive();
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

describe('драфт объявления', () => {
    const offerMock = {
        ...offer,
        actions: { edit: true },
        status: 'DRAFT',
        section: 'used',
        category: 'moto',
    };
    store = mockStore();
    const context = {
        ...contextMock,
        store,
    };

    it('правильно формирует ссылки', () => {
        const wrapper = shallow(<SalesItemFoldedMenu offer={ offerMock }/>, { context: context }).dive();
        expect(shallowToJson(wrapper)).toMatchSnapshot();
    });

    it('без подтверждения не будет удалять драфт', () => {
        global.confirm.mockImplementationOnce(() => false);

        const wrapper = shallow(<SalesItemFoldedMenu offer={ offerMock }/>, { context: context }).dive();
        const deleteLink = wrapper.find('.SalesItemFoldedMenu_link').at(1);
        deleteLink.simulate('click');

        expect(global.confirm).toHaveBeenCalledTimes(1);
        expect(deleteDraftMock).toHaveBeenCalledTimes(0);
    });

    it('при клике на кнопку удалить отправит запрос', () => {
        deleteDraftMock.mockImplementationOnce(() => () => Promise.resolve());
        global.confirm.mockImplementationOnce(() => true);

        const wrapper = shallow(<SalesItemFoldedMenu offer={ offerMock }/>, { context: context }).dive();
        const deleteLink = wrapper.find('.SalesItemFoldedMenu_link').at(1);
        deleteLink.simulate('click');

        expect(global.confirm).toHaveBeenCalledTimes(1);
        expect(deleteDraftMock).toHaveBeenCalledTimes(1);
        expect(deleteDraftMock).toHaveBeenCalledWith({ parent_category: 'moto', draft_id: '123456-aaa' });
    });

    it('после удачного запроса на удаление отправит метрику', () => {
        const draftDeletePromise = Promise.resolve();
        deleteDraftMock.mockImplementationOnce(() => () => draftDeletePromise);
        global.confirm.mockImplementationOnce(() => true);

        const wrapper = shallow(<SalesItemFoldedMenu offer={ offerMock }/>, { context: context }).dive();
        const deleteLink = wrapper.find('.SalesItemFoldedMenu_link').at(1);
        deleteLink.simulate('click');

        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
        return draftDeletePromise
            .then(() => {
                expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(1);
                expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'draft_in_lk', 'delete' ]);
            });
    });

    it('после неудачного запроса на удаление не отправит метрику', () => {
        const draftDeletePromise = Promise.reject();
        deleteDraftMock.mockImplementationOnce(() => () => draftDeletePromise);
        global.confirm.mockImplementationOnce(() => true);

        const wrapper = shallow(<SalesItemFoldedMenu offer={ offerMock }/>, { context: context }).dive();
        const deleteLink = wrapper.find('.SalesItemFoldedMenu_link').at(1);
        deleteLink.simulate('click');

        return draftDeletePromise
            .catch(() => {})
            .then(() => {
                expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledTimes(0);
            });
    });
});
