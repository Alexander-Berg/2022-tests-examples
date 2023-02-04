import { Selector } from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class YCDropDown extends BaseComponent {
   constructor(element) {
      super(element);
      this.button = element.find('.yc-dropdown-menu__switcher-wrapper').find('button');
      this.selectedItem = this.button.find('.yc-button__text');

      this.popup = Selector('.yc-popup.yc-popup_open').find('.yc-menu').filterVisible();
      this.popupItems = this.popup.find('.yc-menu__list-item');
   }

   getPopupItemByText = text => {
      return this.popupItems.find('.yc-menu__item-content').withText(text).parent('.yc-menu__list-item');
   };

   change = async (t, actual, expected) => {
      await this.readValue(t, actual);

      await this.select(t, expected);
   };

   select = async (t, expected) => {
      await this.findElement(t, this.button);

      await t
         .expect(this.popup.exists)
         .eql(false)
         .click(this.button)
         .expect(this.popup.exists)
         .eql(true)
         .click(this.getPopupItemByText(expected));

      await this.readValue(t, expected);
   };

   readValue = async (t, value) => {
      await t.expect(this.selectedItem.withExactText(value).exists).eql(true);
   };
}
