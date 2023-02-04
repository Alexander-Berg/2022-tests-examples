import React from 'react';
import { shallow } from 'enzyme';

import context from 'autoru-frontend/mocks/contextMock';

import getId from 'www-cabinet/react/lib/calls/getId';
import callsMock from 'www-cabinet/react/dataDomain/calls/mocks/withCalls.mock';
import settingsMock from 'www-cabinet/react/dataDomain/calls/mocks/withSettings.mock';
import type { EnrichedCall } from 'www-cabinet/react/dataDomain/calls/types';

import CallsListing from './CallsListing';

const calls = callsMock.callsList.calls as unknown as Array<EnrichedCall>;

const getTranscriptionMock = jest.fn(() => Promise.resolve('SUCCESS'));

const baseProps = {
    callsPlayer: {},
    callsSettings: settingsMock,
    onShowMore: jest.fn(),
    sendComplaint: jest.fn().mockResolvedValue(undefined),
    sendRedirectComplaint: jest.fn().mockResolvedValue(undefined),
    downloadRecord: jest.fn(),
    addTag: () => Promise.resolve(),
    removeTag: () => Promise.resolve(),
    getTags: () => Promise.resolve([]),
    playCallRecord: jest.fn(),
    pauseCallRecord: jest.fn(),
    hidePlayer: jest.fn(),
    getTranscription: getTranscriptionMock,
    hasCallTariff: false,
    dealerId: 777,
    filters: {},
    callsList: calls as unknown as Array<EnrichedCall>,
};

it('должен послать запрос за стенограммой, если еще не посылал', () => {
    const tree = shallowRenderComponent();

    tree.find('CallsListingItem').at(0).simulate('transcriptionClick', getId(calls[0]));

    expect(getTranscriptionMock).toHaveBeenCalled();
});

it('не должен послать запрос за стенограммой, если еще не посылал', () => {
    const tree = shallowRenderComponent();
    tree.find('CallsListingItem').at(0).simulate('transcriptionClick', getId(calls[1]));

    expect(getTranscriptionMock).not.toHaveBeenCalled();
});

function shallowRenderComponent(props = baseProps) {
    return shallow(
        <CallsListing { ...props }/>,
        { context },
    );
}
