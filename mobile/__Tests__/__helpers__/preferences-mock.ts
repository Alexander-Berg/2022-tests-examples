import { Double, Int32, Int64, Nullable } from '../../../../common/ys'
import { SharedPreferences, SharedPreferencesEditor } from '../../code/shared-prefs/shared-preferences'

export class MockSharedPreferences implements SharedPreferences {
  public constructor(public readonly map: Map<string, any> = new Map()) {}

  public getInt32(key: string, def: Int32): Int32 {
    return this.contains(key) ? this.map.get(key) : def
  }
  public getInt64(key: string, def: Int64): Int64 {
    return this.contains(key) ? this.map.get(key) : def
  }
  public getBoolean(key: string, def: boolean): boolean {
    return this.contains(key) ? this.map.get(key) : def
  }
  public getDouble(key: string, def: Double): Double {
    return this.contains(key) ? this.map.get(key) : def
  }
  public getString(key: string, def: Nullable<string>): Nullable<string> {
    return this.contains(key) ? this.map.get(key) : def
  }
  public getStringSet(key: string, def: ReadonlySet<string>): ReadonlySet<string> {
    return this.contains(key) ? this.map.get(key) : def
  }
  public getAll(): ReadonlyMap<string, any> {
    return this.map
  }
  public contains(key: string): boolean {
    return this.map.has(key)
  }

  public edit(): SharedPreferencesEditor {
    return new MockSharedPreferencesEditor(new Map(this.map), (result) => {
      this.map.clear()
      result.forEach((value, key) => this.map.set(key, value))
    })
  }
}

export class MockSharedPreferencesEditor implements SharedPreferencesEditor {
  public constructor(
    private readonly map: Map<string, any>,
    private readonly onSave: (map: ReadonlyMap<string, any>) => void,
  ) {}

  public putInt32(key: string, value: Int32): SharedPreferencesEditor {
    this.map.set(key, value)
    return this
  }

  public putInt64(key: string, value: Int64): SharedPreferencesEditor {
    this.map.set(key, value)
    return this
  }

  public putBoolean(key: string, value: boolean): SharedPreferencesEditor {
    this.map.set(key, value)
    return this
  }

  public putDouble(key: string, value: Double): SharedPreferencesEditor {
    this.map.set(key, value)
    return this
  }

  public putString(key: string, value: string): SharedPreferencesEditor {
    this.map.set(key, value)
    return this
  }

  public putStringSet(key: string, value: ReadonlySet<string>): SharedPreferencesEditor {
    this.map.set(key, value)
    return this
  }

  public remove(key: string): SharedPreferencesEditor {
    this.map.delete(key)
    return this
  }

  public clear(): SharedPreferencesEditor {
    this.map.clear()
    return this
  }

  public commit(): void {
    this.onSave(this.map)
  }

  public apply(): void {
    this.onSave(this.map)
  }
}
