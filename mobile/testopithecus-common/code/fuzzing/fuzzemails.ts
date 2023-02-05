// see: https://gist.github.com/cjaoude/fd9910626629b53c4d25
// also: https://en.wikipedia.org/wiki/Email_address#Valid_email_addresses

export function fuzzValidEmails(): string[] {
  return [
    'email@example.com',
    'firstname.lastname@example.com',
    'email@subdomain.example.com',
    'firstname+lastname@example.com',
    'email@123.123.123.123',
    'email@[123.123.123.123]',
    '"email"@example.com',
    '1234567890@example.com',
    'email@example-one.com',
    '_______@example.com',
    'email@example.name',
    'email@example.museum',
    'email@example.co.jp',
    'firstname-lastname@example.com',
    'much.”moreunusual”@example.com',
    'very.unusual.””.unusual.com@example.com',
    "a!#$%&'*+—/=?^_`{|}~1s.dv@12345-12345-12345-12345-12345-12345.ru",
    'あいうえお@example.com',
  ]
}

export function fuzzInvalidEmails(): string[] {
  return [
    'plainaddress',
    '#@%^%#$@#$@#.com',
    '@example.com',
    'Joe Smith <email@example.com>',
    'email.example.com',
    'email@example@example.com',
    '.email@example.com',
    'email.@example.com',
    'email..email@example.com',
    'email@example.com (Joe Smith)',
    'email@example',
    'email@-example.com',
    'email@example.web',
    'email@111.222.333.44444',
    'email@example..com',
    'Abc..123@example.com',
    '”(),:;<>[]@example.com',
  ]
}
