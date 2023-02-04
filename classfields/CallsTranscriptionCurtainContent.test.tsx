/**
 * @jest-environment node
 */

import React from 'react';
import { shallow } from 'enzyme';
import _ from 'lodash';

import type { CallTranscription_Speaker } from '@vertis/schema-registry/ts-types-snake/auto/calltracking/model';

import context from 'autoru-frontend/mocks/contextMock';

import callsMock from 'www-cabinet/react/dataDomain/calls/mocks/withCalls.mock';
import type { EnrichedCall } from 'www-cabinet/react/dataDomain/calls/types';

import CallsTranscriptionCurtainContent from './CallsTranscriptionCurtainContent';

const scrollIntoViewMock = jest.fn();
const getElementByIdMock = jest.fn(() => ({ scrollIntoView: scrollIntoViewMock } as unknown as HTMLElement));

const call = { ...callsMock.callsList.calls[0] } as unknown as EnrichedCall;
call.transcription = [
    {
        speaker: 'TARGET' as CallTranscription_Speaker,
        start_time_millis: 1040,
        end_time_millis: 9640,
        text: 'пришивайте <b>подворотничок</b> к воротничку',
    },
    {
        speaker: 'SOURCE' as CallTranscription_Speaker,
        start_time_millis: 9640,
        end_time_millis: 14640,
        text: 'а мы не умеем',
    },
    {
        speaker: 'TARGET' as CallTranscription_Speaker,
        start_time_millis: 9780,
        end_time_millis: 15950,
        text: 'никто не умеет… дело не в умении, не в желании, и вообще ни в чём. дело в самом пришивании <b>подворотничка</b>',
    },
];

const defaultProps = {
    sendComplaint: () => Promise.resolve(),
    downloadRecord: () => {},
    showErrorNotification: () => {},
    updateCallById: () => {},
    isLoading: false,
    call,
};

let originalDocument: Document;

beforeEach(() => {
    originalDocument = global.document;
    global.document = {
        getElementById: getElementByIdMock,
    } as unknown as Document;
});

afterEach(() => {
    global.document = originalDocument;
});

it('покажет лоадер, если isLoading = true', () => {
    const tree = shallowRenderComponent({ ...defaultProps, isLoading: true });
    expect(tree.find('Loader')).toExist();
});

it('покажет сообщение, если стенограмма недоступна', () => {
    const callWithNoTranscription = _.cloneDeep(call);
    callWithNoTranscription.transcription_available = false;
    const tree = shallowRenderComponent({ ...defaultProps, call: callWithNoTranscription });

    expect(tree.find('.CallsTranscriptionCurtainContent__notAvailable')).toExist();
});

it('при клике на плей возле фразы запустит плеер с нужной секунды', () => {
    const tree = shallowRenderComponent();

    const playButton = tree.find('.CallsTranscriptionCurtainContent__phrasePlayerControl').at(1);
    const phraseStartTime = 5;
    playButton.simulate('click', { currentTarget: { getAttribute: () => phraseStartTime } });

    const player = tree.find('CallsPlayer');

    expect({ isPlaying: player.prop('isPlaying'), playStartTime: player.prop('playStartTime') }).toEqual({
        isPlaying: true,
        playStartTime: 5,
    });
});

it('подскроллит к фразе с поисковым запросом после загрузки стенограммы', () => {
    global.innerHeight = 400;
    const tree = shallowRenderComponent({ ...defaultProps, isLoading: true });
    tree.setProps({ isLoading: false });

    const firstPhraseWithKeywordIndex = call.transcription?.findIndex(phrase => phrase.text.includes('<b>'));

    expect(getElementByIdMock).toHaveBeenCalledWith('phrase-' + firstPhraseWithKeywordIndex);
    expect(scrollIntoViewMock).toHaveBeenCalled();
});

function shallowRenderComponent(props = defaultProps) {
    return shallow(
        <CallsTranscriptionCurtainContent { ...props }/>,
        { context },
    );
}
