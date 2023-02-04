import {expect} from 'chai';
import cssSelectors from '../../../common/css-selectors';

describe('Фото.', () => {
    const viewport = {width: 1440, height: 900};

    beforeEach(async function () {
        await this.browser.setViewportSize(viewport);
    });

    it('Включение фотослоя', async function () {
        const params = {z: 10, ll: '37.640992,55.775639'};
        await this.browser.openPage(`?ll=${params.ll}&z=${params.z}&l=stv,sta`);
        await this.browser.waitAndClick(cssSelectors.mapControls.panoramaPhoto.photoSwitcher);
        await this.browser.waitForVisible(cssSelectors.mapControls.panoramaPhoto.photoSwitcherEnabled);
        await this.browser.waitForUrlContains({query: {...params, l: 'pht'}});
    });

    it('Открытие фотогалереи из карточки организации', async function () {
        await openBusinessGallery(this.browser);
        await this.browser.waitAndVerifyScreenshot(cssSelectors.photo.modes.preview, 'business-preview-mode', {
            tolerance: 10
        });
        await this.browser.waitAndClick(cssSelectors.photo.frames.first);
        await this.browser.waitForVisible(cssSelectors.photo.modes.tape);
        await this.browser.waitAndVerifyScreenshot(cssSelectors.photo.modes.tape, 'business-tape-mode');
    });

    it('Открытие фотогалереи из фотослоя', async function () {
        await this.browser.openPage('?ll=37.617110,55.7523&z=16&l=pht');
        await this.browser.waitForVisible(cssSelectors.photo.layer);
        await this.browser.perform(async () => {
            const [x, y] = await this.browser.getMapCenter();
            await this.browser.moveToObject('body', Math.round(x), Math.round(y));
            // Дожидаемся подгрузки хотспотов.
            await this.browser.pause(500);
            await this.browser.simulateClick({x, y, description: ''});
        }, 'Подождать появления хотспота фото и кликнуть в него');
        await this.browser.waitForVisible(cssSelectors.photo.modes.tape);
    });

    it('Закрытие рекламы в фотогалерее', async function () {
        await this.browser.openPage(
            '?mode=search&oid=80343678028&ol=biz&photos[business]=80343678028' +
                '&photos[id]=urn:yandex:sprav:photo:90639543'
        );
        await this.browser.waitAndClick(cssSelectors.advertMock.closeButton);
        await this.browser.waitAndVerifyScreenshot(cssSelectors.photo.modes.tape, 'business-tape-after-closing-ad');
    });

    it('Ссылка на автора фото', async function () {
        await this.browser.openPage('?photos[business]=1124715036&photos[id]=urn%3Ayandex%3Asprav%3Aphoto%3A6928612');
        await this.browser.waitAndVerifyLink(cssSelectors.photo.copyrightLink, 'https://foursquare.com/');
    });

    it('Закрытие фотогалереи крестиком', async function () {
        await openBusinessGallery(this.browser);
        await this.browser.waitAndClick(cssSelectors.photo.closeButton);
        await this.browser.waitForHidden(cssSelectors.photo.player);
        await this.browser.waitForVisible(cssSelectors.search.businessCard.photos);
    });

    it('Закрытие фотогалереи по Esc', async function () {
        await openBusinessGallery(this.browser);
        await this.browser.keys('Escape');
        await this.browser.waitForHidden(cssSelectors.photo.player);
        await this.browser.waitForVisible(cssSelectors.search.businessCard.photos);
    });

    it('Поделиться фотогалереей', async function () {
        const shortLinkPart = encodeURIComponent('/-/');
        await openBusinessGallery(this.browser);
        await this.browser.click(cssSelectors.photo.share.button);
        await this.browser.waitAndVerifyScreenshot(cssSelectors.photo.share.popup, 'share-popup', {keepCursor: true});
        await this.browser.waitAndVerifyLink(cssSelectors.photo.share.socialNetButton, {
            value: shortLinkPart,
            method: 'includes'
        });
    });

    describe('Контрол фидбека в фотогалерее организации', () => {
        beforeEach(async function () {
            await openBusinessGallery(this.browser, true);
        });

        it('Нет в режиме предпросмотра всех фото', async function () {
            await this.browser.waitAndClick(cssSelectors.photo.frames.first);
            await this.browser.waitForHidden(cssSelectors.photo.feedbackPopup);
        });

        it('В режиме просмотра одного фото', async function () {
            await this.browser.waitAndClick(cssSelectors.photo.frames.first);
            await this.browser.waitAndClick(cssSelectors.photo.feebackButton);
            await this.browser.waitAndVerifyScreenshot(cssSelectors.photo.feedbackPopup, 'feedback-popup', {
                keepCursor: true
            });
        });
    });

    it('Переход к другому фото организации по клику в стрелочку', async function () {
        await openBusinessPhoto(this.browser);
        await this.browser.waitAndClick(cssSelectors.photo.arrows.forward);
        await this.browser.waitForVisible(cssSelectors.photo.arrows.forward);
        await this.browser.waitForHidden(cssSelectors.photo.arrows.backwardDisabled);
        await checkForwardPhoto(this.browser);
        await this.browser.waitAndClick(cssSelectors.photo.arrows.backward);
        await this.browser.waitForVisible(cssSelectors.photo.arrows.forward);
        await this.browser.waitForVisible(cssSelectors.photo.arrows.backwardDisabled);
        await checkBackwardPhoto(this.browser);
    });

    it('Переход к другому фото организации по клику в фото', async function () {
        await openBusinessPhoto(this.browser);
        await this.browser.simulateClick({
            x: viewport.width / 2 + 100,
            y: viewport.height / 2,
            description: 'Кликнуть в правую половину открытого плеера фотографий.'
        });
        await this.browser.waitForHidden(cssSelectors.photo.arrows.backwardDisabled);
        await checkForwardPhoto(this.browser);
        await this.browser.simulateClick({
            x: viewport.width / 2 - 100,
            y: viewport.height / 2,
            description: 'Кликнуть в левую половину открытого плеера фотографий.'
        });
        await this.browser.waitForVisible(cssSelectors.photo.arrows.forward);
        await this.browser.waitForVisible(cssSelectors.photo.arrows.backwardDisabled);
        await checkBackwardPhoto(this.browser);
    });

    it('Переход к другому фото организации по стрелке на клавиатуре', async function () {
        await openBusinessPhoto(this.browser);
        await this.browser.keys('Right arrow');
        await this.browser.waitForVisible(cssSelectors.photo.arrows.forward);
        await this.browser.waitForHidden(cssSelectors.photo.arrows.backwardDisabled);
        await checkForwardPhoto(this.browser);
        await this.browser.keys('Left arrow');
        await this.browser.waitForVisible(cssSelectors.photo.arrows.forward);
        await this.browser.waitForVisible(cssSelectors.photo.arrows.backwardDisabled);
        await checkBackwardPhoto(this.browser);
    });

    it('Переход к другому фото организации по нажатию пробела на клавиатуре', async function () {
        await openBusinessPhoto(this.browser);
        await this.browser.keys('Space');
        await this.browser.waitForHidden(cssSelectors.photo.arrows.backwardDisabled);
        await this.browser.waitForVisible(cssSelectors.photo.arrows.forward);
        await checkForwardPhoto(this.browser);
    });

    it('Переход к фотогалереи по нажатию крестика при просмотре фотографии', async function () {
        await openBusinessPhoto(this.browser);
        await this.browser.waitAndClick(cssSelectors.photo.closeButton);
        await this.browser.waitForVisible(cssSelectors.photo.modes.preview);
    });

    it('Переход к фотогалереи по нажатию Esc при просмотре фотографии', async function () {
        await openBusinessPhoto(this.browser);
        await this.browser.keys('Escape');
        await this.browser.waitForVisible(cssSelectors.photo.modes.preview);
    });

    describe('Контролы в фотогалерее', () => {
        it('Общий вид', async function () {
            const url = '?ll=37.612629%2C55.755981&z=18&mode=search&oid=1023322799&ol=biz';
            await this.browser.openPage(url);
            await this.browser.waitAndClick(cssSelectors.search.businessCard.photos);
            await this.browser.waitForVisible(cssSelectors.photo.player);
            await this.browser.waitAndVerifyScreenshot(cssSelectors.photo.controlsView, 'controls-view');
        });

        it('Контрол доставки еды', async function () {
            const url = '?mode=search&oid=1802646377&ol=biz';
            await this.browser.openPage(url);
            await this.browser.waitAndClick(cssSelectors.search.businessCard.photos);
            await this.browser.waitForVisible(cssSelectors.photo.player);
            await this.browser.waitAndVerifyScreenshot(cssSelectors.photo.firstControl, 'delivery-control');
        });
    });
});

async function openBusinessPhoto(browser: WebdriverIO.Browser): Promise<void> {
    await browser.openPage('?oid=1050926298&ol=biz&photos%5Bbusiness%5D=1050926298');
    await browser.waitAndClick(cssSelectors.photo.frames.first);
    await browser.waitForVisible(cssSelectors.photo.modes.tape);
}

async function openBusinessGallery(browser: WebdriverIO.Browser, fakeLogin?: boolean): Promise<void> {
    const url = '?mode=search&oid=31772658117&ol=biz';
    await browser.openPage(url, {fakeLogin});
    await browser.waitAndClick(cssSelectors.search.businessCard.photos);
    await browser.waitForVisible(cssSelectors.photo.player);
}

async function checkForwardPhoto(browser: WebdriverIO.Browser): Promise<void> {
    await browser.perform(async () => {
        const frame = await browser.$(cssSelectors.photo.frames.first);
        const gap = await browser.$(cssSelectors.photo.gap);
        const frameSize = await frame.getSize();
        const gapSize = await gap.getSize();
        const x = await frame.getLocation('x');
        expect(x).be.at.least(-(frameSize.width + gapSize.width));
    }, 'Проверить, что первое фото в галерее находится за пределами видимой области.');
    await browser.perform(async () => {
        const frame = await browser.$(cssSelectors.photo.frames.second);
        const x = await frame.getLocation('x');
        expect(x).to.be.equal(0);
    }, 'Проверить, что второе фото находится в видимой области.');
    await browser.waitAndCheckValue(cssSelectors.photo.counter, '2 из 99');
}

async function checkBackwardPhoto(browser: WebdriverIO.Browser): Promise<void> {
    await browser.perform(async () => {
        const frameFirst = await browser.$(cssSelectors.photo.frames.first);
        const frameSecond = await browser.$(cssSelectors.photo.frames.second);
        const gap = await browser.$(cssSelectors.photo.gap);
        const frameSize = await frameFirst.getSize();
        const gapSize = await gap.getSize();
        const x = await frameSecond.getLocation('x');
        expect(x).to.be.at.most(frameSize.width + gapSize.width);
    }, 'Проверить, что второе фото находится за пределами видимой области справа.');
    await browser.perform(async () => {
        const frame = await browser.$(cssSelectors.photo.frames.first);
        const x = await frame.getLocation('x');
        expect(x).to.be.equal(0);
    }, 'Проверить, что первое фото находится в видимой области.');
    await browser.waitAndCheckValue(cssSelectors.photo.counter, '1 из 99');
}
