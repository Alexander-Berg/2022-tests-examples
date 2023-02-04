import { BaseComponent } from './BaseComponent';

export class MonacoEditor extends BaseComponent {
   constructor(element) {
      super(element);
      this.element = element;
      this.monacoEditor = element.find('.monaco-editor');
      this.viewLine = this.monacoEditor.find('.view-lines').find('.view-line');
   }

   typeText = async (t, text) => {
      await this.findElement(t, this.viewLine);
      await t
         .click(this.viewLine)
         .pressKey('shift+up delete shift+down delete')
         // .pressKey('shift+home delete shift+end delete')
         .click(this.viewLine)
         .pressKey(
            text
               .split(' ')
               .map(string => string.split('').join(' '))
               .join(' space '),
         );
   };

   pressKey = async (t, key) => {
      await this.findElement(t, this.viewLine);
      await t.click(this.viewLine).pressKey(key);
   };

   readValue = async (t, value) => {
      // ...
   };

   /*

<div class="view-line">
   <span>
      <span class="mtk10">logger_name</span>
      <span class="mtk1">&nbsp;=&nbsp;logger_name_1,&nbsp;logger_name_2,&nbsp;logger_name_3;&nbsp;</span>
   </span>
</div>

<div class="view-line">
   <span>
      <span class="mtk14">box</span>
      <span class="mtk1">!=test;</span>
      
      <span class="mtk14">host</span
      <span class="mtk1">=host;</span>
   </span>
</div>

<div class="view-line">
   <span>
      <span class="mtk14">box</span>
      <span class="mtk1">!=test;</span>
   </span>
</div>
<div class="view-line">
   <span>
      <span class="mtk14">host</span
      <span class="mtk1">=host;</span>
   </span>
</div>

*/
}
