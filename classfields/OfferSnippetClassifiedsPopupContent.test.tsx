import 'jest-enzyme';
import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';
import MockDate from 'mockdate';

import {
    Multiposting_Classified_ClassifiedName as ClassifiedName,
    OfferStatus,
} from '@vertis/schema-registry/ts-types-snake/auto/api/api_offer_model';

import { nbsp } from 'auto-core/react/lib/html-entities';

import dayjs from 'auto-core/dayjs';
import type { MultipostingClassifiedAutoru } from 'auto-core/types/Multiposting';

import classifiedsMock from 'www-cabinet/react/dataDomain/sales/mock/multipostingClassifieds';

import OfferSnippetClassifiedsPopupContent from './OfferSnippetClassifiedsPopupContent';

const classifiedMock = classifiedsMock[0];
const classifiedMockAvito = classifiedsMock[1];

beforeEach(() => {
    MockDate.set('2020-11-01');
});

afterEach(() => {
    MockDate.reset();
});

it('отобразит баны, если они есть', () => {
    const tree = shallowRenderComponent();
    expect(tree.find('.OfferSnippetClassifiedsPopupContent__bansReasons')).toExist();
});

it('отобразить информацию о бане для Авито', () => {
    const classified = _.cloneDeep(classifiedMockAvito);
    classified.status = OfferStatus.BANNED;

    const tree = shallowRenderComponent(classified);
    expect(tree.find('.OfferSnippetClassifiedsPopupContent__bansReasons').text()).toEqual('<Link /> заблокировано');
});

it('отобразит кнопку "подключить", если классифайд выключен', () => {
    const tree = shallowRenderComponent({
        name: ClassifiedName.AUTORU,
        enabled: false,
        services: [],
        create_date: '1564682400000',
    } as Partial<MultipostingClassifiedAutoru> as MultipostingClassifiedAutoru);
    expect(tree.find('.OfferSnippetClassifiedsPopupContent__button').render().text()).toEqual('Подключить');
});

it('отобразит кнопку "отключить", если классифайд включен', () => {
    const tree = shallowRenderComponent();
    expect(tree.find('.OfferSnippetClassifiedsPopupContent__button').render().text()).toBe('Отключить');
});

it('не покажет услуги, если классифайд выключен', () => {
    const classified = _.cloneDeep(classifiedMock);
    classified.enabled = false;
    const tree = shallowRenderComponent(classified);

    expect(tree.find('.OfferSnippetClassifiedsPopupContent__services')).not.toExist();
});

it('покажет, сколько дней в продаже находится объявление, если есть их количество', () => {
    const classified = _.cloneDeep(classifiedMock);
    classified.status = OfferStatus.ACTIVE;
    const tree = shallowRenderComponent(classified);
    expect(tree.find('.OfferSnippetClassifiedsPopupContent__note').text()).toBe(`В продаже 6${ nbsp }дней`);
});

it('напишет, что объявление "активно еще n дней", если есть expire_date', () => {
    const classified = _.cloneDeep(classifiedMockAvito);
    classified.expire_date = String(dayjs(dayjs().add(5, 'day')).valueOf());

    const tree = shallowRenderComponent(classified);

    expect(tree.find('.OfferSnippetClassifiedsPopupContent__text').text())
        .toEqual(`Объявление активно ещё 5${ nbsp }дней — до 6 ноября`);
});

it('напишет, что объявление "активируется" для Авито', () => {
    const classified = _.cloneDeep(classifiedMockAvito);
    classified.expire_date = '';
    classified.status = OfferStatus.NEED_ACTIVATION;

    const tree = shallowRenderComponent(classified);

    expect(tree.find('.OfferSnippetClassifiedsPopupContent__text').text())
        .toBe('Объявление в процессе публикации');
});

it('напишет, что объявление "активируется" для Автору', () => {
    const classified = _.cloneDeep(classifiedMock);
    delete classified.bansReasons;
    const tree = shallowRenderComponent(classified, true);

    expect(tree.find('.OfferSnippetClassifiedsPopupContent__text').text())
        .toBe('Объявление в процессе публикации');
});

function shallowRenderComponent(classifieds = classifiedMock, isSaleNeedActivation = false) {
    const page = shallow(
        <OfferSnippetClassifiedsPopupContent
            classified={ classifieds }
            onClassifiedUpdate={ () => {} }
            isSaleNeedActivation={ isSaleNeedActivation }
        />,
    );
    return page;
}
