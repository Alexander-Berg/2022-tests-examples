import {
  doubleToInt32,
  floorDouble,
  Int32,
  int32ToDouble,
  int64,
  Int64,
  randomDouble,
  Throwing,
} from '../../../../common/ys'
import { stringReplaceAll } from '../../../common/code/utils/strings'
import { ID } from '../../../mapi/code/api/common/id'
import { Email } from '../../../mapi/code/api/entities/recipient/email'
import { copyArray } from '../../../testopithecus-common/code/utils/utils'
import { FolderName } from '../mail/feature/folder-list-features'

export function reduced(id: ID): string {
  const s = id.toString()
  return s.slice(s.length - 3, s.length)
}

export function getRandomInt32(max: Int32): Int32 {
  return doubleToInt32(floorDouble(randomDouble() * int32ToDouble(max))) % max
}

export function display(email: Email): string {
  // TODO toString function in Email
  return `${email.login}@${email.domain}`
}

export function formatFolderName(folderName: FolderName, parentFolders: FolderName[] = []): FolderName {
  const result = copyArray(parentFolders)
  result.push(folderName)
  return result.join('|')
}

export function fakeMid(): Int64 {
  return int64(-1)
}

export function resolveThrow<T>(action: () => Throwing<T>, defVal: T): T {
  try {
    return action()
  } catch (e) {
    return defVal
  }
}

export function removeAllNonLetterSymbols(str: string): string {
  const tags = ['<br />', '<p>', '</p>', '<br>', '\r', '\n']
  let result = str
  tags.forEach((tag) => {
    result = stringReplaceAll(result, tag, '')
  })
  return result
}

export function formatLogin(login: string): string {
  // passport performs yandex-team-47907-42601@yandex.ru -> yandex-team-47907.42601@yandex.ru
  const index = login.lastIndexOf('-')
  return `${login.slice(0, index)}.${login.slice(index + 1, login.length)}`
}

export class TextGenerator {
  public static basicLatin: string =
    'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_-+=/|~:; '
  public static lowerCaseLatin: string = 'abcdefghijklmnopqrstuvwxyz'

  public generateRandomString(charSet: string, length: Int32): string {
    const result: string[] = []
    while (result.length < length) {
      result.push(charSet.split('')[getRandomInt32(charSet.length)])
    }
    return result.join('')
  }
}
