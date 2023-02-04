/**
 * @jest-environment node
 */
import type { AdsData } from '../../../types/types';
import type { Props } from './Ad';
import { AdSourceType } from '../../../types/types';

import Ad from './Ad';

import React from 'react';
import { render } from 'enzyme';

describe('тестовый баннер', () => {
    describe('statId', () => {
        let ads: AdsData;
        beforeEach(() => {
            ads = {
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
            };
        });

        it('должен передать stat-id из общего конфига без экспа', () => {
            const decodedJsData = renderAndGetJsData({ ads });

            expect(decodedJsData).toHaveProperty('js.statId', '100');
        });

        it('должен передать stat-id из stat_id_by_exp, если есть эксп', () => {
            const decodedJsData = renderAndGetJsData({
                ads,
                hasExperiment: (exp: string) => exp === 'exp1',
            });

            expect(decodedJsData).toHaveProperty('js.statId', '101');
        });

        function renderAndGetJsData(props: Partial<Props>) {
            const wrapper = render(
                <Ad
                    ads={{ code: 'index', data: {} }}
                    getClassForLayoutPlace={ jest.fn() }
                    getClassForPlace={ jest.fn() }
                    nonce=""
                    place="r1"
                    { ...props }
                />,
            );
            const htmlContent = wrapper.find('script').html();
            const encodedJsData = htmlContent && htmlContent.match(/new AD2\(.*?,\s*(?<jsData>.*)\s*[,)]/);
            if (!encodedJsData || !encodedJsData.groups) {
                throw new Error(`Unexpected script contents: ${ htmlContent }`);
            }
            return JSON.parse(encodedJsData.groups.jsData);
        }
    });
});
