import Enzyme from 'enzyme';
import Adapter from 'enzyme-adapter-react-16';

import { initializeDesktopRegistry } from 'common/__tests__/registry';
import { ContractType } from 'common/constants/print-form-rules';

import { AttributeComparisionType, PageMode } from '../constants';
import { PrintFormRulePage } from './page';

jest.mock('common/utils/request');

Enzyme.configure({ adapter: new Adapter() });

describe('admin - print-form-rule', () => {
    beforeAll(initializeDesktopRegistry);

    afterEach(() => {
        jest.resetAllMocks();
    });

    test('Отображение карточки правила ПФ', async () => {
        const { attributeReferenceGeneral, publishedPrintFormRule } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [publishedPrintFormRule, attributeReferenceGeneral]
            },
            initialState: {
                page: {
                    pageMode: PageMode.CARD
                },
                ruleCard: {
                    ruleId: 'someExternalId'
                }
            }
        });

        await page.waitForRuleCard();
        expect(page.isRuleCardLoaded()).toBe(true);

        expect(page.request.get).toHaveBeenCalledTimes(2);
        expect(page.request.get).nthCalledWith(1, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(2, attributeReferenceGeneral.request);
    });

    test('Валидация ПФ (пересечений нет)', async () => {
        const {
            attributeReferenceGeneral,
            unpublishedPrintFormRule,
            validateRuleWithoutIntersections
        } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [
                    unpublishedPrintFormRule,
                    attributeReferenceGeneral,
                    validateRuleWithoutIntersections
                ]
            },
            initialState: {
                page: {
                    pageMode: PageMode.CARD
                },
                ruleCard: {
                    ruleId: 'someExternalId'
                }
            }
        });

        await page.waitForRuleCard();
        await page.validateRule();

        expect(page.getIntersections().length).toBe(0);

        expect(page.request.get).toHaveBeenCalledTimes(3);
        expect(page.request.get).nthCalledWith(1, unpublishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(2, attributeReferenceGeneral.request);
        expect(page.request.get).nthCalledWith(3, validateRuleWithoutIntersections.request);
    });

    test('Валидация ПФ (пересечения есть)', async () => {
        const {
            attributeReferenceGeneral,
            unpublishedPrintFormRule,
            validateRuleWithIntersections
        } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [
                    unpublishedPrintFormRule,
                    attributeReferenceGeneral,
                    validateRuleWithIntersections
                ]
            },
            initialState: {
                page: {
                    pageMode: PageMode.CARD
                },
                ruleCard: {
                    ruleId: 'someExternalId'
                }
            }
        });

        await page.waitForRuleCard();
        await page.validateRule();

        expect(page.getIntersections().length).toBe(3);

        expect(page.request.get).toHaveBeenCalledTimes(3);
        expect(page.request.get).nthCalledWith(1, unpublishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(2, attributeReferenceGeneral.request);
        expect(page.request.get).nthCalledWith(3, validateRuleWithIntersections.request);
    });

    test('Публикация правила ПФ (пересечений нет)', async () => {
        const {
            unpublishedPrintFormRule,
            attributeReferenceGeneral,
            publishRuleWithoutIntersections,
            publishedPrintFormRule
        } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [
                    unpublishedPrintFormRule,
                    attributeReferenceGeneral,
                    publishedPrintFormRule,
                    attributeReferenceGeneral
                ],
                requestPost: [publishRuleWithoutIntersections]
            },
            initialState: {
                page: {
                    pageMode: PageMode.CARD
                },
                ruleCard: {
                    ruleId: 'someExternalId'
                }
            }
        });

        await page.waitForRuleCard();

        expect(page.isRulePublished()).toBe(false);

        await page.publishRuleWithoutIntersections();

        expect(page.isRulePublished()).toBe(true);

        expect(page.request.get).toHaveBeenCalledTimes(4);
        expect(page.request.get).nthCalledWith(1, unpublishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(2, attributeReferenceGeneral.request);
        expect(page.request.get).nthCalledWith(3, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(4, attributeReferenceGeneral.request);

        expect(page.request.post).toHaveBeenCalledTimes(1);
        expect(page.request.post).nthCalledWith(1, publishRuleWithoutIntersections.request);
    });

    test('Публикация правила ПФ (пересечения есть)', async () => {
        const {
            unpublishedPrintFormRule,
            attributeReferenceGeneral,
            publishRuleWithIntersections,
            publishRuleWithForce,
            publishedPrintFormRule
        } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [
                    unpublishedPrintFormRule,
                    attributeReferenceGeneral,
                    publishedPrintFormRule,
                    attributeReferenceGeneral
                ],
                requestPost: [publishRuleWithIntersections, publishRuleWithForce]
            },
            initialState: {
                page: {
                    pageMode: PageMode.CARD
                },
                ruleCard: {
                    ruleId: 'someExternalId'
                }
            }
        });

        await page.waitForRuleCard();

        expect(page.isRulePublished()).toBe(false);

        await page.publishRuleWithIntersections();

        expect(page.getIntersections().length).toBe(3);

        await page.confirmIntersections();

        expect(page.isRulePublished()).toBe(true);

        expect(page.request.get).toHaveBeenCalledTimes(4);
        expect(page.request.get).nthCalledWith(1, unpublishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(2, attributeReferenceGeneral.request);
        expect(page.request.get).nthCalledWith(3, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(4, attributeReferenceGeneral.request);

        expect(page.request.post).toHaveBeenCalledTimes(2);
        expect(page.request.post).nthCalledWith(1, publishRuleWithIntersections.request);
        expect(page.request.post).nthCalledWith(2, publishRuleWithForce.request);
    });

    test('Переключение на форму правила ПФ', async () => {
        const { attributeReferenceGeneral, publishedPrintFormRule } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [
                    publishedPrintFormRule,
                    attributeReferenceGeneral,
                    publishedPrintFormRule,
                    attributeReferenceGeneral
                ]
            },
            initialState: {
                page: {
                    pageMode: PageMode.CARD
                },
                ruleCard: {
                    ruleId: 'someExternalId'
                },
                ruleForm: {
                    ruleId: 'someExternalId'
                }
            }
        });

        await page.waitForRuleCard();

        await page.goToRuleForm();

        expect(page.isRuleFormLoaded()).toBe(true);

        expect(page.request.get).toHaveBeenCalledTimes(4);
        expect(page.request.get).nthCalledWith(1, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(2, attributeReferenceGeneral.request);
        expect(page.request.get).nthCalledWith(3, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(4, attributeReferenceGeneral.request);
    });

    test('Отображение формы существующего правила ПФ', async () => {
        const { attributeReferenceGeneral, publishedPrintFormRule } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [publishedPrintFormRule, attributeReferenceGeneral]
            },
            initialState: {
                page: {
                    pageMode: PageMode.FORM
                },
                ruleForm: {
                    ruleId: 'someExternalId'
                }
            }
        });

        await page.waitForRuleForm();

        expect(page.isRuleFormLoaded()).toBe(true);

        expect(page.request.get).toHaveBeenCalledTimes(2);
        expect(page.request.get).nthCalledWith(1, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(2, attributeReferenceGeneral.request);
    });

    test('Отображение формы нового правила ПФ', async () => {
        const { attributeReferenceGeneral } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [attributeReferenceGeneral]
            },
            initialState: {
                page: {
                    pageMode: PageMode.FORM
                }
            }
        });

        await page.waitForRuleForm();

        expect(page.isRuleFormLoaded()).toBe(true);

        expect(page.request.get).toHaveBeenCalledTimes(1);
        expect(page.request.get).nthCalledWith(1, attributeReferenceGeneral.request);
    });

    test('Переключение Вида Договора на форме нового ПФ', async () => {
        const { attributeReferenceGeneral, attributeReferenceDistribution } = await import(
            './mocks'
        );

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [attributeReferenceGeneral, attributeReferenceDistribution]
            },
            initialState: {
                page: {
                    pageMode: PageMode.FORM
                }
            }
        });

        await page.waitForRuleForm();

        expect(page.getContractType()).toBe(ContractType.GENERAL);

        await page.changeContractType(ContractType.DISTRIBUTION);

        expect(page.getContractType()).toBe(ContractType.DISTRIBUTION);

        expect(page.request.get).toHaveBeenCalledTimes(2);
        expect(page.request.get).nthCalledWith(1, attributeReferenceGeneral.request);
        expect(page.request.get).nthCalledWith(2, attributeReferenceDistribution.request);
    });

    test('Переключение Вида Договора на форме существующего правила ПФ', async () => {
        const {
            publishedPrintFormRule,
            attributeReferenceGeneral,
            attributeReferenceDistribution
        } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [
                    publishedPrintFormRule,
                    attributeReferenceGeneral,
                    attributeReferenceDistribution
                ]
            },
            initialState: {
                page: {
                    pageMode: PageMode.FORM
                },
                ruleForm: {
                    ruleId: 'someExternalId'
                }
            }
        });

        await page.waitForRuleForm();

        expect(page.getContractType()).toBe(ContractType.GENERAL);

        await page.changeContractTypeWithAcceptance(ContractType.DISTRIBUTION);

        expect(page.getContractType()).toBe(ContractType.DISTRIBUTION);

        expect(page.request.get).toHaveBeenCalledTimes(3);
        expect(page.request.get).nthCalledWith(1, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(2, attributeReferenceGeneral.request);
        expect(page.request.get).nthCalledWith(3, attributeReferenceDistribution.request);
    });

    test('Создание правила (есть ошибки)', async () => {
        const { attributeReferenceGeneral } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [attributeReferenceGeneral]
            },
            initialState: {
                page: {
                    pageMode: PageMode.FORM
                }
            }
        });

        await page.waitForRuleForm();

        await page.tryToSaveRule();

        expect(page.getValidationErrors().length).toBe(4);

        expect(page.request.get).toHaveBeenCalledTimes(1);
        expect(page.request.get).nthCalledWith(1, attributeReferenceGeneral.request);
    });

    test('Создание правила (без ошибок)', async () => {
        const { attributeReferenceGeneral, createRuleWithoutIntersections } = await import(
            './mocks'
        );

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [attributeReferenceGeneral],
                requestPost: [createRuleWithoutIntersections]
            },
            mockWindowLocation: true,
            initialState: {
                page: {
                    pageMode: PageMode.FORM
                }
            }
        });

        await page.waitForRuleForm();

        await page.fillRuleAttributes({
            externalId: '__automatic_rtrunk_tplno946_pfno953_GENERAL',
            caption: 'edinyij dogovor postoplata',
            typeId: 'contract'
        });
        await page.addRuleLink({
            name: 'Единый договор (Постоплата)',
            value: 'https://wiki.yandex-team.ru/sales/processing/edinyjj-dogovor/'
        });
        await page.addRuleBlock();
        await page.addRuleElement({
            contextId: 'editable',
            attributeId: 'CREDIT_TYPE',
            attributeComparisionType: AttributeComparisionType.SINGLE,
            value: '0'
        });

        await page.saveNewRule();

        expect(window.location.href).toBe(
            '/print-form-rule.xml?rule_id=__automatic_rtrunk_tplno946_pfno953_GENERAL'
        );

        expect(page.request.get).toHaveBeenCalledTimes(1);
        expect(page.request.get).nthCalledWith(1, attributeReferenceGeneral.request);

        expect(page.request.post).toHaveBeenCalledTimes(1);
        expect(page.request.post).nthCalledWith(1, createRuleWithoutIntersections.request);
    });

    test('Сохранение существующего правила (есть пересечения)', async () => {
        const {
            attributeReferenceGeneral,
            publishedPrintFormRule,
            createRuleWithIntersections,
            createRuleWithForce
        } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [
                    publishedPrintFormRule,
                    attributeReferenceGeneral,
                    publishedPrintFormRule,
                    attributeReferenceGeneral
                ],
                requestPost: [createRuleWithIntersections, createRuleWithForce]
            },
            initialState: {
                page: {
                    pageMode: PageMode.FORM
                },
                ruleCard: {
                    ruleId: 'someExternalId'
                },
                ruleForm: {
                    ruleId: 'someExternalId'
                }
            }
        });

        await page.waitForRuleForm();

        await page.saveRuleWithIntersections();

        expect(page.getIntersections().length).toBe(3);

        await page.confirmIntersections();

        expect(page.isRuleCardLoaded()).toBe(true);

        expect(page.request.get).toHaveBeenCalledTimes(4);
        expect(page.request.get).nthCalledWith(1, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(2, attributeReferenceGeneral.request);
        expect(page.request.get).nthCalledWith(3, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(4, attributeReferenceGeneral.request);

        expect(page.request.post).toHaveBeenCalledTimes(2);
        expect(page.request.post).nthCalledWith(1, createRuleWithIntersections.request);
        expect(page.request.post).nthCalledWith(2, createRuleWithForce.request);
    });

    test('Сохранение существующего правила (нет пересечений)', async () => {
        const {
            attributeReferenceGeneral,
            publishedPrintFormRule,
            updateRuleWithoutIntersections
        } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [
                    publishedPrintFormRule,
                    attributeReferenceGeneral,
                    publishedPrintFormRule,
                    attributeReferenceGeneral
                ],
                requestPost: [updateRuleWithoutIntersections]
            },
            initialState: {
                page: {
                    pageMode: PageMode.FORM
                },
                ruleCard: {
                    ruleId: 'someExternalId'
                },
                ruleForm: {
                    ruleId: 'someExternalId'
                }
            }
        });

        await page.waitForRuleForm();

        await page.saveRuleWithoutIntersections();

        expect(page.isRuleCardLoaded()).toBe(true);

        expect(page.request.get).toHaveBeenCalledTimes(4);
        expect(page.request.get).nthCalledWith(1, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(2, attributeReferenceGeneral.request);
        expect(page.request.get).nthCalledWith(3, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(4, attributeReferenceGeneral.request);

        expect(page.request.post).toHaveBeenCalledTimes(1);
        expect(page.request.post).nthCalledWith(1, updateRuleWithoutIntersections.request);
    });

    test('Переключение на карточку правила ПФ', async () => {
        const { attributeReferenceGeneral, publishedPrintFormRule } = await import('./mocks');

        const page = new PrintFormRulePage({
            mocks: {
                requestGet: [
                    publishedPrintFormRule,
                    attributeReferenceGeneral,
                    publishedPrintFormRule,
                    attributeReferenceGeneral
                ]
            },
            initialState: {
                page: {
                    pageMode: PageMode.FORM
                },
                ruleCard: {
                    ruleId: 'someExternalId'
                },
                ruleForm: {
                    ruleId: 'someExternalId'
                }
            }
        });

        await page.waitForRuleForm();

        await page.goToRuleCard();

        expect(page.isRuleCardLoaded()).toBe(true);

        expect(page.request.get).toHaveBeenCalledTimes(4);
        expect(page.request.get).nthCalledWith(1, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(2, attributeReferenceGeneral.request);
        expect(page.request.get).nthCalledWith(3, publishedPrintFormRule.request);
        expect(page.request.get).nthCalledWith(4, attributeReferenceGeneral.request);
    });
});
