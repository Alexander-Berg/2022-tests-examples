import { ID } from '../../../../mapi/code/api/common/id'
import { int64 } from '../../../../../common/ys'
import { formatFolderName } from '../../utils/mail-utils'
import { FolderName } from '../feature/folder-list-features'

export class DefaultFolderName {
  public static inbox: FolderName = 'Inbox'
  public static mailingLists: FolderName = 'Mailing lists'
  public static socialNetworks: FolderName = 'Social networks'
  public static sent: FolderName = 'Sent'
  public static outgoing: FolderName = 'Outbox'
  public static trash: FolderName = 'Trash'
  public static spam: FolderName = 'Spam'
  public static draft: FolderName = 'Drafts'
  public static template: FolderName = 'Templates'
  public static archive: FolderName = 'Archive'
}

export class FolderBackendName {
  public static inbox: FolderName = 'relevant'
  public static mailingLists: FolderName = 'news'
  public static socialNetworks: FolderName = 'social'
  public static templates: FolderName = 'Drafts|template'
}

export function formatFolderNameIfNeeded(folderName: FolderName): FolderName {
  switch (folderName) {
    case FolderBackendName.inbox:
      return DefaultFolderName.inbox
    case FolderBackendName.socialNetworks:
      return DefaultFolderName.socialNetworks
    case FolderBackendName.mailingLists:
      return DefaultFolderName.mailingLists
    case FolderBackendName.templates:
      return DefaultFolderName.template
    default:
      return folderName
  }
}

export function isTab(name: FolderName): boolean {
  return [FolderBackendName.socialNetworks, FolderBackendName.mailingLists, FolderBackendName.inbox].includes(name)
}

export function tabNameToFid(tabName: FolderName): ID {
  switch (tabName) {
    case FolderBackendName.inbox:
      return int64(-10)
    case FolderBackendName.mailingLists:
      return int64(-11)
    case FolderBackendName.socialNetworks:
      return int64(-12)
    default:
      throw new Error('This is no tab')
  }
}

export function toBackendFolderName(folderDisplayName: string, parentFolders: string[]): string {
  // TODO: реализовать, если мы захотим что-то делать на бэке с вложенными папками
  switch (folderDisplayName) {
    case DefaultFolderName.inbox:
      return 'Inbox'
    case DefaultFolderName.sent:
      return 'Sent'
    case DefaultFolderName.outgoing:
      return 'Outbox'
    case DefaultFolderName.trash:
      return 'Trash'
    case DefaultFolderName.spam:
      return 'Spam'
    case DefaultFolderName.draft:
      return 'Drafts'
    case DefaultFolderName.archive:
      return 'Archive'
    default:
      return formatFolderName(folderDisplayName, parentFolders)
  }
}
