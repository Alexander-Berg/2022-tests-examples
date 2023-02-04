const { elements } = require('./elements');

describe('user', () => {
    describe('new-persons', () => {
        describe('page', () => {
            it('Нет плательщиков - недостуность фильтров', async function () {
                const { browser } = this;
                const { login } = await browser.ybSignIn({ isAdmin: false });
                await browser.ybRun('create_client_for_user', {
                    login
                });

                await browser.ybUrl('user', `new-persons.xml`);

                await browser.waitForVisible(elements.addEmptyPersons);

                await browser.ybAssertView(
                    `new-persons, страница добавления плательщика. Фильтры недоступны`,
                    elements.page
                );
            });
            it('Есть плательщики ur и ph, партнерские и непартнерские - все+обычные, все+партнерские', async function () {
                const { browser } = this;
                const { login } = await browser.ybSignIn({ isAdmin: false });
                await browser.ybRun('test_ur_and_ph_partner_and_not_partner_ci', {
                    login
                });

                await browser.ybUrl('user', `new-persons.xml`);

                await browser.ybWaitForLoad();

                await browser.ybAssertView(`new-persons, Все плательщики обычные`, elements.page);

                await browser.click('[value="partner"]');

                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    `new-persons, Все плательщики партнерские`,
                    elements.page
                );
            });
            it('Есть разные плательщики, партнерские и непартнерские - ФЛ+обычные, ЮЛ+партнерские', async function () {
                const { browser } = this;
                const { login } = await browser.ybSignIn({ isAdmin: false });
                await browser.ybRun('test_various_partner_and_not_partner_ci', {
                    login
                });

                await browser.ybUrl('user', `new-persons.xml`);

                await browser.ybWaitForLoad();

                await browser.click('[value="ph"]');

                await browser.ybWaitForLoad();

                await browser.ybAssertView(`new-persons, ФЛ плательщики обычные`, elements.page);

                await browser.click('[value="ur"]');

                await browser.click('[value="partner"]');

                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    `new-persons, ЮЛ плательщики партнерские`,
                    elements.page
                );
            });
            it('Только заархивированные плательщики - обычные+в архиве, партнерские + в архиве', async function () {
                const { browser } = this;
                const { login } = await browser.ybSignIn({ isAdmin: false });
                await browser.ybRun('test_hidden_persons_ci', {
                    login
                });

                await browser.ybUrl('user', `new-persons.xml`);

                await browser.ybWaitForLoad();

                await browser.click('[value="archived"]');

                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    `new-persons, Обычные плательщики в архиве`,
                    elements.page
                );

                await browser.click('[value="partner"]');

                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    `new-persons, Партенские плательщики в архиве`,
                    elements.page
                );
            });
            it('Много плательщиков - доскроллить вниз, дождаться загрузки', async function () {
                const { browser } = this;
                const { login } = await browser.ybSignIn({ isAdmin: false });

                await browser.ybRun('test_many_persons_ci', {
                    login
                });

                await browser.ybUrl('user', `new-persons.xml`);

                await browser.ybWaitForLoad();

                await browser.ybAssertView(
                    `new-persons, Много пользователей часть 1`,
                    elements.page
                );

                await browser.scroll('.yb-persons__table-row:nth-child(30)');

                await browser.ybAssertView(
                    `new-persons, Много пользователей часть 2`,
                    elements.page
                );
            });
            it('Переход на страницу плательщика по клику на строку', async function () {
                const { browser } = this;
                const { login } = await browser.ybSignIn({ isAdmin: false });
                await browser.ybRun('test_ur_and_ph_partner_and_not_partner_ci', {
                    login
                });

                await browser.ybUrl('user', `new-persons.xml`);

                await browser.ybWaitForLoad();

                await browser.click('.yb-persons__table-row');

                await browser.waitForVisible('.yb-person-detail_email');

                await browser.ybAssertView(
                    `new-persons, Карточка плательщика, переход через клик`,
                    elements.page
                );
            });
            it('Переход на страницу редактирования плательщика по клику на карандашик ', async function () {
                const { browser } = this;
                const { login } = await browser.ybSignIn({ isAdmin: false });
                await browser.ybRun('test_ur_and_ph_partner_and_not_partner_ci', {
                    login
                });

                await browser.ybUrl('user', `new-persons.xml`);

                await browser.ybWaitForLoad();

                // костыльное решение для нажатия на карандашик, потому что
                // Webdriver соверешнно ни в какую не хочет кликать на икноку карандашика,
                // потому что его не видно, пока не наведешь мышку,
                // функция moveToObject у меня не работала, как и moveTo
                await browser.execute(() => {
                    document.querySelector('.yb-persons__edit-icon').click();
                });

                await browser.waitForVisible(elements.submitButton);

                await browser.ybAssertView(
                    `new-persons, Редактироваение  плательщика, переход через карандашик`,
                    elements.page
                );
            });

            it('new-persons, предупреждения о входе под логином без клиента', async function () {
                const { browser } = this;
                await browser.ybSignIn({ login: 'yb-static-balance-5' });
                await browser.ybUrl('user', `new-persons.xml`);
                await browser.waitForVisible('.yb-notification_type_error');
            });
        });
    });
});
