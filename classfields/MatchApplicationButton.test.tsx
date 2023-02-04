import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import _ from 'lodash';
import { InView } from 'react-intersection-observer';

import contextMock from 'autoru-frontend/mocks/contextMock';
import newCardMock from 'autoru-frontend/mockData/state/newCard.mock';

import MatchApplicationButtonDumb from './MatchApplicationButtonDumb';

const offer = newCardMock.card;

const promiseFunc = () => Promise.resolve();

const PROPS = {
    onCheck: promiseFunc,
    onSubmit: promiseFunc,
    marks: [],
    models: [],
    mmm: [],
    offer: offer,
    fetchMarks: _.noop,
    fetchModels: _.noop,
    onModalOpen: _.noop,
    onModalClose: _.noop,
    text: 'Показать лучшие предложения',
};

it('должен отрендерить модал со ссылкой на пользовательское соглашение  для неавторизованного пользователя', () => {
    const tree = shallow(
        <MatchApplicationButtonDumb
            { ...PROPS }
        />,
        { context: contextMock },
    );

    tree.find('Button').simulate('click');

    expect(shallowToJson(tree.find('MatchApplicationModalDesktop').dive())).toMatchSnapshot();
});

it('должен отрендерить сниппет при закрытии модала', () => {
    const tree = shallow(
        <MatchApplicationButtonDumb
            { ...PROPS }
        />,
        { context: contextMock },
    );
    tree.find('Button').simulate('click');
    tree.find('MatchApplicationModalDesktop').simulate('closeModal');

    expect(tree).not.toBeEmptyRender();
});

it('не должен отрендерить сниппет при закрытии модала после отправки формы', () => {
    const tree = shallow(
        <MatchApplicationButtonDumb
            { ...PROPS }
        />,
        { context: contextMock },
    );

    expect(tree).not.toBeEmptyRender();

    tree.find('Button').simulate('click');

    tree.find('MatchApplicationModalDesktop').simulate('submit');

    (tree.find('MatchApplicationModalDesktop').props() as { onSuccessClose(): void }).onSuccessClose();

    expect(tree).toBeEmptyRender();
});

describe('должен отправить метрику', () => {
    it('при появлении сниппета в области видимости', () => {
        const tree = shallow(
            <MatchApplicationButtonDumb
                { ...PROPS }
            />,
            { context: contextMock },
        );
        tree.find(InView).simulate('change', true);
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'get-best-price', 'block-view' ]);
    });

    it('при открытии попапа', () => {
        const tree = shallow(
            <MatchApplicationButtonDumb
                { ...PROPS }
            />,
            { context: contextMock },
        );
        tree.find('Button').simulate('click');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'get-best-price', 'popup-open' ]);
    });

    it('при отправке формы', () => {
        const tree = shallow(
            <MatchApplicationButtonDumb
                { ...PROPS }
            />,
            { context: contextMock },
        );
        tree.find('Button').simulate('click');

        tree.find('MatchApplicationModalDesktop').simulate('submit');
        expect(contextMock.metrika.sendPageEvent).toHaveBeenCalledWith([ 'get-best-price', 'form-submit' ]);
    });
});
