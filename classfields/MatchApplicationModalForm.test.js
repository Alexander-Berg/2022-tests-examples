const _ = require('lodash');
const React = require('react');
const { shallow } = require('enzyme');

const MatchApplicationModalForm = require('./MatchApplicationModalForm');
const MatchApplicationLazyAuthDesktop = require('../MatchApplicationLazyAuthDesktop');

const PROPS = {
    marks: [
        { id: 'AUDI', name: 'AUDI' },
        { id: 'BMW', name: 'BMW' },
    ],
    models: [
        { id: 'A5', name: 'A5' },
        { id: 'Q5', name: 'Q5' },
    ],
    defaultMarkCode: 'AUDI',
    defaultModelCode: 'Q5',
    fetchModels: _.noop,
};

it('должен отправить данные формы, телефон и ИД пользователя при успешной авторизации c дефолтными данными', () => {
    const onSubmitMock = jest.fn();
    const tree = shallow(
        <MatchApplicationModalForm
            { ...PROPS }
            onSubmit={ onSubmitMock }
        />,
    );
    tree.find(MatchApplicationLazyAuthDesktop).simulate('authSuccess', '79999999999');
    expect(onSubmitMock).toHaveBeenCalledWith({
        isCredit: false,
        mark: 'AUDI',
        model: 'Q5',
        phone: '79999999999',
        tradeIn: false,
    });
});

it('должен отправить данные формы, телефон и ИД пользователя при успешной авторизации после заполнения формы', () => {
    const onSubmitMock = jest.fn();
    const tree = shallow(
        <MatchApplicationModalForm
            { ...PROPS }
            onSubmit={ onSubmitMock }
        />,
    );
    tree.find({ name: 'mark' }).simulate('change', [ 'BMW' ], { name: 'mark' });
    tree.find({ name: 'model' }).simulate('change', 'X1', { name: 'model' });
    tree.find({ name: 'tradeIn' }).simulate('check', true, { name: 'tradeIn' });
    tree.find({ name: 'isCredit' }).simulate('check', true, { name: 'isCredit' });
    tree.find(MatchApplicationLazyAuthDesktop).simulate('authSuccess', '79999999999');
    expect(onSubmitMock).toHaveBeenCalledWith({
        isCredit: true,
        mark: 'BMW',
        model: 'X1',
        phone: '79999999999',
        tradeIn: true,
    });
});
