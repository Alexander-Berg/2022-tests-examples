import selectors from '../../../../src/constants/selectors';

describe('Create courier in several companies', () => {
  const courierPhone = '79990002222';
  const courierNumber = '7776690';

  const chooseCommonCompany = (companyId: number) => {
    cy.get(selectors.sidebar.companySelector.control).click();
    cy.get(selectors.sidebar.companySelector.orgs.dropdown);
    cy.get(selectors.sidebar.companySelector.orgs.input).click({ force: true });
    cy.get(selectors.sidebar.companySelector.orgs.input).type(String(companyId), {
      force: true,
    });
    cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrg).click({ force: true });
    cy.get(selectors.sidebar.companySelector.toggleControl).click();
  };

  //st.yandex-team.ru/BBGEO-9912
  before(() => {
    cy.fixture('company-data').then(({ common }) => {
      cy.yandexLogin('superuser');
      cy.openAndCloseVideo();
      cy.waitForElement(selectors.sidebar.companySelector.toggleControl);
      chooseCommonCompany(common.companyId);
    });

    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.couriers).click();
  });

  it('Create courier in the first company', () => {
    cy.get(selectors.content.couriers.newCourier.phone).type(courierPhone);
    cy.get(selectors.content.couriers.newCourier.number).type(courierNumber);
    cy.get(selectors.content.couriers.newCourier.submitButton).click();
  });

  it('Switch to the second company', () => {
    cy.get(selectors.sidebar.companySelector.toggleControl).click();
    cy.get(selectors.sidebar.companySelector.orgs.dropdown);
    cy.get(selectors.sidebar.companySelector.orgs.input).click({ force: true });

    cy.fixture('company-data').then(({ A }) => {
      cy.get(selectors.sidebar.companySelector.orgs.input).type(String(A.companyId), {
        force: true,
      });
    });
    cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrg).click({ force: true });
    cy.get(selectors.sidebar.companySelector.toggleControl).click();

    cy.get(selectors.sidebar.menu.couriers).click();
  });

  it('Add button not disabled', () => {
    cy.get(selectors.content.couriers.newCourier.phone).type(courierPhone);
    cy.get(selectors.content.couriers.newCourier.number).type(courierNumber);

    cy.get(selectors.content.couriers.newCourier.submitButton).should('not.be.disabled');
  });

  it('Сourier is being added', () => {
    cy.get(selectors.content.couriers.newCourier.submitButton).click();

    const selectorToNewCourierLine = `${selectors.content.couriers.search.line}[data-test-anchor="${courierNumber}"]`;

    cy.get(`${selectorToNewCourierLine} ${selectors.content.couriers.search.lineName}`)
      .invoke('text')
      .should('eq', courierNumber);

    cy.get(`${selectorToNewCourierLine} ${selectors.content.couriers.search.lineNumber}`)
      .invoke('text')
      .should('eq', courierNumber);

    cy.get(`${selectorToNewCourierLine} ${selectors.content.couriers.search.lineSms}`)
      .invoke('val')
      .should('eq', 'false');
  });

  after(() => {
    cy.fixture('company-data').then(({ common }) => {
      const selectorToNewCourierLine = `${selectors.content.couriers.search.line}[data-test-anchor="${courierNumber}"]`;

      cy.get(`${selectorToNewCourierLine} ${selectors.content.couriers.search.lineRemove}`).click();
      cy.get(selectors.content.couriers.removeCourier.dialogContainer);
      cy.get(selectors.content.couriers.removeCourier.submitButton).click();

      chooseCommonCompany(common.companyId);

      cy.get(selectors.sidebar.menu.couriers).click();

      cy.get(`${selectorToNewCourierLine} ${selectors.content.couriers.search.lineRemove}`).click();
      cy.get(selectors.content.couriers.removeCourier.dialogContainer);
      cy.get(selectors.content.couriers.removeCourier.submitButton).click();
    });
  });
});
