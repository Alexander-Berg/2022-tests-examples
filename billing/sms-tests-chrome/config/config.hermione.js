describe('config', () => {
    describe('enable', () => {
        it('Включение конфига', async function () {
            const { browser } = this;
            await browser.ybRun('test_enable_config');
        });
    });
    describe('disable', () => {
        it('Отключение конфига', async function () {
            const { browser } = this;
            await browser.ybRun('test_disable_config');
        });
    });
});
