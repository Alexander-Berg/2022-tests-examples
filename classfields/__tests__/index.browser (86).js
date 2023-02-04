import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import DeveloperPageFooterLinks from '../';

const developer = {
    id: '1',
    name: 'Самолет',
    geoStatistic: {
        regions: [ {
            geoId: 11176,
            rgid: '250682',
            subjectFederationName: 'Тюменская',
            locativeSubjectFederationName: 'в Тюменской области'
        }, {
            geoId: 10693,
            rgid: '475523',
            subjectFederationName: 'Калужская',
            locativeSubjectFederationName: 'в Калужской области'
        }, {
            geoId: 10174,
            rgid: '741965',
            subjectFederationName: 'Санкт-Петербург и ЛО',
            locativeSubjectFederationName: 'в Санкт-Петербурге и ЛО'
        }, {
            geoId: 11108,
            rgid: '426964',
            subjectFederationName: 'Пермский',
            locativeSubjectFederationName: 'в Пермском крае'
        }, {
            geoId: 11029,
            rgid: '211571',
            subjectFederationName: 'Ростовская',
            locativeSubjectFederationName: 'в Ростовской области'
        }, {
            geoId: 10841,
            rgid: '475525',
            subjectFederationName: 'Ярославская',
            locativeSubjectFederationName: 'в Ярославской области'
        }, {
            geoId: 11162,
            rgid: '326698',
            subjectFederationName: 'Свердловская',
            locativeSubjectFederationName: 'в Свердловской области'
        }, {
            geoId: 10995,
            rgid: '353118',
            subjectFederationName: 'Краснодарский',
            locativeSubjectFederationName: 'в Краснодарском крае'
        } ]
    }
};

describe('DeveloperPageFooterLinks', () => {
    it('рисует заголовок ссылки с передаными регионами', async() => {
        await render(
            <div>
                <AppProvider initialState={{}}>
                    <DeveloperPageFooterLinks developer={developer} />
                </AppProvider>
            </div>,
            { viewport: { width: 400, height: 350 } }
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });
});
