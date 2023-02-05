import { Email, EmailWithName } from '../../../../../code/api/entities/recipient/email'

describe(Email, () => {
  it('should be constructible from string', () => {
    expect(Email.fromString('login@domain.ru')).toEqual(new Email('login', 'domain.ru'))
    expect(Email.fromString('')).toBeNull()
  })
  it('should be representable as string', () => {
    expect(new Email('login', 'domain.ru').asString()).toBe('login@domain.ru')
    expect(new Email('', '').asString()).toBe('')
    expect(new Email('login', '').asString()).toBe('')
    expect(new Email('', 'domain').asString()).toBe('')
  })
})

describe(EmailWithName, () => {
  it('should be constructible from string', () => {
    expect(EmailWithName.fromString('Sample <sample@yandex.ru>')).toMatchObject({
      login: 'sample',
      domain: 'yandex.ru',
      name: 'Sample',
    })
    expect(EmailWithName.fromString('"Name Surname" <sample@yandex.ru>')).toMatchObject({
      login: 'sample',
      domain: 'yandex.ru',
      name: 'Name Surname',
    })
    expect(EmailWithName.fromString('sample@yandex.ru')).toMatchObject({
      login: 'sample',
      domain: 'yandex.ru',
      name: null,
    })
    expect(EmailWithName.fromString('<sample@yandex.ru>')).toMatchObject({
      login: 'sample',
      domain: 'yandex.ru',
      name: '',
    })
    expect(EmailWithName.fromString('"" <sample@yandex.ru>')).toMatchObject({
      login: 'sample',
      domain: 'yandex.ru',
      name: '',
    })
  })
  it('should be constructible from name and email', () => {
    expect(EmailWithName.fromNameAndEmail(null, '')).toMatchObject({
      login: '',
      domain: '',
      name: null,
    })
    expect(EmailWithName.fromNameAndEmail('Name Surname', 'sample@yandex.ru')).toMatchObject({
      login: 'sample',
      domain: 'yandex.ru',
      name: 'Name Surname',
    })
    expect(EmailWithName.fromNameAndEmail(null, 'sample@yandex.ru')).toMatchObject({
      login: 'sample',
      domain: 'yandex.ru',
      name: null,
    })
    expect(EmailWithName.fromNameAndEmail('', 'sample@yandex.ru')).toMatchObject({
      login: 'sample',
      domain: 'yandex.ru',
      name: '',
    })
    expect(EmailWithName.fromNameAndEmail('', '')).toMatchObject({
      login: '',
      domain: '',
      name: '',
    })
  })
  it('should be stringifiable', () => {
    expect(EmailWithName.fromNameAndEmail(null, '').asString()).toBe('')
    expect(EmailWithName.fromNameAndEmail('Name Surname', 'sample@yandex.ru').asString()).toBe(
      '"Name Surname" <sample@yandex.ru>',
    )
    expect(EmailWithName.fromNameAndEmail(null, 'sample@yandex.ru').asString()).toBe('sample@yandex.ru')
    expect(EmailWithName.fromNameAndEmail('', 'sample@yandex.ru').asString()).toBe('sample@yandex.ru')
    expect(EmailWithName.fromNameAndEmail('', '').asString()).toBe('')
  })
  it('should provide email', () => {
    expect(EmailWithName.fromNameAndEmail(null, '').toEmail()).toBe('')
    expect(EmailWithName.fromNameAndEmail('Name Surname', 'sample@yandex.ru').toEmail()).toBe('sample@yandex.ru')
    expect(EmailWithName.fromNameAndEmail(null, 'sample@yandex.ru').toEmail()).toBe('sample@yandex.ru')
    expect(EmailWithName.fromNameAndEmail('', 'sample@yandex.ru').toEmail()).toBe('sample@yandex.ru')
    expect(EmailWithName.fromNameAndEmail('', '').toEmail()).toBe('')
  })
})
