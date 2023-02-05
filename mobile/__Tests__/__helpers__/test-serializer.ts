import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { JSONItem } from '../../../common/code/json/json-types'
import { Result } from '../../../common/code/result/result'

export class TestSerializer implements JSONSerializer {
  public serialize(item: JSONItem): Result<string> {
    throw new Error('Method not implemented.')
  }
  public deserialize(item: string): Result<JSONItem> {
    throw new Error('Method not implemented.')
  }
}
