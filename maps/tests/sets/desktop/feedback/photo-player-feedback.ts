import cssSelectors from '../../../common/css-selectors';

describe('Фидбек фотогалереи организации', () => {
    describe('без залогина', () => {
        it('открывает домик', async function () {
            await openPhotoPlayer(this.browser);
            await this.browser.waitAndClick(cssSelectors.feedback.photo.button);
            await this.browser.waitForVisible(cssSelectors.loginDialog.view);
        });
    });

    describe('с залогином', () => {
        beforeEach(async function () {
            return openPhotoPlayer(this.browser, {fakeLogin: true});
        });

        /**
         * @see https://testpalm.yandex-team.ru/testcase/maps-168
         */
        it('открывает попап', async function () {
            await this.browser.waitAndClick(cssSelectors.feedback.photo.button);
            await this.browser.waitForVisible(cssSelectors.feedback.photo.popup);
            await this.browser.waitForVisible(cssSelectors.feedback.photo.elements.badQuality);
            await this.browser.waitForVisible(cssSelectors.feedback.photo.elements.irrelevant);
        });

        const cases = [
            {
                title: 'Неприемлемое содержание',
                selector: cssSelectors.feedback.photo.elements.badQuality
            },
            {
                title: 'На фото не эта организация',
                selector: cssSelectors.feedback.photo.elements.irrelevant
            }
        ];

        cases.forEach((testcase) => {
            // eslint-disable-next-line jest/valid-describe
            describe('диалог "' + testcase.title + '"', () => {
                async function closeDialog(browser: WebdriverIO.Browser) {
                    await browser.waitAndClick(cssSelectors.feedback.main.dialog.closeButton);
                    await browser.waitForHidden(cssSelectors.feedback.main.dialog.dialog);
                }

                beforeEach(async function () {
                    await this.browser.waitAndClick(cssSelectors.feedback.photo.button);
                    await this.browser.waitAndClick(testcase.selector);
                    await this.browser.waitForVisible(cssSelectors.feedback.main.dialog.dialog);
                });

                it('закрывается по клику в крестик', async function () {
                    await closeDialog(this.browser);
                });

                it('закрывается по клику в отмену', async function () {
                    await this.browser.waitAndClick(cssSelectors.feedback.form.buttons.cancel);
                    await this.browser.waitForHidden(cssSelectors.feedback.main.dialog.dialog);
                });

                describe('c закрытием в конце', () => {
                    // Закрытие в крестик после каждого теста,
                    // чтобы выход пользователя работал корректно.
                    afterEach(async function () {
                        await closeDialog(this.browser);
                    });

                    it('открывается', async function () {
                        await this.browser.waitForVisible(cssSelectors.feedback.main.dialog.closeButton);
                        await this.browser.waitForVisible(cssSelectors.feedback.form.comment);
                        await this.browser.waitForVisible(cssSelectors.feedback.form.buttons.cancel);
                        await this.browser.waitForVisible(cssSelectors.feedback.form.buttons.submit.disabled);
                        await this.browser.waitAndCheckValue(cssSelectors.feedback.main.dialog.title, testcase.title);
                    });

                    it('ввод комментария активирует кнопку отправки', async function () {
                        const comment = 'my comment';
                        await this.browser.setValueToInput(cssSelectors.feedback.form.comment, comment);
                        await this.browser.waitAndCheckValue(cssSelectors.feedback.form.comment, comment);
                        await this.browser.waitForVisible(cssSelectors.feedback.form.buttons.submit.enabled);
                    });

                    // TODO: проверить отправку.
                    // @see https://st.yandex-team.ru/MAPSUI-3770
                });
            });
        });
    });
});

async function openPhotoPlayer(browser: WebdriverIO.Browser, {fakeLogin = false} = {}): Promise<void> {
    const link = '?ol=biz&oid=1022264035';

    await browser.openPage(link, {fakeLogin});
    await browser.waitAndClick(cssSelectors.search.businessCard.photos);
    await browser.waitForVisible(cssSelectors.photo.player);
    await browser.waitAndClick(cssSelectors.photo.frames.first);
}
