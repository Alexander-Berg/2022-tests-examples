import { SelectDataE2e, SelectDataTest } from '../../helpers/extractors';
import { Button } from '../components/Button';
import { YCCheckbox } from '../components/YCCheckbox';
import { ModalLayout } from '../components/ModalLayout';

export class RemoveStageModal extends ModalLayout {
   checkbox = new YCCheckbox(this.body);

   okButton = new Button(SelectDataE2e('RemoveStageConfirmModal:OkButton'), 'RemoveStageConfirmModal:OkButton');

   cancelButton = new Button(
      SelectDataE2e('RemoveStageConfirmModal:CancelButton'),
      'RemoveStageConfirmModal:CancelButton',
   );

   loader = SelectDataTest('RemoveStageConfirmModal:Loader');

   async exists(t) {
      await t.expect(this.wrapper.exists).ok('', { timeout: 120000 }); // всё время падает!
   }

   async notExists(t) {
      await t.expect(this.wrapper.exists).notOk('', { timeout: 120000 });
   }

   async confirm(t) {
      await this.exists(t);

      // Ждём, когда пропадет индикатор загрузки (Проверка AWACS)
      await t.expect(this.loader.exists).notOk('', { timeout: 120000 }); // падает

      await this.checkbox.check(t);

      await this.okButton.click(t);
   }
}
