import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import type { Call } from '@vertis/schema-registry/ts-types-snake/auto/calltracking/model';

import context from 'autoru-frontend/mocks/contextMock';

import getId from 'www-cabinet/react/lib/calls/getId';
import callsMock from 'www-cabinet/react/dataDomain/calls/mocks/withCalls.mock';

import CallsPlayer from './CallsPlayer';

const calls = callsMock.callsList.calls as unknown as Array<Call>;

const baseProps = {
    isVisible: true,
    isPlaying: false,

    sendComplaint: () => Promise.resolve(),
    downloadRecord: _.noop,
    playCallRecord: _.noop,
    pauseCallRecord: _.noop,
    hidePlayer: _.noop,
};

it('должен формировать в title длительность ожидания, имя оффера и телефон', () => {
    const tree = shallow(
        <CallsPlayer { ...baseProps } call={ calls[0] }/>,
        { context },
    );

    const title = (tree.instance() as CallsPlayer).renderTitle();

    expect(title).toMatchSnapshot();
});

it('не должен формировать в title параметры звонка, если их нет', () => {
    const callMock = _.cloneDeep(calls[1]);
    callMock.wait_duration!.seconds = 0;
    callMock.source = undefined;

    const tree = shallow(
        <CallsPlayer { ...baseProps } call={ callMock }/>,
        { context },
    );

    const title = (tree.instance() as CallsPlayer).renderTitle();

    expect(title).toBeNull();
});

it('должен закрыть плеер, показать нотификацию об ошибке, спрятать кнопку звонка, обновив поле can_play в store, и скачать запись', () => {
    const callMock = _.cloneDeep(calls[1]);
    callMock.wait_duration!.seconds = 0;
    callMock.source = undefined;
    const showErrorNotification = jest.fn();
    const hidePlayer = jest.fn();
    const updateCallById = jest.fn();
    const downloadRecord = jest.fn();
    const tree = shallow(
        <CallsPlayer
            { ...baseProps }
            call={ callMock }
            showErrorNotification={ showErrorNotification }
            hidePlayer={ hidePlayer }
            updateCallById={ updateCallById }
            downloadRecord={ downloadRecord }
        />,
        { context },
    );

    (tree.instance() as CallsPlayer).onError();

    const callId = getId(callMock);

    expect(hidePlayer).toHaveBeenCalled();
    expect(showErrorNotification).toHaveBeenCalledWith('Запись не может быть проиграна');
    expect(updateCallById).toHaveBeenCalledWith(callId, { can_play: false });
    expect(downloadRecord).toHaveBeenCalledWith(callId, 'Запись не может быть проиграна');
});
