import { BaseComponent } from './BaseComponent';

export class YCTabs extends BaseComponent {
   constructor(element) {
      super(element);
      this.element = element;

      this.tabsItems = this.element.find('.yc-tabs__item');
      this.tabs = this.tabsItems.parent('.yc-tabs');
   }

   readValue = async (t, value) => {
      await this.findElement(t, this.tabs);
      await t.expect(this.tabs.find('.yc-tabs__item_active').withExactText(value).exists).eql(true);
   };

   select = async (t, expected) => {
      await this.findElement(t, this.tabs);
      await t.click(this.tabs.find('.yc-tabs__item').withExactText(expected));
      await this.readValue(t, expected);
   };

   change = async (t, actual, expected) => {
      await this.findElement(t, this.tabs);
      await this.readValue(t, actual);
      await this.select(t, expected);
      await this.readValue(t, expected);
   };
}
