import { Selector } from 'testcafe';
import { SelectDataTest } from '../../helpers/extractors';
import { Link } from './Link';

export class Header {
   // body = Selector('.header');

   logo = SelectDataTest('logo');
   linkLogo = this.logo.find('a > div');

   eventBadge = Selector('button.events-notifier-icon');
   eventPopup = Selector('.yc-popup.yc-popup_open.events-notifier-popup').filterVisible();

   user = Selector('.header__user');

   supportLinks = SelectDataTest('support-links');
   linkTelegram = SelectDataTest('support-link-telegram');
   linkST = SelectDataTest('support-link-st');
   linkDocs = SelectDataTest('support-link-docs');
}
