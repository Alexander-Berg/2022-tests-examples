# coding: utf-8
__author__ = 'sandyk'

############################################################################################
mt940_transaction_template = '''
:61:{transaction_dt}{credit_type}{amount}NTRF //11
:86:~01{~01}~02{~02}~03{~03}~04{~04}~05{~05}~06{~06}~11~1320{transaction_dt}~1520{transaction_dt}~17{~17}~18{~18}~19{~19}'''

mt940_header_template = ''':20:STMT20{statement_dt}
:25:{account}
:28C:10022
:60F:C{statement_dt}EUR{start_amount}'''

mt940_footer_template = '''
:62F:C{statement_dt}EUR{end_amount}
:64:C{statement_dt}EUR{end_amount}
:86:NAME ACCOUNT OWNER:wwwww
ACCOUNT DESCRIPTION:  CURR
-'''

############################################################################################
iso_header_template = '''<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:camt.053.001.02" xmlns:xsi="http:?//www.w3.org/2001/XMLSchema-instance">
<BkToCstmrStmt>
<GrpHdr>
	<MsgId>IMBKRUMM677206{statement_dt_short}06:07:080003</MsgId>
	<CreDtTm>{statement_dt_long}T06:07:08+03:00</CreDtTm>
</GrpHdr>
<Stmt>
	<Id>677206RUR401102001/{statement_dt_long}/11</Id>
	<ElctrncSeqNb>11</ElctrncSeqNb>
	<CreDtTm>{statement_dt_long}T06:07:08+03:00</CreDtTm>
	<FrToDt><FrDtTm>{statement_dt_long}T00:00:00+03:00</FrDtTm><ToDtTm>{statement_dt_long}T00:00:00+03:00</ToDtTm></FrToDt>
	<Acct>
		<Id><Othr><Id>{account}</Id><SchmeNm><Cd>BBAN</Cd></SchmeNm></Othr></Id>
		<Ccy>{currency}</Ccy>
		<Ownr>
			<Nm>OOOY</Nm>
			<Id><OrgId><Othr><Id>677206</Id><SchmeNm><Cd>BANK</Cd></SchmeNm></Othr></OrgId></Id>
		</Ownr>
		<Svcr><FinInstnId><BIC>IMBKRUMM</BIC></FinInstnId></Svcr>
	</Acct>
	<Bal>
		<Tp><CdOrPrtry><Cd>OPBD</Cd></CdOrPrtry></Tp>
		<Amt Ccy="{currency}">{start_amount}</Amt>
		<CdtDbtInd>CRDT</CdtDbtInd>
		<Dt><Dt>{statement_dt_long}</Dt></Dt>
	</Bal>
	<Bal>
		<Tp><CdOrPrtry><Cd>CLBD</Cd></CdOrPrtry></Tp>
		<Amt Ccy="{currency}">{end_amount}</Amt>
		<CdtDbtInd>CRDT</CdtDbtInd>
		<Dt><Dt>{statement_dt_long}</Dt></Dt>
	</Bal>
	<TxsSummry>
		<TtlCdtNtries><NbOfNtries>1000</NbOfNtries><Sum>1000</Sum></TtlCdtNtries>
		<TtlDbtNtries><NbOfNtries>1000</NbOfNtries><Sum>1000</Sum></TtlDbtNtries>
	</TxsSummry>'''

iso_footer_template = '''
</Stmt>
</BkToCstmrStmt>
</Document>'''

iso_transaction_template = '''
<Ntry>
	<NtryRef>1</NtryRef>
	<Amt Ccy="{currency}">{amount}</Amt>
	<CdtDbtInd>{credit_type}</CdtDbtInd>
	<Sts>BOOK</Sts>
	<BookgDt><Dt>{transaction_dt}</Dt></BookgDt>
	<ValDt><Dt>{transaction_dt}</Dt></ValDt>
	<AcctSvcrRef>TQ6666666666</AcctSvcrRef>
	<BkTxCd>

		<Domn><Cd>PMNT</Cd><Fmly><Cd>ICDT</Cd><SubFmlyCd>NTAV</SubFmlyCd></Fmly></Domn>

		<Prtry><Cd>FTRF</Cd></Prtry>
	</BkTxCd>
	<NtryDtls>
		<TxDtls>
			<Refs><EndToEndId>22467</EndToEndId></Refs>
			<RltdPties>
				<Dbtr>
					<Nm>{~21}</Nm>
					<Id>
						<OrgId>
							<Othr><Id>{~22}</Id><SchmeNm><Cd>TXID</Cd></SchmeNm></Othr>
						</OrgId>
					</Id>
				</Dbtr>
				<DbtrAcct><Id><Othr><Id>{~23}</Id><SchmeNm><Cd>BBAN</Cd></SchmeNm></Othr></Id></DbtrAcct>
				<Cdtr>
					<Nm>{~01}</Nm>
					<Id>
						<OrgId>
							<Othr><Id>{~02}</Id><SchmeNm><Cd>TXID</Cd></SchmeNm></Othr>
						</OrgId>
					</Id>
				</Cdtr>
				<CdtrAcct><Id><Othr><Id>{~03}</Id><SchmeNm><Cd>BBAN</Cd></SchmeNm></Othr></Id></CdtrAcct>
			</RltdPties>
			<RltdAgts>
				<DbtrAgt>
					<FinInstnId>
						<ClrSysMmbId><ClrSysId><Cd>RUCBC</Cd></ClrSysId><MmbId>{~24}</MmbId></ClrSysMmbId>
						<Nm>{~25}</Nm>
						<Othr><Id>{~26}</Id></Othr>
					</FinInstnId>
				</DbtrAgt>
				<CdtrAgt>
					<FinInstnId>
						<ClrSysMmbId><ClrSysId><Cd>RUCBC</Cd></ClrSysId><MmbId>{~04}</MmbId></ClrSysMmbId>
						<Nm>{~05}</Nm>
						<Othr><Id>{~11}</Id></Othr>
					</FinInstnId>
				</CdtrAgt>
			</RltdAgts>

			<RmtInf>
				<Ustrd>{~06}</Ustrd>

				<Strd>
					<RfrdDocInf><Tp><CdOrPrtry><Prtry>POD</Prtry></CdOrPrtry></Tp><RltdDt>{transaction_dt}</RltdDt></RfrdDocInf>				</Strd>
			</RmtInf>

		</TxDtls>
	</NtryDtls>
</Ntry>'''

############################################################################################
onec_header_template = '''1CClientBankExchange
ВерсияФормата=1.01
Кодировка=windows-1251
Отправитель=
Получатель=
ДатаСоздания={statement_dt}
ВремяСоздания=11:24:40
ДатаНачала={statement_dt}
ДатаКонца={statement_dt}
РасчСчет={account}
СекцияРасчСчет
ДатаНачала={statement_dt}
ДатаКонца={statement_dt}
РасчСчет={account}
НачальныйОстаток={start_amount}
ВсегоСписано=0
ВсегоПоступило=0
КонечныйОстаток={end_amount}
КонецРасчСчет'''

onec_footer_template = '''
КонецФайла'''

onec_transaction_template ='''
СекцияДокумент=Прочее
Номер=296041
Дата={transaction_dt}
Сумма={amount}
ПлательщикСчет={~23}
Плательщик={~21}
ПлательщикИНН={~22}
ПлательщикКПП={~27}
ПлательщикРасчСчет={~03}
ПлательщикБанк1={~25}
ПлательщикБИК={~24}
ПлательщикКорсчет={~26}
ПолучательСчет={~03}
{credit_type}={transaction_dt}
Получатель={~01}
ПолучательИНН={~02}
ПолучательКПП={~20}
ПолучательРасчСчет={~11}
ПолучательБанк1={~05}
ПолучательБИК={~04}
ПолучательКорсчет={~11}
ВидПлатежа=
ВидОплаты=01
Код=
СтатусСоставителя=
ПоказательКБК=
ОКАТО=
ПоказательОснования=
ПоказательПериода=
ПоказательНомера=
ПоказательДаты=
ПоказательТипа=
Очередность=
НазначениеПлатежа={~06}
КонецДокумента'''
############################################################################################