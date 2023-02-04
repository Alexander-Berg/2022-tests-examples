jest.mock('auto-core/lib/clipboard', () => ({ copyText: jest.fn() }));

import React from 'react';
import { shallow } from 'enzyme';
import { shallowToJson } from 'enzyme-to-json';
import _ from 'lodash';

import type { Call, CallBilling } from '@vertis/schema-registry/ts-types-snake/auto/calltracking/model';

import contextMock from 'autoru-frontend/mocks/contextMock';

import { copyText } from 'auto-core/lib/clipboard';

import CallsListingItemSystemTag from 'www-cabinet/react/components/CallsListingItemSystemTag/CallsListingItemSystemTag';
import withCalls from 'www-cabinet/react/dataDomain/calls/mocks/withCalls.mock';
import settingsMock from 'www-cabinet/react/dataDomain/calls/mocks/withSettings.mock';

import CallsListingItem from './CallsListingItem';

const callMock = withCalls.callsList.calls[0] as unknown as Call & { can_play: boolean };

const baseProps = {
    call: callMock,
    callsSettings: settingsMock,
    hasCallTariff: true,
    isActive: false,
    isPlaying: false,
    dealerId: 20101,
    isClient: true,

    sendComplaint: jest.fn().mockResolvedValue(undefined),
    sendRedirectComplaint: jest.fn().mockResolvedValue(undefined),
    downloadRecord: _.noop,
    addTag: () => Promise.resolve(),
    removeTag: () => Promise.resolve(),
    getTags: () => Promise.resolve([]),
    playCallRecord: _.noop,
    pauseCallRecord: _.noop,
    onTranscriptionClick: _.noop,
};

it('должен писать "Пропущенный" в колонке про запись, если результат звонка не является успешным', () => {
    const callMockClone = _.cloneDeep(callMock);
    callMockClone.result = 'NO_CONFIRMATION' as Call['result'];

    const tree = shallow(
        <CallsListingItem { ...baseProps } call={ callMockClone }/>,
        { context: contextMock },
    );

    const callInfoColumn = tree.find('.CallsListingItem__call').childAt(0).children().children();

    expect(shallowToJson(callInfoColumn)).toBe('Пропущенный');
});

it('не должен рендерить тег с жалобой, если статус жалобы NO_COMPLAINT', () => {
    const callMockClone = _.cloneDeep(callMock);
    callMockClone!.billing!.complaint_state = 'NO_COMPLAINT' as CallBilling['complaint_state'];

    const tree = shallow(
        <CallsListingItem { ...baseProps } call={ callMockClone }/>,
        { context: contextMock },
    );

    const complaintTag = tree.find({ type: CallsListingItemSystemTag.TYPE.WITH_COMPLAINT });

    expect(shallowToJson(complaintTag)).toBeNull();
});

it('должен рендерить тег с жалобой с переданным статусом, если статус жалобы не NO_COMPLAINT', () => {
    const tree = shallow(
        <CallsListingItem { ...baseProps }/>,
        { context: contextMock },
    );

    const complaintTag = tree.find({ type: CallsListingItemSystemTag.TYPE.WITH_COMPLAINT });

    expect(shallowToJson(complaintTag)).toMatchSnapshot();
});

it('должен вызывать экшен воспроизведения, если звонок не проигрывается', () => {
    const playCallRecord = jest.fn();

    const tree = shallow(
        <CallsListingItem { ...baseProps } playCallRecord={ playCallRecord }/>,
        { context: contextMock },
    );

    (tree.instance() as CallsListingItem).onClickPlayer();

    expect(playCallRecord).toHaveBeenCalledWith(baseProps.call);
});

it('должен вызывать экшен паузы, если звонок проигрывается', () => {
    const pauseCallRecord = jest.fn();

    const tree = shallow(
        <CallsListingItem { ...baseProps } pauseCallRecord={ pauseCallRecord } isPlaying={ true }/>,
        { context: contextMock },
    );

    (tree.instance() as CallsListingItem).onClickPlayer();

    expect(pauseCallRecord).toHaveBeenCalled();
});

it('должен копировать текст при клике на текст', () => {
    const tree = shallow(
        <CallsListingItem { ...baseProps } isManager={ true }/>,
        { context: contextMock },
    );
    tree.setState({ isVisibleMenu: true });

    const menuItems = tree.find('.CallsListingItem__menuItem');
    menuItems.at(menuItems.length - 1).simulate('click');

    expect(copyText).toHaveBeenCalledWith(callMock?.external_id?.id);
});

it('должен не показывать в выпадающем меню пункт "копировать id" не менеджеру', () => {
    const tree = shallow(
        <CallsListingItem { ...baseProps }/>,
        { context: contextMock },
    );
    tree.setState({ isVisibleMenu: true });

    const menuItems = tree.find('.CallsListingItem__menuItem');

    expect(menuItems.findWhere(node => node.text() === 'Скопировать ID звонка')).not.toExist();
});
