import { getDataE2eSelector, getDataQaSelector } from '../helpers/extractors';

import { YCSuggest } from './components/YcSuggest';
import { Button } from './components/Button';

export class FormNewYasmTags {
   constructor(yasm) {
      this.itype = new YCSuggest(yasm.find(getDataE2eSelector('YasmTags:Itype')));

      this.buttons = {
         addTag: new Button(yasm.find(getDataQaSelector('YasmTags:AddTag'))),
      };

      this.tag = (n = 1) => {
         const tag = yasm.find(getDataE2eSelector('YasmTag')).nth(n - 1);

         return {
            key: new YCSuggest(tag.find(getDataE2eSelector('YasmTag:Key'))),
            value: new YCSuggest(tag.find(getDataE2eSelector('YasmTag:Value'))),

            buttons: {
               removeTag: new Button(tag.find(getDataE2eSelector('YasmTag:RemoveTag'))),
            },
         };
      };

      this.actions = {
         // countTags: async () => {
         //    const count = await yasm.find(getDataE2eSelector('YasmTag')).count;

         //    return count;
         // },

         getTagIndex: async (key = '') => {
            const count = await yasm.find(getDataE2eSelector('YasmTag')).count;

            for (let i = 0; i < count; i++) {
               if (await yasm.find(getDataE2eSelector('YasmTag')).nth(i).find(`input[value="${key}"]`).exists) {
                  return i + 1;
               }
            }

            return undefined;
         },
      };
   }
}
