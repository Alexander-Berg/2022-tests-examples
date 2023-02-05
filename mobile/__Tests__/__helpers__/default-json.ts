import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { JSONItem } from '../../../common/code/json/json-types'
import { Result } from '../../../common/code/result/result'
import { JSONItemFromJSON, JSONItemToJSONString } from './json-helpers'

export class DefaultJSONSerializer implements JSONSerializer {
  public deserialize(item: string): Result<JSONItem> {
    const jsonObj = JSON.parse(item)
    const value = this.parse(jsonObj)
    return new Result(value, null)
  }

  public serialize(item: JSONItem): Result<string> {
    return new Result<string>(JSONItemToJSONString(item), null)
  }

  public parse(element: any): JSONItem {
    return JSONItemFromJSON(element)
  }
}
