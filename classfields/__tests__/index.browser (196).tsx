import React from 'react';
import { render } from 'jest-puppeteer-react';

import takeScreenshot from '@realty-front/jest-utils/puppeteer/tests-helpers/take-screenshot';

import { AddressInfoStatus, IAddress } from 'realty-core/view/react/common/types/egrnPaidReport';
import { AppProvider } from 'realty-core/view/react/libs/test-helpers';

import reducer from 'view/reducers/pages/EGRNAddressPurchasePage';

import { EGRNAddressPurchaseFormContainer } from '../container';

import suggestStyles from '../../EGRNAddressPurchaseSuggest/styles.module.css';

import { addressInfoMock, store } from './mocks';

const modalInputSelector = `.${suggestStyles.modalContent} input`;
const suggestSubmitButtonSelector = `.${suggestStyles.modalContent} .${suggestStyles.submitButton}`;

const Component: React.FC<{ Gate?: Record<string, unknown> }> = ({ Gate }) => (
    <div style={{ backgroundColor: '#fafafa', margin: -20, padding: 20 }}>
        <AppProvider rootReducer={reducer} initialState={store} Gate={Gate}>
            <EGRNAddressPurchaseFormContainer />
        </AppProvider>
    </div>
);

const successTemplateTest = async (addressInfo: IAddress, width?: number, height?: number) => {
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
        viewport: { width: width ?? 360, height: height ?? 350 },
    });

    await page.click('input');
    await page.waitFor(300);
    await page.type(modalInputSelector, '78:11:0006078:9239');
    await page.waitFor(300);
    await page.keyboard.press('ArrowDown');
    await page.waitFor(100);
    await page.click(suggestSubmitButtonSelector);
    await page.waitFor(100);
};

const errorTemplateTest = async (
    createAddressInfoError?: IAddress | Record<string, unknown>,
    width?: number,
    height?: number
) => {
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
        viewport: { width: width ?? 360, height: height ?? 270 },
    });

    await page.click('input');
    await page.waitFor(300);
    await page.type(modalInputSelector, '78:11:0006078:9239');
    await page.waitFor(300);
    await page.keyboard.press('ArrowDown');
    await page.waitFor(100);
    await page.click(suggestSubmitButtonSelector);
    await page.waitFor(100);
};

describe('EGRNAddressPurchaseForm', () => {
    it('???????????????????? ?? ?????????????????? ??????????????????', async () => {
        await render(<Component />, {
            viewport: { width: 360, height: 220 },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('???????????????????? ?? ?????????????????? ????????????????', async () => {
        const Gate = {
            create: (action: string) => {
                switch (action) {
                    case 'egrn-address-suggest.flatSuggest':
                        return Promise.resolve([{ cadastralNumber: '78:11:0006078:9239' }]);
                    case 'egrn-paid-report.createAddressInfo':
                        return new Promise(() => undefined);
                }
            },
        };

        await render(<Component Gate={Gate} />, {
            viewport: { width: 360, height: 300 },
        });

        await page.click('input');
        await page.waitFor(300);
        await page.type(modalInputSelector, '78:11:0006078:9239');
        await page.waitFor(300);
        await page.keyboard.press('ArrowDown');
        await page.waitFor(100);
        await page.click(suggestSubmitButtonSelector);
        await page.waitFor(100);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('???????????????????? ?? ?????????????????? ???????????? (???????????? ???????? / ??????????????)', async () => {
        await errorTemplateTest();

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('???????????????????? ?? ?????????????????? ???????????? (???????????? ???????? / ??????????????) ?????????????????? ????????????????????', async () => {
        await errorTemplateTest(undefined, 600, 270);

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('???????????????????? ?? ?????????????????? ???????????? (???????????? ???????????????????? ???????????????????????? ????????????)', async () => {
        await errorTemplateTest({ ...addressInfoMock, status: AddressInfoStatus.FAILED_TO_GET_CADASTRAL_NUMBER });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('???????????????????? ?? ?????????????????? ???????????? (???????????? ???????????????????? ???????????? ???? ????????????????)', async () => {
        await errorTemplateTest({ ...addressInfoMock, status: AddressInfoStatus.FAILED_TO_GET_OBJECT_INFO });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('???????????????????? ?? ?????????????????? ???????????? (?????????????????????? ????????????)', async () => {
        await errorTemplateTest({ ...addressInfoMock, status: AddressInfoStatus.ERROR });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('???????????????????? ?? ?????????????????? ???????????? ?? ?????????????????? ??????????????, ?????????????????? ????????????????????', async () => {
        await successTemplateTest(
            {
                ...addressInfoMock,
                evaluatedObjectInfo: {
                    ...addressInfoMock.evaluatedObjectInfo,
                    area: 1.1,
                },
            },
            600,
            450
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('???????????????????? ?? ?????????????????? ???????????? ?? ?????????????????? ??????????????, ?????????????????? ????????????????????, ?? ???????????????? ??????????????', async () => {
        await successTemplateTest(
            {
                ...addressInfoMock,
                evaluatedObjectInfo: {
                    ...addressInfoMock.evaluatedObjectInfo,
                    area: 1.1,
                },
            },
            600,
            320
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('???????????????????? ?? ?????????????????? ???????????? ?? ?????????????????? ??????????????', async () => {
        await successTemplateTest({
            ...addressInfoMock,
            evaluatedObjectInfo: {
                ...addressInfoMock.evaluatedObjectInfo,
                area: 1.1,
            },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('???????????????????? ?? ???????????????? ?? ?????????????? ???????? ???????????? ?????????????? ??????????????', async () => {
        await successTemplateTest({
            ...addressInfoMock,
            evaluatedObjectInfo: {
                ...addressInfoMock.evaluatedObjectInfo,
                area: 900000000,
                rrAddress:
                    '??. ??????????-??????????????????, ?????????? ???????????????? ???????????????? ????????????????????????, ?????? 199, ?????????????? ??, ????????. 9??, ????. 77' +
                    'wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww',
            },
        });

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('???????????????????? ?? ?????????????????? ???????????? ?? ?????????????? ???????????????? ?? ?????????????? ??????????????', async () => {
        await successTemplateTest(
            {
                ...addressInfoMock,
                evaluatedObjectInfo: {
                    ...addressInfoMock.evaluatedObjectInfo,
                    area: 900000000,
                    rrAddress:
                        '??. ??????????-??????????????????, ?????????? ???????????????? ???????????????? ????????????????????????, ?????? 199, ?????????????? ??, ????????. 9, ????. 7' +
                        'wwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwwww',
                },
            },
            undefined,
            700
        );

        expect(await takeScreenshot()).toMatchImageSnapshot();
    });

    it('???????????????????? ?? ?????????????????? ???????????? ?????? ?????????????? ?? ??????????', async () => {
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
