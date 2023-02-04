import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import _ from 'lodash';

import contextMock from 'autoru-frontend/mocks/contextMock';

import PageMatchApplication from './PageMatchApplicationDumb';

const RESOLVE = () => Promise.resolve();
const REJECT_ERROR = () => Promise.reject('ERROR');
const REJECT_CONFLICT = () => Promise.reject('CONFLICT');

it('должен отрендерить модал с просьбой выбрать другой авто, если проверка показала, что нет предложений по такой марке-модели', () => {
    return new Promise<void>((done) => {
        const tree = shallow(
            <PageMatchApplication
                marks={ [] }
                models={ [] }
                fetchModels={ _.noop }
                onCheck={ REJECT_ERROR }
                onSubmit={ RESOLVE }
                params={{}}
                shouldRenderPage
            />,
            { context: contextMock },
        );
        tree.find('MatchApplicationForm').simulate('submit', {});
        setTimeout(
            () => {
                const modal = tree.find('MatchApplicationModal').dive();
                expect(shallowToJson(modal)).toMatchSnapshot();
                done();
            },
            0,
        );
    });
});

it('должен отрендерить модал c информацией об успешной отправке, в случае успешной отправки формы', () => {
    return new Promise<void>((done) => {
        const tree = shallow(
            <PageMatchApplication
                marks={ [] }
                models={ [] }
                fetchModels={ _.noop }
                onCheck={ RESOLVE }
                onSubmit={ RESOLVE }
                params={{}}
                shouldRenderPage
            />,
            { context: contextMock },
        );
        tree.find('MatchApplicationForm').simulate('submit', {});
        setTimeout(
            () => {
                const modal = tree.find('MatchApplicationModal').dive();
                expect(shallowToJson(modal)).toMatchSnapshot();
                done();
            },
            0,
        );
    });
});

it('должен отрендерить модал c информацией о том, что заявка уже есть, если отправка формы вернула ошибку CONFLICT', () => {
    return new Promise<void>((done) => {
        const tree = shallow(
            <PageMatchApplication
                marks={ [] }
                models={ [] }
                fetchModels={ _.noop }
                onCheck={ RESOLVE }
                onSubmit={ REJECT_CONFLICT }
                params={{}}
                shouldRenderPage
            />,
            { context: contextMock },
        );
        tree.find('MatchApplicationForm').simulate('submit', {});
        setTimeout(
            () => {
                const modal = tree.find('MatchApplicationModal').dive();
                expect(shallowToJson(modal)).toMatchSnapshot();
                done();
            },
            0,
        );
    });
});

it('должен отрендерить модал c информацией об ошибке, если отправка формы вернула любую ошибку, кроме CONFLICT', () => {
    return new Promise<void>((done) => {
        const tree = shallow(
            <PageMatchApplication
                marks={ [] }
                models={ [] }
                fetchModels={ _.noop }
                onCheck={ RESOLVE }
                onSubmit={ REJECT_ERROR }
                params={{}}
                shouldRenderPage
            />,
            { context: contextMock },
        );
        tree.find('MatchApplicationForm').simulate('submit', {});
        setTimeout(
            () => {
                const modal = tree.find('MatchApplicationModal').dive();
                expect(shallowToJson(modal)).toMatchSnapshot();
                done();
            },
            0,
        );
    });
});

it('должен отрендерить сообщение о недоступности сервиса, если функция недоступна в текущем регионе', () => {
    const tree = shallow(
        <PageMatchApplication
            marks={ [] }
            models={ [] }
            fetchModels={ _.noop }
            onCheck={ RESOLVE }
            onSubmit={ RESOLVE }
            params={{}}
            shouldRenderPage={ false }
        />,
        { context: contextMock },
    );
    const emptyPlaceholder = tree.find('.PageMatchApplication__emptyPlaceholder');
    expect(emptyPlaceholder).toExist();
});
