import {expect} from 'chai';
import cssSelectors from '../../../common/css-selectors';
import getSelectorByText from '../../../lib/func/get-selector-by-text';

const EXPECTED_SIZES: Record<'letter' | 'landscape', WebdriverIO.Size> = {
    letter: {width: 775, height: 1483},
    landscape: {width: 1081, height: 935}
};

describe('Страница печати.', () => {
    beforeEach(async function () {
        await this.browser.setViewportSize({width: 1920, height: 1380});
        await this.browser.openPage('/print/213/moscow/?ll=37.637216,55.740598&z=12');
    });

    it('Содержит все нужные элементы', async function () {
        await this.browser.waitAndVerifyScreenshot(cssSelectors.printPage.view, 'print-page', {
            ignoreElements: cssSelectors.qrCodeView
        });
    });

    it('Меняет размер при смене размера страницы', async function () {
        const expectedSize = EXPECTED_SIZES.letter;
        await this.browser.waitAndClick(cssSelectors.printPage.topControls.size);
        await this.browser.waitAndClick(cssSelectors.printPage.sizes.letter);
        await this.browser.perform(async () => {
            const element = await this.browser.$(cssSelectors.printPage.firstPage);
            const size = await element.getSize();
            expect(roundSize(size)).to.be.deep.equal(expectedSize);
        }, `Проверить, что размер \`${cssSelectors.printPage.firstPage}\` соответствует ${expectedSize.width}px * ${expectedSize.height}px`);
    });

    it('Меняет размер при смене ориентации', async function () {
        const expectedSize = EXPECTED_SIZES.landscape;
        await this.browser.waitAndClick(cssSelectors.printPage.topControls.orientation);
        await this.browser.waitAndClick(cssSelectors.printPage.orientations.landscape);
        await this.browser.perform(async () => {
            const element = await this.browser.$(cssSelectors.printPage.firstPage);
            const size = await element.getSize();
            expect(roundSize(size)).to.be.deep.equal(expectedSize);
        }, `Проверить, что размер \`${cssSelectors.printPage.firstPage}\` соответствует ${expectedSize.width}px * ${expectedSize.height}px`);
    });

    it('Кнопка "Печать" открывает браузерный диалог печати', async function () {
        /* eslint-disable prefer-arrow-callback, no-undef, no-var, @typescript-eslint/no-explicit-any */
        function mockPrint() {
            var windowPrint = window.print;

            window.print = function () {
                (window as any)._printCalled = true;
                window.print = windowPrint;
            };
        }

        function checkPrint() {
            var value = (window as any)._printCalled;
            (window as any)._printCalled = undefined;
            return value;
        }
        /* eslint-enable prefer-arrow-callback, no-undef, no-var, @typescript-eslint/no-explicit-any */

        await this.browser.perform(async () => {
            await this.browser.execute(mockPrint);
        }, null);
        await this.browser.waitAndClick(getSelectorByText('Печать', cssSelectors.printPage.firstPage));

        await this.browser.perform(async () => {
            const wasPrintCalled = await this.browser.execute(checkPrint);
            if (!wasPrintCalled) {
                throw new Error('Диалог печати не открылся');
            }
        }, 'Проверить, что открылся браузерный диалог печати');
    });
});

describe('Пробрасывание pctx в печать', () => {
    it('Правильно написаны названия компаний.', async function () {
        await this.browser.setViewportSize({width: 1920, height: 1380});
        await this.browser.openPage(
            '/print/213/moscow/?ll=37.58537649999995%2C55.73481494770902&mode=routes&rtext=55.733842%2C38' +
                '.588144~55.735186%2C37.584529~55.735791%2C37.582898&' +
                'rtt=auto&ruri=ymapsbm1%3A%2F%2Forg%3Foid%3D1124715036~' +
                'ymapsbm1%3A%2F%2Forg%3Foid%3D1775296581~ymapsbm1%3A%2F%2Forg%3Foid%3D1685527330&z=17'
        );
        await this.browser.waitAndVerifyScreenshot(cssSelectors.printPage.routes.auto, 'routes-pctx-namings');
    });
});

function roundSize(size: WebdriverIO.Size): WebdriverIO.Size {
    return {
        width: Math.round(size.width),
        height: Math.round(size.height)
    };
}
