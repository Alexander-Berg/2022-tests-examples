// import {Selector} from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class Viewer extends BaseComponent {
   constructor(element) {
      super(element);
      this.diff = element.find('table.diff');
   }

   // table.diff diff-split
   // table.diff diff-unified

   // tbody.diff-hunk

   // tr.diff-line (history compare)
   // tr.diff-line.diff-line-normal
   // tr.diff-line diff-line-old-only
   // tr.diff-line.diff-line-new-only
   // tr.diff-line.diff-line-compare
   //   td.diff-code diff-code-normal
   //   td.diff-code diff-code-delete
   //   td.diff-code diff-code-insert

   field = async (t, field, value) => {
      if (value) {
         await t
            .expect(this.diff.find('td.diff-code-normal').withText(field).find('.token').withText(value).exists)
            .eql(true, `field: '${field}' field value not found: ${value}`);
      } else {
         await t
            .expect(this.diff.find('td.diff-code-normal').withText(field).exists)
            .eql(true, `field: '${field}' field not found`);
      }
   };

   fieldDeleted = async (t, field, value) => {
      if (value) {
         await t
            .expect(this.diff.find('td.diff-code-delete').withText(field).find('.token').withText(value).exists)
            .eql(true, `fieldDeleted: '${field}' field value not found: ${value}`);
      } else {
         await t
            .expect(this.diff.find('td.diff-code-delete').withText(field).exists)
            .eql(true, `fieldDeleted: '${field}' field not found`);
      }
   };

   fieldNotDeleted = async (t, field, value) => {
      if (value) {
         await t
            .expect(this.diff.find('td.diff-code-delete').withText(field).find('.token').withText(value).exists)
            .eql(false, `fieldNotDeleted: '${field}' field value not found: ${value}`);
      } else {
         await t
            .expect(this.diff.find('td.diff-code-delete').withText(field).exists)
            .eql(false, `fieldNotDeleted: '${field}' field not found`);
      }
   };

   fieldInserted = async (t, field, value) => {
      if (value) {
         await t
            .expect(this.diff.find('td.diff-code-insert').withText(field).find('.token').withText(value).exists)
            .eql(true, `fieldInserted: '${field}' field value not found: ${value}`);
      } else {
         await t
            .expect(this.diff.find('td.diff-code-insert').withText(field).exists)
            .eql(true, `fieldInserted: '${field}' field not found`);
      }
   };

   fieldNotInserted = async (t, field, value) => {
      if (value) {
         await t
            .expect(this.diff.find('td.diff-code-insert').withText(field).find('.token').withText(value).exists)
            .eql(false, `fieldNotInserted: '${field}' field value not found: ${value}`);
      } else {
         await t
            .expect(this.diff.find('td.diff-code-insert').withText(field).exists)
            .eql(false, `fieldNotInserted: '${field}' field not found`);
      }
   };
}
