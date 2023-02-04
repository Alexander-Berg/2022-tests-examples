import React from 'react';
import { render } from 'jest-puppeteer-react';
import noop from 'lodash/noop';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import type { AnyObject } from 'realty-core/types/utils';
import { AddressInfoStatus, IAddress } from 'realty-core/view/react/common/types/egrnPaidReport';

import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import { EGRNAddressPurchaseForm } from '../';
import suggestStyles from '../../EGRNAddressPurchaseSuggest/styles.module.css';

import { addressInfoMock, store } from './mocks';

const Component: React.FC<{ Gate?: AnyObject }> = ({ Gate }) => (
    <AppProvider initialState={store} Gate={Gate}>
        <EGRNAddressPurchaseForm />
    </AppProvider>
);

const successTemplateTest = async (addressInfo: IAddress) => {
    const Gate = {
        create: (action: string) => {
            switch (action) {
                case 'egrn-address-suggest.flatSuggest':
                    return Promise.resolve([{ cadastralNumber: '78:11:0006078:9239' }]);
                case 'egrn-paid-report.createAddressInfo':
                    return Promise.resolve(addressInfo);
            }
        },
    };

    await render(<Component Gate={Gate} />, {
        viewport: { width: 900, height: 700 },
    });

    await page.type('input', '78:11:0006078:9239');
    await page.waitFor(300);
    await page.keyboard.press('ArrowDown');
    await page.waitFor(100);
    await page.click(`.${suggestStyles.submitButton}`);
    await page.waitFor(100);
};

const errorTemplateTest = async (createAddressInfoError?: IAddress | AnyObject) => {
    const Gate = {
        create: (action: string) => {
            switch (action) {
                case 'egrn-address-suggest.flatSuggest':
                    return Promise.resolve([{ cadastralNumber: '78:11:0006078:9239' }]);
                case 'egrn-paid-report.createAddressInfo':
                    return Promise.reject(createAddressInfoError);
            }
        },
    };

    await render(<Component Gate={Gate} />, {
        viewport: { width: 900, height: 550 },
    });

    await page.type('input', '78:11:0006078:9239');
    await page.waitFor(300);
    await page.keyboard.press('ArrowDown');
    await page.waitFor(100);
    await page.click(`.${suggestStyles.submitButton}`);
    await page.waitFor(100);
};

describe('EGRNAddressPurchaseForm', () => {
    it('рендерится в дефолтном состоянии', async () => {
        await render(<Component />, {
            viewport: { width: 900, height: 450 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('рендерится в состоянии загрузки', async () => {
        const Gate = {
            create: (action: string) => {
                switch (action) {
                    case 'egrn-address-suggest.flatSuggest':
                        return Promise.resolve([{ cadastralNumber: '78:11:0006078:9239' }]);
                    case 'egrn-paid-report.createAddressInfo':
                        return new Promise(noop);
                }
            },
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 900, height: 650 },
        });

        await page.type('input', '78:11:0006078:9239');
        await page.waitFor(300);
        await page.keyboard.press('ArrowDown');
        await page.waitFor(100);
        await page.click(`.${suggestStyles.submitButton}`);
        await page.waitFor(100);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    describe('Рендер ошибки', () => {
        it('сетевая ошибка', async () => {
            await errorTemplateTest();

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('кадастровый номер не найден', async () => {
            await errorTemplateTest({ ...addressInfoMock, status: AddressInfoStatus.FAILED_TO_GET_CADASTRAL_NUMBER });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('данные о квартире не найдены', async () => {
            await errorTemplateTest({ ...addressInfoMock, status: AddressInfoStatus.FAILED_TO_GET_OBJECT_INFO });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('неизвестная ошибка (бэкендная)', async () => {
            await errorTemplateTest({ ...addressInfoMock, status: AddressInfoStatus.ERROR });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });

    describe('рендер найденного адреса', () => {
        it('дефолт', async () => {
            await successTemplateTest({
                ...addressInfoMock,
                evaluatedObjectInfo: {
                    ...addressInfoMock.evaluatedObjectInfo,
                    area: 1.1,
                },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('с длинным адресом и большой площадью', async () => {
            await successTemplateTest({
                ...addressInfoMock,
                evaluatedObjectInfo: {
                    ...addressInfoMock.evaluatedObjectInfo,
                    area: 900000000,
                    rrAddress:
                        'г. Санкт-Петербург, улица Великого Анатолия Коломяжского, дом 19, квартал Б, корп. 9, кв. 77' +
                        'wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww',
                },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });

        it('без площади и этажа', async () => {
            await successTemplateTest({
                ...addressInfoMock,
                evaluatedObjectInfo: {
                    ...addressInfoMock.evaluatedObjectInfo,
                    area: undefined,
                    floor: undefined,
                },
            });

            expect(await takeScreenshot()).toMatchImageSnapshot();
        });
    });
});
