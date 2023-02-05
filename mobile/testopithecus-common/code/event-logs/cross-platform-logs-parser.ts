import { JSONSerializer } from '../../../common/code/json/json-serializer'
import { MapJSONItem, StringJSONItem } from '../../../common/code/json/json-types'
import { EventusEvent, EventusConstants } from '../../../eventus-common/code/eventus-event'
import { requireNonNull } from '../utils/utils'

export class CrossPlatformLogsParser {
  public constructor(private jsonSerializer: JSONSerializer) {}

  public parse(line: string): EventusEvent {
    const json = this.jsonSerializer.deserialize(line).getValue() as MapJSONItem
    const value = requireNonNull(json.getMap('value'), 'Нет аттрибутов у эвента!')
    const eventusName = (requireNonNull(
      value.get('event_name'),
      'Имя евента должно быть в аттрибутах',
    ) as StringJSONItem).value
    const loggingName = requireNonNull(json.getString('name'), 'Имя эвента должно быть в имени эвента')
    if (loggingName !== `${EventusConstants.PREFIX}${eventusName}`) {
      throw new Error('Плохое имя')
    }
    // eslint-disable-next-line no-underscore-dangle
    return EventusEvent.fromMap(eventusName, value)
  }
}
