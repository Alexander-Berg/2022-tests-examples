import { Int32 } from '../../../../../common/ys'
import { ConditionEvaluator } from '../../../code/expression/condition-evaluator'

import { LanguageKind } from '../../../code/expression/condition-parameter'
import { Variable, Version } from '../../../code/expression/variable'

interface ConditionParameters {
  lang(): LanguageKind
  osVersion(): Version
  appBuildNumber(): Int32
  hasTeamAccount(): boolean
  hasYandexoidAccount(): boolean
  applicationId(): string
  uuidHashMod100(): Int32
  uuid(): string
  list(): string[]
  map(): Map<string, string>
}

class ConditionParameterNames {
  public static readonly lang: string = 'lang'
  public static readonly osVersion: string = 'osVersion'
  public static readonly appBuildNumber: string = 'appBuildNumber'
  public static readonly hasTeamAccount: string = 'hasTeamAccount'
  public static readonly hasYandexoidAccount: string = 'hasYandexoidAccount'
  public static readonly applicationId: string = 'applicationId'
  public static readonly uuidHashMod100: string = 'uuidHashMod100'
  public static readonly uuid: string = 'uuid'
  public static readonly list: string = 'list'
  public static readonly map: string = 'map'

  public static init(params: ConditionParameters): Map<string, Variable> {
    const values = new Map<string, Variable>()
    values.set(ConditionParameterNames.lang, Variable.string(params.lang().toString()))
    values.set(ConditionParameterNames.osVersion, Variable.version(params.osVersion()))
    values.set(ConditionParameterNames.appBuildNumber, Variable.int(params.appBuildNumber()))
    values.set(ConditionParameterNames.hasTeamAccount, Variable.boolean(params.hasTeamAccount()))
    values.set(ConditionParameterNames.hasYandexoidAccount, Variable.boolean(params.hasYandexoidAccount()))
    values.set(ConditionParameterNames.applicationId, Variable.string(params.applicationId()))
    values.set(ConditionParameterNames.uuidHashMod100, Variable.int(params.uuidHashMod100()))
    values.set(ConditionParameterNames.uuid, Variable.string(params.uuid()))
    values.set(ConditionParameterNames.list, Variable.array(params.list()))
    values.set(ConditionParameterNames.map, Variable.map(params.map()))
    return values
  }
}

describe('Condition evaluator', () => {
  const params: ConditionParameters = {
    lang(): LanguageKind {
      return LanguageKind.Ru
    },
    osVersion(): Version {
      return new Version('1.0')
    },
    appBuildNumber(): Int32 {
      return 1000
    },
    hasTeamAccount(): boolean {
      return true
    },
    hasYandexoidAccount(): boolean {
      return true
    },
    applicationId(): string {
      return 'ru.yandex.mail'
    },
    uuidHashMod100(): Int32 {
      return 49
    },
    uuid(): string {
      return 'abyrvalg'
    },
    list(): string[] {
      return ['one', 'two', 'three']
    },
    map(): Map<string, string> {
      return new Map([
        ['one', 'two'],
        ['three', 'four'],
      ])
    },
  }
  const conditionEvaluator = new ConditionEvaluator(ConditionParameterNames.init(params))

  it('should return true for lang ru', () => {
    expect(conditionEvaluator.evaluate('lang == "ru"')).toBe(true)
  })

  it('should return false for lang ua', () => {
    expect(conditionEvaluator.evaluate('lang == "ua"')).toBe(false)
  })

  it('should return true for appBuildNumber >= 1000', () => {
    expect(conditionEvaluator.evaluate('appBuildNumber >= 1000')).toBe(true)
  })

  it('should return false for appBuildNumber < 1000', () => {
    expect(conditionEvaluator.evaluate('appBuildNumber < 1000')).toBe(false)
  })

  it('should return true for osVersion == 1.0', () => {
    expect(conditionEvaluator.evaluate('osVersion == v("1.0")')).toBe(true)
  })

  it('should return false for osVersion == 1.1', () => {
    expect(conditionEvaluator.evaluate('osVersion == v("1.1")')).toBe(false)
  })

  it('should compare versions', () => {
    expect(conditionEvaluator.evaluate('osVersion < v("1.1")')).toBe(true)
    expect(conditionEvaluator.evaluate('osVersion < v("0.9")')).toBe(false)
  })

  it('should return error for incorrect versions comparison', () => {
    expect(() => conditionEvaluator.evaluate('osVersion < v("1.x")')).toThrowError(
      'Incorrect format for Variable: "<Variable type: version, value: <JSONItem kind: string, value: "1.x">>"',
    )
  })

  it('should return true for hasTeamAccount', () => {
    expect(conditionEvaluator.evaluate('hasTeamAccount')).toBe(true)
  })

  it('should return true for hasYandexoidAccount', () => {
    expect(conditionEvaluator.evaluate('hasYandexoidAccount')).toBe(true)
  })

  it('should return true for applicationId', () => {
    expect(conditionEvaluator.evaluate('applicationId == "ru.yandex.mail"')).toBe(true)
  })

  it('should return true if uuid hash % 100 < 50', () => {
    expect(conditionEvaluator.evaluate('uuidHashMod100 < 50')).toBe(true)
  })

  it('should return false if uuid hash % 100 >= 50', () => {
    expect(conditionEvaluator.evaluate('uuidHashMod100 >= 50')).toBe(false)
  })

  it('should return true if uuid is abyrvalg', () => {
    expect(conditionEvaluator.evaluate('uuid == "abyrvalg"')).toBe(true)
  })

  it('should return false if uuid is empty', () => {
    expect(conditionEvaluator.evaluate('uuid == ""')).toBe(false)
  })

  it('should return error for bad condition', () => {
    expect(() => conditionEvaluator.evaluate('osVersion == 1.1')).toThrowError('Incompatible types version and double')
  })

  it('should work with `has`, `in`, `not in` for list', () => {
    expect(conditionEvaluator.evaluate('list has "one"')).toBe(true)
    expect(conditionEvaluator.evaluate('list has "four"')).toBe(false)
    expect(conditionEvaluator.evaluate('"one" in list')).toBe(true)
    expect(conditionEvaluator.evaluate('"four" in list')).toBe(false)
    expect(conditionEvaluator.evaluate('"four" not in list')).toBe(true)
    expect(conditionEvaluator.evaluate('"one" not in list')).toBe(false)
  })

  it('should work with `in`, `not in` for map', () => {
    expect(conditionEvaluator.evaluate('"one" in map')).toBe(true)
    expect(conditionEvaluator.evaluate('"four" in map')).toBe(false)
    expect(conditionEvaluator.evaluate('"four" not in map')).toBe(true)
    expect(conditionEvaluator.evaluate('"one" not in map')).toBe(false)
  })

  it('should work with `of` for map', () => {
    expect(conditionEvaluator.evaluate('"one" of map == "two"')).toBe(true)
    expect(conditionEvaluator.evaluate('"three" of map == "four"')).toBe(true)
    expect(conditionEvaluator.evaluate('"one" of map != "three"')).toBe(true)
    expect(conditionEvaluator.evaluate('"two" of map == ""')).toBe(true)
  })

  it('should work with `in`, `not in`, `of` + `[]`', () => {
    expect(conditionEvaluator.evaluate('"one" of map in ["two", "three"]')).toBe(true)
    expect(conditionEvaluator.evaluate('"three" of map not in ["one", "two"]')).toBe(true)
    expect(conditionEvaluator.evaluate('["three", "four"] has "one" of map')).toBe(false)
  })

  it('should return error for invalid conditions', () => {
    expect(() => conditionEvaluator.evaluate('"one" of lang')).toThrowError('Incompatible types string and string')
    expect(() => conditionEvaluator.evaluate('"key" of "one"')).toThrowError('Incompatible types string and string')
    expect(() => conditionEvaluator.evaluate('"key" of hasYandexoidAccount')).toThrowError(
      'Incompatible types string and boolean',
    )
    expect(() => conditionEvaluator.evaluate('list of hasYandexoidAccount')).toThrowError(
      'Incompatible types array and boolean',
    )
    expect(() => conditionEvaluator.evaluate('osVersion has "one"')).toThrowError(
      'Incompatible types version and string',
    )
    expect(() => conditionEvaluator.evaluate('"one" in uuidHashMod100')).toThrowError(
      'Incompatible types int and string',
    )
    expect(() => conditionEvaluator.evaluate('map not in hasTeamAccount')).toThrowError(
      'Incompatible types boolean and map',
    )
    expect(() => conditionEvaluator.evaluate('"foo" of not (in list "not") in ["bar"]')).toThrowError(
      "Unknown operation '('",
    )
  })
})
