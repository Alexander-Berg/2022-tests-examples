/**
 * @jest-environment node
 */

import React from 'react';
import { Provider } from 'react-redux';
import { render } from 'enzyme';
import { AdSourceType } from '@vertis/ads/build/types/types';

import mockStore from 'autoru-frontend/mocks/mockStore';
import contextMock from 'autoru-frontend/mocks/contextMock';
import createContextProvider from 'autoru-frontend/mocks/createContextProvider';
import cloneOfferWithHelpers from 'autoru-frontend/mockData/state/helpers/offer/cloneOfferWithHelpers';

import { AdPlace } from 'auto-core/lib/ads/AD2.types';

import configStateMock from 'auto-core/react/dataDomain/config/mock';

import Ad from './Ad';
import type { ReduxState } from './Ad';

const Context = createContextProvider(contextMock);

describe('тестовый баннер', () => {
    describe('statId', () => {
        let state: Partial<ReduxState>;
        beforeEach(() => {
            state = {
                ads: {
                    data: {
                        code: 'card',
                        data: {},
                        settings: {
                            r1: {
                                id: 'some_id',
                                sources: [
                                    {
                                        type: AdSourceType.RTB,
                                        extParams: { awaps_section: '29337' },
                                        code: 'R-A-464111-1',
                                    },
                                ],
                                stat_id_by_exp: {
                                    exp1: '101',
                                },
                            },
                        },
                        statId: '100',
                    },
                    isFetching: false,
                },
                card: cloneOfferWithHelpers({}).withSalon({ client_id: '123' }).value(),
                config: configStateMock.withPageType('card').value(),
            };
        });

        it('должен передать stat-id из общего конфига без экспа', () => {
            state.config = configStateMock.withPageType('index').value();
            const decodedJsData = renderAndGetJsData(state);

            expect(decodedJsData).toHaveProperty('js.statId', '100');
        });

        it('должен передать stat-id из stat_id_by_exp, если есть эксп', () => {
            contextMock.hasExperiment.mockImplementation((exp) => exp === 'exp1');
            state.config = configStateMock.withPageType('index').value();
            const decodedJsData = renderAndGetJsData(state);

            expect(decodedJsData).toHaveProperty('js.statId', '101');
        });

        function renderAndGetJsData(state: Partial<ReduxState>) {
            const wrapper = render(
                <Provider store={ mockStore(state) }><Context><Ad place={ AdPlace.R1 }/></Context></Provider>,
            );
            const htmlContent = wrapper.find('script').html();
            // eslint-disable-next-line regexp/no-super-linear-backtracking
            const encodedJsData = htmlContent && htmlContent.match(/new AD2\(.*?,\s*(?<jsData>.*)\s*[,)]/);
            if (!encodedJsData || !encodedJsData.groups) {
                throw new Error(`Unexpected script contents: ${ htmlContent }`);
            }
            return JSON.parse(encodedJsData.groups.jsData);
        }
    });
});
