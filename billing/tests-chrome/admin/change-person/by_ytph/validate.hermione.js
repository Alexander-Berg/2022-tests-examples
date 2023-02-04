const { partner, personType } = require('./common');
const { directOpenPersonCreation, assertDetailsValidation } = require('../helpers');

describe('admin', () => {
    describe('change-person', () => {
        describe(personType, () => {
            describe('валидация', () => {
                it('обязательные поля', async function () {
                    const { browser } = this;

                    await directOpenPersonCreation(browser, { personType, partner });
                    await assertDetailsValidation(browser);
                });
            });
        });
    });
});
