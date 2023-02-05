import { Int32 } from '../../../../common/ys'
import { PseudoRandomProvider } from '../utils/pseudo-random'
import { randomInt, RandomProvider } from '../utils/random'
import { fuzzStrings } from './fuzz-strings'
import { fuzzInvalidEmails, fuzzValidEmails } from './fuzzemails'

export class Fuzzer {
  private static unicodeMix: string[] = '๏̯͡๏斯坦尼斯会文社═╬╬═۩۞۩★★★▀▄▀▄▀▄▀▄۞۞۞இஇஇ®®®√√√๑۩۩๑¤¤¤♂♂♂•••ツツツ●•●•♡♡♡♥♥♥ღღღ♀♀♀♫♫♫₪₪₪øøø♪♪♪ஐஐஐ↕↕↕˜”*°•..•°*”˜ששששש☻☻☻تتت˙˙·.ૐ╬╬╬٭٭٭◘◘◘❖❖❖♣♣♣ύύύ†††☆☆☆ΩΩΩ™①①①♠♠♠█▌○○○☺☺☺ټټﻩﻩﻩ*ﻩ*ﻩ*ﻩ*ﻩ*ﻩﻩﻩ☼☼عتبررفيقة,أناأنتيتلقّىتبحثل,ويحافظأنتيسكت¶¶¶▼▼◄◄►►■«»©©©░░░░░<<>>%$$$###№@@@"""!~````^^&&???***((()))__---++===///||||░▒▓██▓▒░☀☂♂☻♥╝╝╝ЬЬЬ╕╕╕◘◘◘♣♠♦○○♣♦☻☺000♥♣M♣♣55U╒◙j[♀+♂=♥]™͡๏̯͡๏‡╥█◘(•̪●)◗◖◕◔◓◒▲△▴▵▶▷▸▹►▻▼▽▾▿◀◁◂◃◄◅◆◇◈◉◊○◌◍◎●◐◑◯◮◭◬◫◪◩◨◧■□▢▣▤▥▦▧▨▩▪▫▬▭▮▯▰▱▓▒░▐▏▎▍▌▋▊▉█▂▃▄▅▆▇█▒▓╴╵╶╷╸╹╺╻╼╽╾╿┇┆┅┄┃│╇╆╅╄╃╂╁╀┿┾┽┼┻┺┹┸┷┶┵┴┳┲┱┰┯┮┭┬┫┪┩┨┧┦┥┤┣┢┡┠┟┞┝├┛┚┙┘┗┖┕└┓┒┑┐┏┎┍┌┋┊┉┈┇┆┅╬╫╪╩╨╧╦╥╤╣╢╡╠╟╞╝╜╛╚╙╘╗╖╕╔╓╒║═╏╎╍╌╋╊╉╈༼༽༾༿‣•⑊⑉⑈⑇⑆〭〯〮〬◦〪〫❝❜❛❞₪۩๑¤۞‾□▪▫◊●◦•۝ʻʼʽʾʿˀˁ˂˃˄˅ˆˇˈˉˊˋˌˍˎˏːˑ˒˓˔˕˖˗˘˙˚˛˜˝˞ˠˡˡˢˣˤ̙̘̗̖̔̒̓̑̐̏̎̍̌̋̊̉̈̇̆̅̄̃̂́̀̚̕˩˨˧˦˥̸̡̢̧̨̛̜̝̟̠̣̤̥̦̩̪̫̬̭̮̯̰̱̲̹̺։ְֱֲֳִֵֶַָ֑֖֛֥֦֧֪֚֭֮֒֓֔֕֗֘֙֜֝֞֟֠֡֨֩֫֬֯׃٠٭۝๏།༎༏༓༗༘༙༚༛༜༝༞༟༶༷༵‼‽‖'.split(
    '',
  )
  private static unicodeParentheses: string[] = '︵︶︷︸︹︺︿﹀︽︾'.split('')
  private static unicodeSmile: string[] = 'ソッヅツゾシジｯﾂｼﾝ〴〳〵〲〱〷〥〤〡ٺقتثةت'.split('')
  private static unicodeZodiac: string[] = '♈♉♊♋♌♍♎♏♐♑♒♓'.split('')
  private static unicodeOther: string[] = '✽✾✿❀❁❂❃❄❅❆❇❈❉❊❋♠♡♢♣♤♥♦♧♂♁♀☿♃♄♅♆♇♩♪♫♬♭♮♯☂☃☁☀ﻩ*⁂☚☛☜☝☞☟✌➳❤❣❢ஐஇఞఎയ✁✄✉✔✓☐☑☒✪★☆〠☯☮☭☄☊☣☢☤☬☫☪☨☦☧☥〄〩❦❧♨☸☠✆☎☏‼⌚⌛☡�💩'.split(
    '',
  )
  private static unicodeAll: string[][] = [
    Fuzzer.unicodeMix,
    Fuzzer.unicodeParentheses,
    Fuzzer.unicodeSmile,
    Fuzzer.unicodeZodiac,
    Fuzzer.unicodeOther,
  ]

  public constructor(private random: RandomProvider = PseudoRandomProvider.INSTANCE) {}

  public naughtyString(minLength: Int32): string {
    const result: string[] = []
    let length: Int32 = 0
    while (length < minLength) {
      const str = fuzzStrings()[randomInt(this.random, 0, fuzzStrings().length)]
      result.push(str)
      length += str.split('').length
    }
    return result.join('')
  }

  public unicodeString(length: Int32): string {
    const result: string[] = []
    while (result.length < length) {
      const unicode = Fuzzer.unicodeAll[randomInt(this.random, 0, Fuzzer.unicodeAll.length)]
      result.push(unicode[randomInt(this.random, 0, unicode.length)])
    }
    return result.join('')
  }

  public fuzzyTitle(random: RandomProvider, minLength: Int32 = 100): string {
    if (randomInt(random, 0, 2) === 0) {
      return this.naughtyString(minLength)
    } else {
      return this.unicodeString(minLength)
    }
  }

  public fuzzyBody(random: RandomProvider, minLength: Int32 = 10000): string {
    return this.fuzzyTitle(random, minLength)
  }

  public fuzzyAttachment(random: RandomProvider, type: AttachmentType = AttachmentType.ZIP): string {
    const filename = this.naughtyString(20)
    return `/c/attachment/${type}/${filename}`
  }

  public fuzzyValidEmail(): string {
    return fuzzValidEmails()[randomInt(this.random, 0, fuzzValidEmails().length)]
  }

  public fuzzyInvalidEmail(): string {
    return fuzzInvalidEmails()[randomInt(this.random, 0, fuzzInvalidEmails().length)]
  }
}

export enum AttachmentType {
  ZIP,
}
