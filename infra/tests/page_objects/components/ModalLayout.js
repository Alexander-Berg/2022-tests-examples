import { Selector } from 'testcafe';

import { Button } from './Button';
import { YCTabs } from './YcTabs';

const modal = Selector('.yc-dialog');

export class ModalLayout {
   wrapper = Selector('.yc-dialog');

   header = modal.find('.yc-dialog-header');
   body = modal.find('.yc-dialog-body');
   footer = modal.find('.yc-dialog-footer');

   tabs = new YCTabs(this.body.find('.yc-tabs'));

   title = modal.find('.yc-dialog-header__caption');

   closeButton = new Button(modal.find('.yc-dialog-btn-close'));
   cancelButton = new Button(this.footer.find('.yc-dialog-footer__button.yc-dialog-footer__button_action_cancel'));
   okButton = new Button(this.footer.find('.yc-dialog-footer__button.yc-dialog-footer__button_action_apply'));
}
