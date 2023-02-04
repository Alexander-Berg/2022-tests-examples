import { AceEditor } from './AceEditor';

export class AceEditorSh extends AceEditor {
   constructor(element) {
      super(element);
      this.element = element;
      this.aceEditor = element.find('.ace_editor');
      this.aceGutter = this.aceEditor.find('.ace_gutter');
   }

   containsText = async (t, value) => {
      await t
         .expect(
            this.element
               .find('.ace_content')
               .find('.ace_layer.ace_text-layer')
               .find('.ace_line')
               .find('span')
               .withText(value).exists,
         )
         .eql(true);
   };
}
