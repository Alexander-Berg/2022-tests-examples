import * as authKeyset from '../../../../src/translations/auth';
import selectors from '../../../src/constants/selectors';
import { getRandomInt } from '../../../src/utils/random';
import { getCompany, getAccountInfo, companyId } from '../../fixtures/company-create-response';

const REQUIRED_FIELDS = [
  'company-name',
  'name',
  'position',
  'phone',
  'email',
  'vehicle-park',
  'experience',
];

const ERRORS = {
  requiredField: authKeyset.ru.fieldsValidations_isRequired,
  phoneInvalid: authKeyset.ru.fieldsValidations_isNotPhone,
  emailInvalid: authKeyset.ru.fieldsValidations_isNotEmail,
};

context('Create company', function () {
  beforeEach(function () {
    cy.preserveCookies();
  });

  before(function () {
    cy.yandexLogin('temp');
  });

  it('Suggest to create company', function () {
    cy.get(selectors.noLogin.newCompany).should('contain', authKeyset.ru.createCompany_title);
  });

  it('Remove name, phone, acceptance', () => {
    cy.get(selectors.noLogin.registerForm.input('name')).clear();
    cy.get(selectors.noLogin.registerForm.input('phone')).clear();
    cy.get<HTMLInputElement>(selectors.noLogin.registerForm.termsOfUseCheckbox).then($el => {
      if ($el[0].checked) {
        // didn't use cypress "uncheck", because that method don't test click at label scenario
        cy.get(selectors.noLogin.registerForm.termsOfUseLabel).click();
      }
    });

    cy.get(selectors.noLogin.registerForm.input('name')).should('have.value', '');
    cy.get(selectors.noLogin.registerForm.input('phone')).should('have.value', '');
    cy.get(selectors.noLogin.registerForm.termsOfUseCheckbox).should('not.be.checked');
  });

  it('Try to create with unfilled form', () => {
    cy.get(selectors.noLogin.registerForm.submit).click();

    REQUIRED_FIELDS.forEach(field => {
      cy.get(selectors.noLogin.registerForm.field(field))
        .find(selectors.noLogin.registerForm.error)
        .should('be.visible')
        .should('have.text', ERRORS.requiredField);
    });
    cy.get(selectors.noLogin.registerForm.termsOfUse).find(selectors.noLogin.registerForm.error);
    cy.get(selectors.noLogin.registerForm.submit).should('have.attr', 'disabled');
  });

  it('Fill fields', () => {
    cy.fixture('testData').then(({ accounts }) => {
      cy.get(selectors.noLogin.registerForm.input('company-name')).type(
        `YTEST-${getRandomInt(100, 1000)}`,
      );

      cy.get(selectors.noLogin.registerForm.input('name')).type('Тест');
      cy.get(selectors.noLogin.registerForm.input('phone')).type('+70000000000');
      cy.get(selectors.noLogin.registerForm.input('email')).type(`${accounts.temp}@yandex.ru`);
      cy.get(selectors.noLogin.registerForm.select('position')).click();
      cy.focused().type('{enter}', { force: true });
      cy.get(selectors.noLogin.registerForm.select('vehicle-park')).click();
      cy.focused().type('{enter}', { force: true });
      cy.get(selectors.noLogin.registerForm.select('experience')).click();
      cy.focused().type('{enter}', { force: true });
      cy.get(selectors.noLogin.registerForm.termsOfUseLabel).click();

      REQUIRED_FIELDS.forEach(field => {
        cy.get(selectors.noLogin.registerForm.field(field))
          .find(selectors.noLogin.registerForm.error)
          .should('not.exist');
      });
      cy.get(selectors.noLogin.registerForm.termsOfUse)
        .find(selectors.noLogin.registerForm.error)
        .should('not.exist');
      cy.get(selectors.noLogin.registerForm.submit).should('have.not.attr', 'disabled');
    });
  });

  it('Terms is not accepted', () => {
    cy.get<HTMLInputElement>(selectors.noLogin.registerForm.termsOfUseCheckbox).then($el => {
      if ($el[0].checked) {
        // didn't use cypress "uncheck", because that method don't test click at label scenario
        cy.get(selectors.noLogin.registerForm.termsOfUseLabel).click();
      }
    });
    cy.get(selectors.noLogin.registerForm.submit).click();

    cy.get(selectors.noLogin.registerForm.submit).should('have.attr', 'disabled');
    cy.get(selectors.noLogin.registerForm.termsOfUse)
      .find(selectors.noLogin.registerForm.error)
      .should('be.visible');
  });

  it('Terms is accepted', () => {
    cy.get(selectors.noLogin.registerForm.termsOfUseLabel).click();

    cy.get(selectors.noLogin.registerForm.submit).should('have.not.attr', 'disabled');
    cy.get(selectors.noLogin.registerForm.termsOfUse)
      .find(selectors.noLogin.registerForm.error)
      .should('not.exist');
  });

  it('Company name is emptied', () => {
    cy.get(selectors.noLogin.registerForm.input('company-name')).clear();
    cy.get(selectors.noLogin.registerForm.submit).click();

    cy.get(selectors.noLogin.registerForm.submit).should('have.attr', 'disabled');
    cy.get(selectors.noLogin.registerForm.field('company-name'))
      .find(selectors.noLogin.registerForm.error)
      .should('be.visible');
  });

  it('Company name is filled', () => {
    cy.get(selectors.noLogin.registerForm.input('company-name')).type(
      `YTEST-${getRandomInt(100, 1000)}`,
    );

    cy.get(selectors.noLogin.registerForm.submit).should('have.not.attr', 'disabled');
    cy.get(selectors.noLogin.registerForm.field('company-name'))
      .find(selectors.noLogin.registerForm.error)
      .should('not.exist');
  });

  it('Phone name is emptied', () => {
    cy.get(selectors.noLogin.registerForm.input('phone')).clear();
    cy.get(selectors.noLogin.registerForm.submit).click();

    cy.get(selectors.noLogin.registerForm.submit).should('have.attr', 'disabled');
    cy.get(selectors.noLogin.registerForm.field('phone'))
      .find(selectors.noLogin.registerForm.error)
      .should('be.visible');
  });

  it('Phone is invalid', () => {
    cy.get(selectors.noLogin.registerForm.input('phone')).type('+70000000000000');
    cy.get(selectors.noLogin.registerForm.submit).click();

    cy.get(selectors.noLogin.registerForm.submit).should('have.attr', 'disabled');
    cy.get(selectors.noLogin.registerForm.field('phone'))
      .find(selectors.noLogin.registerForm.error)
      .should('be.visible');
    cy.get(selectors.noLogin.registerForm.field('phone'))
      .find(selectors.noLogin.registerForm.error)
      .should('have.text', ERRORS.phoneInvalid);
  });

  it('Phone is valid', () => {
    cy.get(selectors.noLogin.registerForm.input('phone')).clear();
    cy.get(selectors.noLogin.registerForm.input('phone')).type('+70000000000');

    cy.get(selectors.noLogin.registerForm.submit).should('have.not.attr', 'disabled');
    cy.get(selectors.noLogin.registerForm.field('phone'))
      .find(selectors.noLogin.registerForm.error)
      .should('not.exist');
  });

  it('Email is emptied', () => {
    cy.get(selectors.noLogin.registerForm.input('email')).clear();
    cy.get(selectors.noLogin.registerForm.submit).click();

    cy.get(selectors.noLogin.registerForm.submit).should('have.attr', 'disabled');
    cy.get(selectors.noLogin.registerForm.field('email'))
      .find(selectors.noLogin.registerForm.error)
      .should('be.visible');
    cy.get(selectors.noLogin.registerForm.field('email'))
      .find(selectors.noLogin.registerForm.error)
      .should('have.text', ERRORS.requiredField);
  });

  it('Email is invalid', () => {
    cy.fixture('testData').then(({ accounts }) => {
      cy.get(selectors.noLogin.registerForm.input('email')).type(accounts.temp);
      cy.get(selectors.noLogin.registerForm.submit).click();

      cy.get(selectors.noLogin.registerForm.field('email'))
        .find(selectors.noLogin.registerForm.error)
        .should('be.visible');
      cy.get(selectors.noLogin.registerForm.field('email'))
        .find(selectors.noLogin.registerForm.error)
        .should('have.text', ERRORS.emailInvalid);
    });
  });

  it('Create company with valid data', () => {
    cy.fixture('testData').then(({ accounts }) => {
      cy.get(selectors.noLogin.registerForm.input('email')).clear();
      cy.get(selectors.noLogin.registerForm.input('email')).type(`${accounts.temp}@yandex.ru`);

      cy.intercept(
        {
          pathname: '**/create-company',
        },
        req => {
          req.reply(200, getCompany(req.body.name), { 'Content-type': 'application/json' });
          req.body = {};
        },
      );

      cy.intercept(
        {
          pathname: '**/current_user',
        },
        req => {
          req.continue(res => {
            const data = getCompany('fake');
            const crDate = new Date().toISOString();

            res.body = {
              ...res.body,
              role: 'admin',
              confirmed_at: crDate,
              id: 593191,
              login: res.body.passportUser.displayName,
              is_super: false,
              company_users: [
                {
                  company_id: data.id,
                  company_name: data.name,
                  created_at: crDate,
                  role: 'admin',
                },
              ],
            };
            res.send();
          });
        },
      );

      cy.intercept(
        {
          pathname: `**/companies/${companyId}`,
        },
        req => {
          req.reply(200, getCompany('YTEST-fake'), { 'Content-type': 'application/json' });
        },
      );

      cy.intercept(
        {
          pathname: `**/companies/${companyId}/route-events`,
        },
        req => {
          req.reply(200, [], { 'Content-type': 'application/json' });
        },
      );

      cy.intercept(
        {
          pathname: `**/companies/${companyId}/depots`,
        },
        req => {
          req.reply(200, [], { 'Content-type': 'application/json' });
        },
      );

      cy.intercept(
        {
          pathname: `**/companies/${companyId}/account-info`,
        },
        req => {
          req.reply(200, getAccountInfo(), { 'Content-type': 'application/json' });
        },
      );

      cy.intercept(
        {
          pathname: `**/companies`,
        },
        req => {
          req.reply(200, [getCompany('YTEST-fake')], { 'Content-type': 'application/json' });
        },
      );

      cy.get(selectors.noLogin.registerForm.submit).click();

      cy.get('.success-popup__popup', { timeout: 10000 }).should('be.visible');
    });
  });

  after(function () {
    cy.clearCookies();
  });
});
