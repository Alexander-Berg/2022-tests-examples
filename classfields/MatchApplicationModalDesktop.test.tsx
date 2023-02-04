import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import _ from 'lodash';

import type { TBreadcrumbsMark, TBreadcrumbsModel } from 'auto-core/types/TBreadcrumbs';

import MatchApplicationModalDesktop from './MatchApplicationModalDesktop';

const RESOLVE = () => Promise.resolve();
const REJECT_ERROR = () => Promise.reject('ERROR');
const REJECT_CONFLICT = () => Promise.reject('CONFLICT');

const DEFAULT_PROPS = {
    onCheck: RESOLVE,
    onSubmit: RESOLVE,
    onSuccessClose: _.noop,
    onCloseModal: _.noop,
    marks: [ { id: 'AUDI', name: 'Audi' } as TBreadcrumbsMark ],
    models: [ { id: 'Q5', name: 'Q5' } as TBreadcrumbsModel ],
    defaultMarkCode: 'AUDI',
    defaultModelCode: 'Q5',
    fetchMarks: _.noop,
    fetchModels: _.noop,
    visible: true,
};

it('должен отрендерить в правой колонке форму по дефолту', () => {
    const tree = shallow(
        <MatchApplicationModalDesktop
            { ...DEFAULT_PROPS }
        />,
    );
    const column = tree.find('.MatchApplicationModalDesktop__column').at(1);
    expect(shallowToJson(column)).toMatchSnapshot();
});

it('должен отрендерить в правой колонке уведомление с просьбой выбрать другой авто, если проверка показала, что нет предложений по такой марке-модели', () => {
    return new Promise<void>((done) => {
        const tree = shallow(
            <MatchApplicationModalDesktop
                { ...DEFAULT_PROPS }
                onCheck={ REJECT_ERROR }
            />,
        );
        tree.find('MatchApplicationModalForm').simulate('submit', {});
        setTimeout(
            () => {
                const column = tree.find('.MatchApplicationModalDesktop__column').at(1);
                expect(shallowToJson(column)).toMatchSnapshot();
                done();
            },
            0,
        );
    });
});

it('должен отрендерить в правой колонке уведомление о том, что такая заявка уже есть, если отправка формы вернула ошибку CONFLICT', () => {
    return new Promise<void>((done) => {
        const tree = shallow(
            <MatchApplicationModalDesktop
                { ...DEFAULT_PROPS }
                onCheck={ RESOLVE }
                onSubmit={ REJECT_CONFLICT }
            />,
        );
        tree.find('MatchApplicationModalForm').simulate('submit', {});
        setTimeout(
            () => {
                const column = tree.find('.MatchApplicationModalDesktop__column').at(1);
                expect(shallowToJson(column)).toMatchSnapshot();
                done();
            },
            0,
        );
    });
});

it('должен отрендерить в правой колонке уведомление об ошибке, если отправка формы вернула любую ошибку, кроме CONFLICT', () => {
    return new Promise<void>((done) => {
        const tree = shallow(
            <MatchApplicationModalDesktop
                { ...DEFAULT_PROPS }
                onCheck={ RESOLVE }
                onSubmit={ REJECT_ERROR }
            />,
        );
        tree.find('MatchApplicationModalForm').simulate('submit', {});
        setTimeout(
            () => {
                const column = tree.find('.MatchApplicationModalDesktop__column').at(1);
                expect(shallowToJson(column)).toMatchSnapshot();
                done();
            },
            0,
        );
    });
});

it('должен отрендерить в правой колонке уведомление об успешной отправке, в случае успешной отправки формы', () => {
    return new Promise<void>((done) => {
        const tree = shallow(
            <MatchApplicationModalDesktop
                { ...DEFAULT_PROPS }
                onCheck={ RESOLVE }
                onSubmit={ RESOLVE }
            />,
        );
        tree.find('MatchApplicationModalForm').simulate('submit', {});
        setTimeout(
            () => {
                const column = tree.find('.MatchApplicationModalDesktop__column').at(1);
                expect(shallowToJson(column)).toMatchSnapshot();
                done();
            },
            0,
        );
    });
});
