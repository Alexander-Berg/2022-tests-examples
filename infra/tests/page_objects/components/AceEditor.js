import { BaseComponent } from './BaseComponent';

export class AceEditor extends BaseComponent {
   constructor(element) {
      super(element);
      this.element = element;
      this.aceEditor = element.find('.ace_editor');
      this.aceGutter = this.aceEditor.find('.ace_gutter');
   }

   typeText = async (t, text) => {
      await this.findElement(t, this.aceEditor);
      await t
         .click(this.aceGutter)
         .pressKey('shift+home delete shift+end delete')
         .pressKey(
            text
               .split(' ')
               .map(string => string.split('').join(' '))
               .join(' space '),
         );
   };

   containsText = async (t, value) => {
      await t
         .expect(
            this.element.find('.ace_content').find('.ace_layer.ace_text-layer').find('.ace_line').withText(value)
               .exists,
         )
         .eql(true);
   };

   /*

    readIdentifier = async (t, identifier) => {
        await t.expect(this.aceEditor.find('.ace_layer.ace_text-layer').find('.ace_line').find('.ace_identifier').withValue(identifier).exists).eql(true);
    }

    readString = async (t, string) => {
        await t.expect(this.aceEditor.find('.ace_layer.ace_text-layer').find('.ace_line').find('.ace_string').withValue(string).exists).eql(true);
    }

    */

   /*

    .ace_keyword
    .ace_operator
    .ace_identifier
    .ace_constant
    .ace_numeric
    .ace_string
    .ace_start
    .ace_end

    <div class="ace_content">
        <div class="ace_layer ace_text-layer">
            <div class="ace_line_group">
                <div class="ace_line">
                    <span class="ace_keyword ace_operator">/</span>
                    <span class="ace_identifier">simple_http_server</span>
                    <span class="ace_constant ace_numeric">8228</span>
                    <span class="ace_string ace_start">'</span>
                    <span class="ace_string">Test #1579091716480</span>
                    <span class="ace_string ace_end">'</span>
                </div>
            </div>
        </div>
    </div>

    <div class="ace_content">
        <div class="ace_layer ace_text-layer">
            <div class="ace_line_group">
                <div class="ace_line">
                    debug test message (1587407924862)
                </div>
            </div>
        </div>
    </div>

    */
}
