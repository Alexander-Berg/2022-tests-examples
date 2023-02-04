import selectors from '../../../src/constants/selectors';

const popup = {
  title: 'Сохраните свои Машины в Маршрутизации',
};

context('Reference guide video (ru)', function () {
  before(() => {
    cy.clearLocalforage();
    cy.preserveCookies();
    cy.yandexLogin('mvrpManager');
    cy.openAndCloseVideo();
  });

  it('should be visible on first entering vehicles reference book', function () {
    cy.get(selectors.sidebar.menu.collections).click();
    cy.get(selectors.sidebar.menu.vehiclesReferenceBook).click();
    cy.get(selectors.modal.helpVideo.popup).should('be.visible');
    cy.get(selectors.modal.helpVideo.title).should('have.text', popup.title);
  });

  it('should be closed on close button click', function () {
    cy.get(selectors.modal.helpVideo.close).click();
    cy.get(selectors.modal.helpVideo.popup).should('not.exist');
  });

  it('should not exist on reload', function () {
    cy.reload();
    cy.get(selectors.sidebar.menu.collections).should('be.visible');
    cy.wait(2000);
    cy.get(selectors.modal.helpVideo.popup).should('not.exist');
  });

  it('should be closed on click outside', function () {
    cy.clearLocalforage();
    cy.openAndCloseVideo({ onlyOpen: true });
    cy.yandexLogin('mvrpManager');
    cy.get(selectors.modal.helpVideo.close).click();
    cy.get(selectors.sidebar.menu.collections).click();
    cy.get(selectors.sidebar.menu.vehiclesReferenceBook).click();

    cy.get(selectors.modal.helpVideo.popup).should('be.visible');
    cy.get(selectors.modal.helpVideo.title).should('have.text', popup.title);

    cy.get('body').click(0, 0, { scrollBehavior: false });
    cy.get(selectors.modal.helpVideo.popup).should('not.exist');
  });
  it('should be visible after clicking link in help', function () {
    cy.get(selectors.sidebar.menu.help).click();
    cy.get(selectors.sidebar.menu.helpItems.referenceGuide).click();

    cy.get(selectors.modal.helpVideo.popup).should('be.visible');
    cy.get(selectors.modal.helpVideo.title).should('have.text', popup.title);
  });
  it('should not exist after logging from other account', function () {
    cy.yandexLogin('mvrpViewManager');
    cy.get(selectors.modal.helpVideo.close).click();
    cy.get(selectors.sidebar.menu.collections).click();
    cy.get(selectors.sidebar.menu.vehiclesReferenceBook).click();

    cy.get(selectors.sidebar.menu.collections).should('be.visible');
    cy.wait(2000);
    cy.get(selectors.modal.helpVideo.popup).should('not.exist');
  });

  it('should be visible on login after cleaning storage', () => {
    cy.clearLocalforage();
    cy.clearCookies();
    cy.openAndCloseVideo({ onlyOpen: true });

    cy.yandexLogin('mvrpManager');
    cy.openAndCloseVideo();
    cy.get(selectors.sidebar.menu.collections).click();
    cy.get(selectors.sidebar.menu.vehiclesReferenceBook).click();
    cy.get(selectors.modal.helpVideo.popup).should('be.visible');
    cy.get(selectors.modal.helpVideo.title).should('have.text', popup.title);
  });
});

context('Reference guide video (com)', () => {
  it('should not exist', function () {
    cy.yandexLogin('mvrpManager', { tld: 'com' });

    cy.get(selectors.sidebar.menu.collections).click();
    cy.get(selectors.sidebar.menu.vehiclesReferenceBook).click();

    cy.wait(200);
    cy.get(selectors.modal.helpVideo.popup).should('not.exist');

    cy.get(selectors.sidebar.menu.help).click();
    cy.get(selectors.sidebar.menu.helpItems.referenceGuide).should('not.exist');
  });
});
