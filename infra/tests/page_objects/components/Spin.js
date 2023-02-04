// import {Selector} from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class Spin extends BaseComponent {
   constructor(element) {
      super(element);
      this.element = element;
      this.spin = element.find('spin2');
   }

   /*
    exists = async (t) => {
        await t.expect(this.spin.exists).eql(true);
    }

    notExists = async (t) => {
        await t.expect(this.spin.exists).eql(false);
    }
*/
   /*
    <div data-lego="react" class="spin2 spin2_size_xxs spin2_view_default spin2_tone_default spin2_progress_yes"></div>
    */

   /* click = async (t) => {
        await this.findElement(t, this.element);
        // if element has no parent selector (duplicateDeployUnit, deleteDeployUnit, tvm--add-client, tvm__button_delete-destination)
        if (await this.element.tagName === 'button') {
            await t.click(this.element);
        } else {
            await t.click(this.button);
        }
    }*/

   // isDisabled = async (t) => {
   // ...
   // }
}
