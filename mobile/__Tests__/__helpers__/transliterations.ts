import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { DefaultTransliterationModel } from '../../code/busilogics/transliteration/default-transliteration-model'
import { TransliterationModel } from '../../code/busilogics/transliteration/transliteration-model'

export function getTransliteration(): TransliterationModel {
  // const lists = new Map<string, readonly string[]>().set('301', ['Родди', 'Родерик'])
  // const names =
  //   new Map<string, readonly Int32[]>()
  //     .set('roddi', [301])
  //     .set('ро', [352, 67, 6, 295, 359, 9, 41, 171, 204, 301, 315, 339, 86, 24, 155])
  //     .set('ра', [128, 6, 9, 267, 145, 24, 155, 33, 289, 41, 298, 171, 300,
  //                301, 71, 330, 339, 213, 86, 95, 352, 359, 369, 244])
  //     .set('ro', [6, 9, 301, 86, 24, 155])
  //     .set('ру', [67, 359, 9, 233, 139, 204, 301, 18, 155])
  //     .set('rod', [24, 301])
  //     .set('rody', [24, 301])
  //     .set('rodj', [24, 301])
  //     .set('род', [24, 67, 301])
  //     .set('роди', [24, 301])
  //     .set('рад', [24, 301])
  //     .set('фр', [233, 269, 301, 209, 278, 24, 319])
  //     .set('фро', [24, 301, 269])
  //     .set('фрод', [24, 301])
  //     .set('руд', [67, 301])
  // const alternatives = new TransliterationAlternatives(lists, names)
  const serializer: JSONSerializer = {
    serialize: jest.fn(),
    deserialize: jest.fn(),
  }

  return new DefaultTransliterationModel('', serializer)
}
