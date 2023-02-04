import { AceEditor } from './AceEditor';

export class AceEditorJson extends AceEditor {
   constructor(element) {
      super(element);
      this.element = element;
      this.aceEditor = element.find('.ace_editor');
      this.aceGutter = this.aceEditor.find('.ace_gutter');
      this.aceLine = this.aceEditor.find('.ace_content').find('.ace_layer.ace_text-layer').find('.ace_line');
   }

   containsKeyValue = async (t, key, value) => {
      const line = this.aceLine.find('span.ace_variable').withText(key).nextSibling('span').withText(value);

      await t.expect(line.exists).eql(true);
   };

   /*
    
    <div class="ace_line">
        <span class="ace_variable">"box"</span>:
        <span class="ace_string">"Box1"</span>,
    </div>
    <div class="ace_line">
        <span class="ace_variable">"box"</span>:
        <span class="ace_constant ace_language ace_boolean">true</span>,
    </div>
    
    */
}
