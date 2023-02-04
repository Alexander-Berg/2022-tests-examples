describe('user', () => {
    describe('representatives', () => {
        it('страница представителей', async function () {
            const { browser } = this;

            await browser.ybSignIn({ login: 'yb-static-representative' });

            await browser.ybUrl('user', `representatives.xml`);
            await browser.waitForVisible('.yb-user-content');
            await browser.ybWaitForInvisible('.Spin2');

            await browser.ybAssertView('просмотр страницы представителей', '.yb-user-content');
        });
    });
});
