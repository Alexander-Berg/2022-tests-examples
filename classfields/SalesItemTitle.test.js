const React = require('react');
const { shallow } = require('enzyme');
const { shallowToJson } = require('enzyme-to-json');

const contextMock = require('autoru-frontend/mocks/contextMock').default;

const SalesItemTitle = require('./SalesItemTitle');

let offer;
beforeEach(() => {
    offer = {
        category: 'cars',
        status: 'ACTIVE',
        vehicle_info: {
            mark_info: { name: 'Chery' },
            model_info: { name: 'Tiggo (T11)' },
            super_gen: { id: 1, name: 'I' },
        },
    };
});

it('должен правильно отрендерить тайтл активного объявления', () => {
    offer.status = 'ACTIVE';
    offer.actions = { edit: true };

    const wrapper = shallow(<SalesItemTitle offer={ offer }/>, { context: contextMock });
    wrapper.find('.SalesItemTitle').simulate('click');
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно отрендерить тайтл активного объявления мото/комтс', () => {
    offer.status = 'ACTIVE';
    offer.actions = { edit: true };
    delete offer.vehicle_info.super_gen;

    const wrapper = shallow(<SalesItemTitle offer={ offer }/>, { context: contextMock });
    wrapper.find('.SalesItemTitle').simulate('click');
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно отрендерить тайтл неактивного объявления', () => {
    offer.status = 'INACTIVE';
    offer.actions = { edit: true };

    const wrapper = shallow(<SalesItemTitle offer={ offer }/>, { context: contextMock });
    wrapper.find('.SalesItemTitle').simulate('click');
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно отрендерить тайтл забаненного объявления без возможности редактирования', () => {
    offer.actions = { edit: false };
    offer.status = 'BANNED';

    const wrapper = shallow(<SalesItemTitle offer={ offer }/>, { context: contextMock });
    wrapper.find('.SalesItemTitle').simulate('click');
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно отрендерить тайтл забаненного объявления с возможностью редактирования', () => {
    offer.actions = { edit: true };
    offer.status = 'BANNED';

    const wrapper = shallow(<SalesItemTitle offer={ offer }/>, { context: contextMock });
    wrapper.find('.SalesItemTitle').simulate('click');
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно отрендерить тайтл протухшего объявления', () => {
    offer.status = 'EXPIRED';
    offer.actions = { edit: true };

    const wrapper = shallow(<SalesItemTitle offer={ offer }/>, { context: contextMock });
    wrapper.find('.SalesItemTitle').simulate('click');
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});

it('должен правильно отрендерить тайтл драфта объявления', () => {
    offer.status = 'DRAFT';
    offer.actions = { edit: true };

    const wrapper = shallow(<SalesItemTitle offer={ offer }/>, { context: contextMock });
    expect(shallowToJson(wrapper)).toMatchSnapshot();
});
