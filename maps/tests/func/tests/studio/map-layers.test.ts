import {URL} from 'url';
import * as qs from 'qs';
import {waitForSelector, clickAndWaitForRequest, openStudio, clickToSelector} from '../../utils/commands';
import {SELECTORS} from './selectors';

describe('Растровая карта', () => {
    beforeEach(async () => {
        await openStudio({mapMode: 'raster', z: 16});
        await waitForSelector(SELECTORS.container);
    });

    test('Ночной режим', async () => {
        await clickToSelector(SELECTORS.mapStatusBar.layersMenuButton);
        await clickAndWaitForRequest(SELECTORS.mapLayersMenu.designControl, (req) => {
            const url = new URL(req.url());
            const query = qs.parse(url.search, {ignoreQueryPrefix: true});

            return url.pathname.includes('tiles') && 'mode' in query && query.mode === 'night';
        });
    });

    describe.each<[string, string, ...string[]]>([
        ['carparks', SELECTORS.mapLayersMenu.carparksControl],
        ['trf', SELECTORS.mapLayersMenu.trfControl],
        ['trfe', SELECTORS.mapLayersMenu.trfeControl],
        ['mrce', SELECTORS.mapLayersMenu.mrceControl, SELECTORS.mapLayersMenu.streetViewControl],
        ['mrcpe', SELECTORS.mapLayersMenu.mrcpeControl, SELECTORS.mapLayersMenu.streetViewControl],
        [
            'sta,stv',
            SELECTORS.mapLayersMenu.stvControl,
            SELECTORS.mapLayersMenu.streetViewControl,
            SELECTORS.mapLayersMenu.mrcpeControl
        ]
    ])('Слои карты', (l: string, selector: string, ...addSelectors: string[]) => {
        test(`Слой ${l}`, async () => {
            await clickToSelector(SELECTORS.mapStatusBar.layersMenuButton);

            if (addSelectors) {
                for (let i = 0; i < addSelectors.length; i++) {
                    await clickToSelector(addSelectors[i]);
                }
            }

            await clickAndWaitForRequest(selector, (req) => {
                const url = new URL(req.url());
                const query = qs.parse(url.search, {ignoreQueryPrefix: true});

                return (
                    url.pathname.includes('tiles') &&
                    'x' in query &&
                    'y' in query &&
                    'z' in query &&
                    'l' in query &&
                    'format' in query &&
                    'style_key' in query &&
                    'l' in query &&
                    query.l === l
                );
            });
        });
    });
});
