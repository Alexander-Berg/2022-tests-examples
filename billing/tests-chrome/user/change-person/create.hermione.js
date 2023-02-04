describe('user', () => {
    describe('change-person', () => {
        describe('create', async function () {
            it('ph проверяет создание плательщика физлица [smoke]', async function () {
                const { browser } = this;

                const { login } = await browser.ybSignIn();
                await browser.ybRun('create_client_for_user', [login]);

                // Заполняем форму создания плательщика
                await browser.ybUrl('user', 'persons.xml');

                await browser.ybSetSteps(`Нажимает создать плательщика`);

                await browser.waitForVisible('form[action="change-person.xml"] #type');
                await browser.click('form[action="change-person.xml"] #type');
                await browser.selectByValue('form[action="change-person.xml"] #type', 'ph');
                await browser.click('form[action="change-person.xml"] #type');
                // FF bug workaround  *[type="submit"]
                await browser.click('form[action="change-person.xml"] *[type="submit"]');

                await browser.waitForVisible('form[action="ctl-change-person.xml"]');
                await browser.ybAssertView(
                    'create ph clear',
                    'form[action="ctl-change-person.xml"]'
                );

                await browser.ybSetSteps(`Заполняет поля`);

                // FF bug workaround  body #id
                await browser.setValue('body #lname', 'Иванов');
                await browser.setValue('body #fname', 'Иван');
                await browser.setValue('body #mname', 'Иванович');
                await browser.setValue('body #phone', '+7 495 123-45-67');
                await browser.setValue('body #fax', '+7 495 123-45-67');
                await browser.setValue('body #email', 'ivanov@yandex.ru');
                await browser.setValue('body #postcode', '119021');
                await browser.setValue('body #city', 'Москва');
                await browser.setValue('body #postaddress', 'ул. Льва Толстого, д. 16');
                await browser.setValue('body #bik', '044030653');
                await browser.setValue('body #account', '40817810455000000131');
                await browser.setValue('body #corraccount', '00000000000000000000');
                await browser.setValue('body #bank', 'Банк');
                await browser.setValue('body #bankcity', 'ул. Льва Толстого, д. 16');
                await browser.click('body #country-id');
                await browser.selectByValue('body #country-id', '225');
                await browser.click('body #agree');
                await browser.click('.b-note'); // Выходим из окна выбора

                await browser.ybAssertView(
                    'create ph filled',
                    'form[action="ctl-change-person.xml"]'
                );

                await browser.ybSetSteps(`Отправляет`);

                await browser.click('form[action="ctl-change-person.xml"] *[type="submit"]');
                await browser.waitForVisible('.l-page');
                await browser.ybAssertView('create ph person', '.l-page', {
                    hideElements: ['#type']
                });
            });
        });
    });
});
