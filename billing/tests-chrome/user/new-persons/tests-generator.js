const {
    openPersonCreation,
    assertMandatoryFieldsValidation,
    setValues,
    assertMandatoryFields,
    assertAllFields,
    save,
    assertMandatoryCard,
    assertAllCard,
    openPersonCard,
    assertCardBeforeEdit,
    clickEdit,
    assertFormBeforeEdit,
    assertFormAfterEdit,
    assertCardAfterEdit
} = require('./helpers');

module.exports = function ({ partner, personType, details, isUr }) {
    describe(`${personType}_${partner}`, () => {
        describe('создание', () => {
            describe('клиент', () => {
                it('создание с обязательными полями', async function () {
                    const { browser } = this;

                    await openPersonCreation(browser, { personType, isUr });
                    await assertMandatoryFieldsValidation(browser, { personType });
                    await setValues(
                        browser,
                        details.filter(detail => detail.isMandatory === true)
                    );
                    await assertMandatoryFields(browser, { personType });
                    await save(browser);
                    await assertMandatoryCard(browser, { personType });
                });

                it('создание со всеми полями', async function () {
                    const { browser } = this;

                    await openPersonCreation(browser, { personType, isUr });
                    await setValues(browser, details);
                    await assertAllFields(browser, { personType });
                    await save(browser);
                    await assertAllCard(browser, { personType });
                });
            });
        });
        describe('редактирование', () => {
            describe('клиент', () => {
                it('редактирование всех доступных полей', async function () {
                    const { browser } = this;

                    await openPersonCard(browser, { personType });
                    await assertCardBeforeEdit(browser, { personType });
                    await clickEdit(browser);
                    await assertFormBeforeEdit(browser, { personType });
                    await setValues(
                        browser,
                        details.filter(detail => detail.newValue !== undefined),
                        { edit: true }
                    );
                    await assertFormAfterEdit(browser, { personType });
                    await save(browser);
                    await assertCardAfterEdit(browser, { personType });
                });
            });
        });
    });
};
