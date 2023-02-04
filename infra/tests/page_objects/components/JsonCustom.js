import { BaseComponent } from './BaseComponent';

export class JsonCustom extends BaseComponent {
   constructor(element) {
      super(element);
      this.element = element;
      this.json = element.find('[data-test="json-custom"]');
   }

   containsKeyValue = async (t, key, value, eql = true) => {
      const keyValue = this.json
         .find('span.key')
         .withText(`"${key}"`)
         .parent(`[data-key="${key}"]`)
         .find('span.value')
         // @nodejsgirl withExactText для строгих проверок значения, не менять
         .withExactText(value);

      await t.expect(keyValue.exists).eql(eql);
   };

   clickOnValue = async (t, key, value) => {
      const keyValue = this.json
         .find('span.key')
         .withText(`"${key}"`)
         .parent(`[data-key="${key}"]`)
         .find('span.clickable')
         // @nodejsgirl withExactText для строгих проверок значения
         .withExactText(value);

      await this.findElement(t, keyValue);
      await t.click(keyValue);
   };
}
