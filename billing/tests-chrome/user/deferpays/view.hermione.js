const { hideElements } = require('./helpers');

describe('user', () => {
    describe('deferpays', () => {
        it('нет кредитов', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yb-static-balance-6' });

            await browser.ybUrl('user', `deferpays.xml`);
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]');

            await browser.ybAssertView('просмотр страницы кредитов, нет кредитов', '.content');
        });

        it('есть задолженность', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yb-static-deferpays-5' });

            await browser.ybUrl('user', `deferpays.xml`);
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]');
            await browser.click('.blc_credit_information a');

            await browser.ybAssertView(
                'просмотр страницы кредитов, есть задолженность',
                '.content'
            );
        });

        it('нерезы и субклиенты', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yb-static-deferpays-2' });

            await browser.ybUrl('user', `deferpays.xml`);
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]');
            await browser.click('.blc_credit_information a:nth-child(1)');
            await browser.click('.blc_credit_information a:nth-child(2)');

            await browser.ybAssertView(
                'просмотр страницы кредитов, нерезы и субклиенты',
                '.content'
            );
        });

        it('недействующие договоры', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yb-static-deferpays-1' });

            await browser.ybUrl('user', `deferpays.xml`);
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]');
            await browser.click('.blc_credit_information a:nth-child(1)');
            await browser.click('.blc_credit_information a:nth-child(2)');

            await browser.ybAssertView(
                'просмотр страницы кредитов, недействующие договоры',
                '.content'
            );
        });

        it('фиктивная схема, пагинация', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yb-static-deferpays-4' });

            await browser.ybUrl('user', `deferpays.xml`);
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]');

            await browser.ybAssertView('просмотр страницы кредитов, фиктивная схема', '.content', {
                hideElements
            });

            await browser.click('.pages .next');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]');
            await browser.ybAssertView(
                'просмотр страницы кредитов, 2 страница',
                '#repayment-form',
                { hideElements }
            );

            await browser.selectByValue('select[name="ps"]', '20');
            await browser.click('input[value="Вывести"]');
            await browser.ybWaitForInvisible('img[alt="Waiting for data"]');
            await browser.ybAssertView(
                'просмотр страницы кредитов, 20 элементов',
                '#repayment-form',
                { hideElements }
            );
        });
    });
});
