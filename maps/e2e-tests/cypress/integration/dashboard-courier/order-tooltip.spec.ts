import selectors from '../../../src/constants/selectors';
import { courierNameRecord } from '../../../src/constants/couriers';
import { routeNumberRecord } from '../../../src/constants/routes';

import urls from '../../../src/utils/urls';
import time from '../../../src/utils/time';
import moment from 'moment';

const courierDetailsLabels = {
  login: 'Логин',
  phone: 'Телефон',
  smsTumbler: 'Отправлять SMS клиентам',
};

const routeMapDetailsLabels = {
  builtTheRoute: 'Построил маршрут',
  sentCoordinates: 'Присылал координаты',
  timeZone: 'Часовой пояс',
};

const moveMapView = (diff: number): void => {
  cy.get(selectors.content.courier.route.map.yandex.depotIcon)
    .eq(0)
    .triggerOnLayer(selectors.content.courier.route.map.yandex.eventsLayer, {
      event: 'mousedown',
      scrollBehavior: false,
    })
    .triggerOnLayer(selectors.content.courier.route.map.yandex.eventsLayer, {
      event: 'mousemove',
      deltaX: diff,
    })
    .triggerOnLayer(selectors.content.courier.route.map.yandex.eventsLayer, {
      event: 'mouseup',
      scrollBehavior: false,
    });
};

// @see https://testpalm.yandex-team.ru/courier/testcases/355
context('Courier page', () => {
  before(() => {
    cy.preserveCookies();
    cy.fixture('company-data').then(({ common }) => {
      const date = moment(time.TIME_TODAY).add(5, 'days').format(urls.dashboard.dateFormat);
      const link = urls.couriersList.createLink(common.companyId, {
        date: date,
      });
      cy.yandexLogin('manager', { link });
    });
    cy.get(selectors.sidebar.menu.monitoringGroup).click();
    cy.get(selectors.sidebar.menu.dashboard).click();
    cy.get(selectors.content.dashboard.couriers.table.routeName)
      .contains(new RegExp(`^${routeNumberRecord.LOT_OF_ORDERS}$`))
      .click();
  });

  context('Map order tooltip ', () => {
    it('on hover order pin tooltip should be show', () => {
      cy.get(selectors.content.courier.route.map.yandex.activeOrder)
        .eq(0)
        .triggerOnLayer(selectors.content.courier.route.map.yandex.eventsLayer, {
          event: 'mouseover',
          scrollBehavior: false,
        });
      cy.get(selectors.content.courier.route.map.yandex.orderPinTooltip).should('be.visible');
    });

    it('order row hovered pin should be selected', () => {
      cy.get(selectors.content.courier.route.tableRow).first().should('have.class', 'row-selected');
    });

    it('should display order name, address, client name and delivery interval on tooltip', () => {
      cy.get(selectors.content.courier.route.map.yandex.orderPinTooltip)
        .should('be.visible')
        .invoke('text')
        .as('location1');
      cy.get(selectors.content.courier.route.map.yandex.orderPinTooltip)
        .get(selectors.content.courier.route.map.yandex.orderPinTooltipTitle)
        .should('contain', 'Заказ 1000');

      cy.get(selectors.content.courier.route.map.yandex.getOrderPinTooltipTableRow(1)).should(
        'contain',
        'customer-lot-of-orders',
      );
      cy.get(selectors.content.courier.route.map.yandex.getOrderPinTooltipTableRow(2)).should(
        'contain',
        '00:00 - 23:59',
      );
      cy.get(selectors.content.courier.route.map.yandex.getOrderPinTooltipTableRow(3)).should(
        'contain',
        'some adress-0',
      );
    });

    it('should close tooltip when unhover pin', () => {
      cy.get(selectors.content.courier.route.map.yandex.activeOrder)
        .eq(0)
        .triggerOnLayer(selectors.content.courier.route.map.yandex.eventsLayer, {
          event: 'mouseout',
          scrollBehavior: false,
        });
      cy.get(selectors.content.courier.route.map.yandex.orderPinTooltip).should('not.exist');
    });

    it('hover on new pin tooltip should have other information', function () {
      cy.get(selectors.content.courier.route.map.yandex.activeOrder)
        .eq(10)
        .triggerOnLayer(selectors.content.courier.route.map.yandex.eventsLayer, {
          event: 'mouseover',
          scrollBehavior: false,
        });

      cy.get(selectors.content.courier.route.map.yandex.orderPinTooltip)
        .invoke('text')
        .should('not.eq', this.location1);
    });

    it('on clicked pin should scroll to selected order row', function () {
      cy.wait(300);

      // get row content
      cy.get(selectors.content.courier.route.getTableRow(3))
        .should('be.visible')
        .invoke('text')
        .as('row1');

      // click pin
      cy.get(selectors.content.courier.route.map.yandex.activeOrder)
        .eq(12)
        .triggerOnLayer(selectors.content.courier.route.map.yandex.eventsLayer, {
          event: 'click',
          scrollBehavior: false,
        });
      cy.wait(100);

      // check that content row changed (scrolled)
      cy.get(selectors.content.courier.route.getTableRow(3))
        .invoke('text')
        .should('not.eq', this.row1);

      cy.get(selectors.content.courier.route.getTableRow(3)).should('have.class', 'row-selected');

      cy.get(selectors.content.courier.route.map.yandex.orderPinTooltipTitle)
        .invoke('text')
        .then(orderString => {
          const orderNumber = orderString.split(' ')[1];
          cy.get(
            `${selectors.content.courier.route.getTableRow(3)} ${
              selectors.content.courier.route.tableCellOrderNumber
            }`,
          )
            .invoke('text')
            .should('eq', orderNumber);
        });
    });
  });
});
