import * as companySettingsKeyset from '../../../../src/translations/company-settings';
import * as createRouteFormKeyset from '../../../../src/translations/create-route-form';
import * as authKeyset from '../../../../src/translations/auth';
import * as headerKeyset from '../../../../src/translations/header';
import * as courierRouteKeyset from '../../../../src/translations/courier-route';
import * as importKeyset from '../../../../src/translations/import';
import * as videoModalKeyset from '../../../../src/translations/video-modal';
import * as sidebarKeyset from '../../../../src/translations/sidebar';
import selectors from '../../../src/constants/selectors';
import { getBaseUrl } from '../../support/utils';

// @see https://testpalm.yandex-team.ru/courier/testcases/439
context('Sidebar - common', () => {
  before(() => {
    cy.yandexLogin('superuser');
  });

  it('Sidebar is open on start page', () => {
    cy.waitForElement(selectors.content.mvrp.start).wait(2000).openAndCloseVideo();
    cy.get(selectors.sidebar.view).should('have.class', 'sidebar_is-open');
  });

  it('Sidebar is close when clicked on close button', () => {
    cy.get(selectors.sidebar.closeButton).click();
    cy.get(selectors.sidebar.view).should('have.not.class', 'sidebar_is-open');
  });

  it('Sidebar is open when clicked on open button', () => {
    cy.get(selectors.sidebar.closeButton).click();
    cy.get(selectors.sidebar.view).should('have.class', 'sidebar_is-open');
  });

  it('Sidebar close, click by button company open sidebar and menu company', () => {
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
    cy.get(selectors.content.dashboard.view);
    cy.get(selectors.sidebar.closeButton).click();
    cy.wait(100);
    cy.get(selectors.sidebar.companySelector.iconLetterCompany).click();
    cy.get(selectors.sidebar.view).should('have.class', 'sidebar_is-open');
    cy.get(selectors.sidebar.companySelector.toggleControl).should(
      'have.class',
      'select-item__control_active',
    );
    cy.get(selectors.superUserRights.companyDropdown);
    cy.get(selectors.sidebar.closeButton).click();
  });

  it('Sidebar close, click by button planning do not open sidebar, open screen start mvrp', () => {
    cy.get(selectors.sidebar.closeButton).click();
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
    cy.get(selectors.content.dashboard.view);
    cy.get(selectors.sidebar.closeButton).click();
    cy.get(selectors.sidebar.menu.mvrp).click();
    cy.get(selectors.content.mvrp.start);
    cy.get(selectors.content.mvrp.fileInput);
    cy.get(selectors.sidebar.view).should('have.not.class', 'sidebar_is-open');
  });

  it('Sidebar close, click by button password open sidebar and open user settings', () => {
    cy.get(selectors.sidebar.user.view).click();
    cy.get(selectors.sidebar.view).should('have.class', 'sidebar_is-open');
    cy.get(selectors.sidebar.user.panel);
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/498
  describe('Help links section', function () {
    after(function () {
      cy.openAndCloseVideo();
    });

    it('should open and show links on click', function () {
      cy.get(selectors.sidebar.menu.help).click();

      cy.get(selectors.sidebar.menu.helpItems.documentation).should('be.visible');
      cy.get(selectors.sidebar.menu.helpItems.interfaceGuide).should('be.visible');
      cy.get(selectors.sidebar.menu.helpItems.referenceGuide).should('be.visible');
      cy.get(selectors.sidebar.menu.helpItems.new).should('be.visible');
      cy.get(selectors.sidebar.menu.helpItems.getTemplate).should('be.visible');
    });

    it('should have correct links', function () {
      cy.get(selectors.sidebar.menu.helpItems.documentation)
        .should('have.attr', 'href')
        .and('include', sidebarKeyset.ru.help_link);

      cy.get(selectors.sidebar.menu.helpItems.new)
        .should('have.attr', 'href')
        .and('include', sidebarKeyset.ru.releaseNotesLink);

      cy.get(selectors.sidebar.menu.helpItems.getTemplate).then(function ($a) {
        const href = $a.prop('href');

        cy.request(href).its('body').should('include', 'Формирование файла планирования');
      });
    });

    it('should open help video on interface guide item click', function () {
      cy.get(selectors.sidebar.menu.helpItems.interfaceGuide).click();

      cy.get(selectors.modal.helpVideo.popup).should('exist');
      cy.get(selectors.modal.helpVideo.video).should('exist');
      cy.get(selectors.modal.helpVideo.title).should(
        'have.text',
        videoModalKeyset.ru.video_LESSON_ONE_title,
      );
      cy.get(selectors.modal.helpVideo.info)
        .should('include.text', 'Если после просмотра видео у вас останутся вопросы, то')
        .should('include.text', 'подробную инструкцию можете прочитать в');
      cy.get(selectors.modal.helpVideo.helpLink)
        .should('have.text', videoModalKeyset.ru.video_LESSON_ONE_description_LinkText)
        .should('have.attr', 'href')
        .and('include', sidebarKeyset.ru.help_link);
      cy.get(selectors.modal.helpVideo.videoSource)
        .should('have.attr', 'src')
        .and('include', encodeURI('Первое планирование'));

      cy.get(selectors.modal.helpVideo.close).click();
    });

    it('should open help video on reference guide item click', function () {
      cy.get(selectors.sidebar.menu.helpItems.referenceGuide).click();

      cy.get(selectors.modal.helpVideo.popup).should('exist');
      cy.get(selectors.modal.helpVideo.video).should('exist');
      cy.get(selectors.modal.helpVideo.title).should(
        'have.text',
        videoModalKeyset.ru.video_VEHICLES_REFERENCE_BOOK_title,
      );
      cy.get(selectors.modal.helpVideo.info)
        .should('include.text', 'Транспортные средства теперь можно сохранить в')
        .should('include.text', 'Справочнике и использовать при новом планировании');
      cy.get(selectors.modal.helpVideo.videoSource)
        .should('have.attr', 'src')
        .and('include', 'reference-book-ru');

      cy.get(selectors.modal.helpVideo.close).click();
    });

    // @see https://testpalm.yandex-team.ru/courier/testcases/559
    describe('Interface guide video', () => {
      it('should open modal on click', () => {
        cy.get(selectors.sidebar.menu.helpItems.interfaceGuide).click();

        cy.get(selectors.modal.helpVideo.popup).should('exist');
        cy.get(selectors.modal.helpVideo.video).should('have.prop', 'paused', true);
      });

      it('should start play the video on play button click', () => {
        cy.get(selectors.modal.helpVideo.play).click();

        cy.get(selectors.modal.helpVideo.video).should('have.prop', 'paused', false);
        cy.get(selectors.modal.helpVideo.video).invoke('prop', 'currentTime').should('not.eq', 0);
      });

      it('should rewind the video', () => {
        cy.get(selectors.modal.helpVideo.video).then($video => {
          const video = $video[0] as HTMLMediaElement;
          video.currentTime = 120;
        });

        cy.get(selectors.modal.helpVideo.video)
          .invoke('prop', 'currentTime')
          .should('be.greaterThan', 120);
      });

      it('should pause the video', () => {
        cy.get(selectors.modal.helpVideo.video).then($video => {
          const video = $video[0] as HTMLMediaElement;
          video.pause();
        });

        cy.get(selectors.modal.helpVideo.video).should('have.prop', 'paused', true);
      });

      it('should close modal when video is playing', () => {
        cy.get(selectors.modal.helpVideo.video).then($video => {
          const video = $video[0] as HTMLMediaElement;
          video.play();
        });

        cy.get(selectors.modal.helpVideo.video).should('have.prop', 'paused', false);
        cy.get(selectors.modal.helpVideo.popup).parent().click(10, 10).should('not.exist');
        cy.get(selectors.modal.helpVideo.popup).should('not.exist');
      });

      it('should close modal by clicking close button', () => {
        cy.get(selectors.sidebar.menu.helpItems.interfaceGuide).click();
        cy.get(selectors.modal.helpVideo.closeButton).click();

        cy.get(selectors.modal.helpVideo.popup).should('not.exist');
      });
    });
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/443
  describe('User selector links', function () {
    beforeEach(() => {
      cy.preserveCookies();
    });

    before(function () {
      cy.clearCookies();
      cy.yandexLogin('admin');
      cy.waitForElement(selectors.sidebar.user.control);
    });

    after(function () {
      cy.clearCookies();
    });

    it('Show user panel on click', function () {
      cy.get(selectors.sidebar.user.control).click();

      cy.get(selectors.sidebar.user.panel).should('exist');
      cy.get(selectors.sidebar.user.addAccountLink).should('exist');
      cy.get(selectors.sidebar.user.mailLink).should('have.prop', 'href', 'http://mail.yandex.ru/');
      cy.get(selectors.sidebar.user.manageAccountLink)
        .should('have.prop', 'href')
        .and('include', 'https://passport.yandex.ru');
      cy.get(selectors.sidebar.user.logout)
        .should('have.prop', 'href')
        .and('include', 'https://passport.yandex.ru/passport?action=logout');
      cy.get(selectors.sidebar.user.settingsLink).should(
        'have.prop',
        'href',
        'https://yandex.ru/tune',
      );
      cy.get(selectors.sidebar.user.supportLink).should(
        'have.prop',
        'href',
        'https://yandex.ru/support',
      );
    });
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/572
  describe('Contact support form', () => {
    before(function () {
      cy.clearCookies();
      cy.yandexLogin('adminMulti');
    });

    it('should open on sidebar link click', () => {
      cy.get(selectors.sidebar.serviceLogo);
      cy.get(selectors.sidebar.menu.contactSupport)
        .invoke('prop', 'href')
        .then(href => {
          cy.forceVisit(href);
          cy.get('h1').should('have.text', 'Обращение в службу поддержки Яндекс Маршрутизации');
          cy.contains('tr', courierRouteKeyset.ru['routeTable_column_courier.name'])
            .contains('*')
            .should('exist');
          cy.contains('tr', headerKeyset.ru.user_email).contains('*').should('exist');
          cy.contains('tr', 'ID компании').should('exist');
          cy.contains('tr', 'Продукт').contains('*').should('exist');
          cy.contains('tr', 'Сообщение').contains('*').should('exist');
        });
    });

    it('should contain predefined values', () => {
      cy.fixture('testData').then(({ accounts }) => {
        cy.fixture('company-data').then(({ shareFromOthersCompany }) => {
          cy.contains('tr', courierRouteKeyset.ru['routeTable_column_courier.name'])
            .find('input')
            .should('have.value', 'Pupkin Vasily');
          cy.contains('tr', headerKeyset.ru.user_email)
            .find('input')
            .should('have.value', `${accounts.adminMulti}@yandex.ru`);
          cy.contains('tr', 'ID компании ')
            .find('input')
            .should('have.value', `${shareFromOthersCompany.companyId}`);
        });
      });

      cy.go('back');
    });

    it('switch company', () => {
      cy.fixture('company-data').then(({ V }) => {
        cy.get(selectors.sidebar.companySelector.toggleControl).click();
        cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrg)
          .contains(V.companyName)
          .click({ force: true });
        cy.get(selectors.content.dashboard.view);
        cy.get(selectors.sidebar.companySelector.toggleControl).click();
      });
    });

    it('should contain correct values for other company', () => {
      cy.get(selectors.sidebar.menu.contactSupport)
        .invoke('prop', 'href')
        .then(href => {
          cy.forceVisit(href);

          cy.fixture('testData').then(({ accounts }) => {
            cy.fixture('company-data').then(({ V }) => {
              cy.contains('tr', courierRouteKeyset.ru['routeTable_column_courier.name'])
                .find('input')
                .should('have.value', 'Pupkin Vasily');
              cy.contains('tr', headerKeyset.ru.user_email)
                .find('input')
                .should('have.value', `${accounts.adminMulti}@yandex.ru`);
              cy.contains('tr', 'ID компании ')
                .find('input')
                .should('have.value', `${V.companyId}`);
            });
          });

          cy.go('back');
        });
    });

    it('link is sticky on bottom when scrolling sidebar menu', () => {
      cy.viewport(1280, 800);
      cy.get(selectors.sidebar.menu.monitoringGroup).click({ scrollBehavior: false });
      cy.get(selectors.sidebar.menu.reports).click({ scrollBehavior: false });
      cy.get(selectors.sidebar.menu.developmentGroup).click({ scrollBehavior: false });
      cy.get(selectors.sidebar.menu.help).click({ scrollBehavior: false }).wait(300);

      cy.get(selectors.sidebar.menu.support).then($link => {
        const initialTop = $link[0].getBoundingClientRect().top;

        cy.get(selectors.sidebar.menu.helpItems.getTemplate).scrollIntoView().wait(300);

        cy.get(selectors.sidebar.menu.support)
          .should('be.visible')
          .then($link => {
            return $link[0].getBoundingClientRect().top;
          })
          .should('eq', initialTop);

        cy.get(selectors.sidebar.menu.monitoringGroup).scrollIntoView().wait(300);

        cy.get(selectors.sidebar.menu.support)
          .should('be.visible')
          .then($link => {
            return $link[0].getBoundingClientRect().top;
          })
          .should('eq', initialTop);
      });
    });
  });
});

const baseUrl = Cypress.env('BASE_URL').replace('ru', 'com.tr');
context('Common elements on .com.tr domain', { baseUrl }, () => {
  beforeEach(() => {
    cy.preserveCookies();
  });

  before(function () {
    cy.clearCookies();
    cy.yandexLogin('admin', { tld: 'com.tr' });
    cy.waitForElement(selectors.sidebar.user.control);
    cy.get(selectors.sidebar.menu.help).click();
  });

  after(function () {
    cy.clearCookies();
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/443
  describe('User selector links', function () {
    it('Show user panel', function () {
      cy.get(selectors.sidebar.user.control).click();

      cy.get(selectors.sidebar.user.panel).should('exist');
      cy.get(selectors.sidebar.user.addAccountLink).should('exist');
      cy.get(selectors.sidebar.user.mailLink).should(
        'have.prop',
        'href',
        'http://mail.yandex.com.tr/',
      );
      cy.get(selectors.sidebar.user.manageAccountLink)
        .should('have.prop', 'href')
        .and('include', 'https://passport.yandex.com.tr');
      cy.get(selectors.sidebar.user.logout)
        .should('have.prop', 'href')
        .and('include', 'https://passport.yandex.com.tr/passport?action=logout');
      cy.get(selectors.sidebar.user.settingsLink).should(
        'have.prop',
        'href',
        'https://yandex.com.tr/tune',
      );
      cy.get(selectors.sidebar.user.supportLink).should(
        'have.prop',
        'href',
        'https://yandex.com.tr/support',
      );
    });
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/559
  describe('Interface guide video', () => {
    it('should open modal on click', () => {
      cy.get(selectors.sidebar.menu.helpItems.interfaceGuide).click();

      cy.get(selectors.modal.helpVideo.popup).should('exist');
      cy.get(selectors.modal.helpVideo.video).should('have.prop', 'paused', true);
    });

    it('should start play the video on play button click', () => {
      cy.get(selectors.modal.helpVideo.play).click();

      cy.get(selectors.modal.helpVideo.video).should('have.prop', 'paused', false);
      cy.get(selectors.modal.helpVideo.video).invoke('prop', 'currentTime').should('not.eq', 0);
    });

    it('should close modal when video is playing', () => {
      cy.get(selectors.modal.helpVideo.video).then($video => {
        const video = $video[0] as HTMLMediaElement;
        video.play();
      });

      cy.get(selectors.modal.helpVideo.video).should('have.prop', 'paused', false);
      cy.get(selectors.modal.helpVideo.popup).parent().click(10, 10).should('not.exist');
      cy.get(selectors.modal.helpVideo.popup).should('not.exist');
    });

    it('should close modal by clicking close button', () => {
      cy.get(selectors.sidebar.menu.helpItems.interfaceGuide).click();
      cy.get(selectors.modal.helpVideo.closeButton).click();

      cy.get(selectors.modal.helpVideo.popup).should('not.exist');
    });
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/562
  describe('Help links section', function () {
    it('should contain links', function () {
      cy.get(selectors.sidebar.menu.helpItems.documentation).should('be.visible');
      cy.get(selectors.sidebar.menu.helpItems.interfaceGuide).should('be.visible');
      cy.get(selectors.sidebar.menu.helpItems.new).should('be.visible');
    });

    it('should have correct links', function () {
      cy.get(selectors.sidebar.menu.helpItems.documentation)
        .should('have.text', importKeyset.tr.docs)
        .should('have.attr', 'href')
        .and('include', authKeyset.en.createCompanySuccess_vrcDocLink);

      cy.get(selectors.sidebar.menu.helpItems.new)
        .should('have.text', sidebarKeyset.tr.releaseNotes)
        .should('have.attr', 'href')
        .and('include', sidebarKeyset.tr.releaseNotesLink);
    });

    it('should open help video on interface guide item click', function () {
      cy.get(selectors.sidebar.menu.helpItems.interfaceGuide)
        .should('have.text', sidebarKeyset.tr.video_LESSON_ONE)
        .click();

      cy.get(selectors.modal.helpVideo.popup).should('exist');
      cy.get(selectors.modal.helpVideo.video).should('exist');
      cy.get(selectors.modal.helpVideo.title).should(
        'have.text',
        "Yandex.Routing'i nasıl kullanacağınızı 1 dakikada öğrenin.",
      );
      cy.get(selectors.modal.helpVideo.info)
        .should(
          'include.text',
          'Videoyu izledikten sonra hâlâ sorularınız varsa, ayrıntılı talimatları',
        )
        .should('include.text', 'okuyabilirsiniz');
      cy.get(selectors.modal.helpVideo.helpLink)
        .should('have.text', videoModalKeyset.tr.video_LESSON_ONE_description_LinkText)
        .should('have.attr', 'href')
        .and('include', authKeyset.en.createCompanySuccess_vrcDocLink);

      cy.get(selectors.modal.helpVideo.video).should('exist');

      cy.get(selectors.modal.helpVideo.close).click();
    });
  });

  // @see https://testpalm.yandex-team.ru/courier/testcases/573
  describe('Contact support form', () => {
    before(function () {
      cy.clearCookies();
      cy.yandexLogin('adminMulti', { tld: 'com.tr' });
    });

    it('should open on sidebar link click', () => {
      cy.get(selectors.sidebar.menu.support).click();
      cy.get(selectors.sidebar.menu.contactSupport).should('have.attr', 'target', '_blank');
      cy.get(selectors.sidebar.menu.contactSupport).invoke('removeAttr', 'target').click();
      cy.get('h1').should('have.text', 'RouteQ support');
      cy.contains('tr', createRouteFormKeyset.en.label_number).contains('*').should('exist');
      cy.contains('tr', 'Email').contains('*').should('exist');
      cy.contains('tr', companySettingsKeyset.en.companyId).should('exist');
      cy.contains('tr', 'Question or problem').contains('*').should('exist');
    });

    it('should contain predefined values', () => {
      cy.fixture('testData').then(({ accounts }) => {
        cy.fixture('company-data').then(({ shareFromOthersCompany }) => {
          cy.contains('tr', createRouteFormKeyset.en.label_number)
            .find('input')
            .should('have.value', 'Pupkin Vasily');
          cy.contains('tr', 'Email')
            .find('input')
            .should('have.value', `${accounts.adminMulti}@yandex.ru`);
          cy.contains('tr', companySettingsKeyset.en.companyId)
            .find('input')
            .should('have.value', `${shareFromOthersCompany.companyId}`);
        });
      });

      cy.go('back');
    });

    it('switch company', () => {
      cy.fixture('company-data').then(({ V }) => {
        cy.get(selectors.sidebar.companySelector.toggleControl).click();
        cy.get(selectors.sidebar.companySelector.orgs.dropdownItems.anyOrg)
          .contains(V.companyName)
          .click({ force: true });
        cy.get(selectors.content.dashboard.view);
        cy.get(selectors.sidebar.companySelector.toggleControl).click();
      });
    });

    it('should contain correct values for other company', () => {
      cy.get(selectors.sidebar.menu.support).click();
      cy.get(selectors.sidebar.menu.contactSupport).should('have.attr', 'target', '_blank');
      cy.get(selectors.sidebar.menu.contactSupport).invoke('removeAttr', 'target').click();

      cy.fixture('testData').then(({ accounts }) => {
        cy.fixture('company-data').then(({ V }) => {
          cy.contains('tr', createRouteFormKeyset.en.label_number)
            .find('input')
            .should('have.value', 'Pupkin Vasily');
          cy.contains('tr', 'Email')
            .find('input')
            .should('have.value', `${accounts.adminMulti}@yandex.ru`);
          cy.contains('tr', companySettingsKeyset.en.companyId)
            .find('input')
            .should('have.value', `${V.companyId}`);
        });
      });

      cy.go('back');
    });
  });
});

describe('Switching available accounts', () => {
  // @see https://testpalm.yandex-team.ru/courier/testcases/444
  beforeEach(() => {
    cy.preserveCookies();
  });

  before(function () {
    cy.clearCookies();

    cy.readLoginCookies('temp-manager-admin').then(token => {
      if (token) {
        cy.setLoginCookies(token);
      } else {
        cy.manualPassportLogin('temp');
        cy.manualPassportLogin('manager');
        cy.manualPassportLogin('admin');
        cy.writeLoginCookies('temp-manager-admin');
      }

      cy.visit(getBaseUrl());
    });
  });

  it('Show user panel', function () {
    cy.get(selectors.sidebar.user.control).click();

    cy.get(selectors.sidebar.user.panel).should('exist');
    cy.get(selectors.sidebar.user.users.current).should('be.visible');
    cy.get(selectors.sidebar.user.users.inactiveUserName).should('be.visible');
    cy.get(selectors.sidebar.user.addAccountLink).should('be.visible');
    cy.get(selectors.sidebar.user.mailLink).should('be.visible');
    cy.get(selectors.sidebar.user.manageAccountLink).should('be.visible');
    cy.get(selectors.sidebar.user.logout).should('be.visible');
    cy.get(selectors.sidebar.user.settingsLink).should('be.visible');
    cy.get(selectors.sidebar.user.supportLink).should('be.visible');
  });

  it('Show list of accounts', () => {
    cy.fixture('testData').then(({ accounts }) => {
      const accountsSet = new Set([accounts.temp, accounts.admin, accounts.manager]);

      cy.get(selectors.sidebar.user.users.all)
        .should('have.length', 3)
        .each(name => {
          const nameText = name.text();

          expect(accountsSet).to.include(nameText);

          accountsSet.delete(nameText);
        });
    });
  });

  it('Close user panel on current user name click', function () {
    cy.get(selectors.sidebar.user.users.current).click({ force: true });

    cy.get(selectors.sidebar.user.panel).should('not.exist');
    cy.get(selectors.sidebar.user.users.inactiveUserName).should('not.be.visible');
    cy.get(selectors.sidebar.user.addAccountLink).should('not.be.visible');
    cy.get(selectors.sidebar.user.mailLink).should('not.be.visible');
    cy.get(selectors.sidebar.user.manageAccountLink).should('not.be.visible');
    cy.get(selectors.sidebar.user.logout).should('not.be.visible');
    cy.get(selectors.sidebar.user.settingsLink).should('not.be.visible');
    cy.get(selectors.sidebar.user.supportLink).should('not.be.visible');

    cy.get(selectors.sidebar.user.users.current).should('be.visible');
  });

  it('Switch user from user panel', function () {
    cy.get(selectors.sidebar.user.users.current).click();

    cy.fixture('testData').then(({ accounts }) => {
      cy.get(selectors.sidebar.user.users.current).then(el => {
        const login = el.text().includes(accounts.admin) ? accounts.manager : accounts.admin;
        cy.get(selectors.sidebar.user.users.inactiveUserName).contains(login).click();

        cy.get(selectors.sidebar.user.users.current).should('have.text', login);
        cy.get(selectors.sidebar.user.panel).should('not.exist');

        cy.get(selectors.content.dashboard.view).should('exist');
      });
    });
  });

  it('Add account button contains link to add user', () => {
    cy.get(selectors.sidebar.user.control).click({ force: true });

    cy.get(selectors.sidebar.user.addAccountLink)
      .should('have.prop', 'href')
      .and('include', 'https://passport.yandex.ru/auth?mode=add-user');
  });

  it('Login empty user', () => {
    cy.fixture('testData').then(({ accounts }) => {
      const login = accounts.temp;
      cy.get(selectors.sidebar.user.users.inactiveUserName).contains(login).click({ force: true });

      cy.get(selectors.noLogin.title).should('have.text', authKeyset.ru.noAccess_header);

      cy.visit(getBaseUrl());
      cy.get(selectors.noLogin.registerForm.form).should('exist');
    });
  });
});
