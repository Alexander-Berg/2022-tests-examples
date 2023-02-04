test_ticket_description = '''
<#<pre wrap>
Здравствуйте!
Посмотрите, пожалуйста.
--
Cлужба поддержки Яндекса
-------------------
</pre>#><{""Original message:""
Original message:
> -----BEGIN PGP SIGNED MESSAGE-----
> Hash: SHA1
> 
> Notice ID: 7b495bb01770da4a204a
> Notice Date: 2019-02-27T08:06:50Z
> 
> YANDEX LLC
> 
> Dear Sir or Madam:
> 
> We are contacting you on behalf of Paramount Pictures Corporation (Paramount).  Under penalty of perjury, I assert that IP-Echelon Pty. Ltd., (IP-Echelon) is authorized to act on behalf of the owner of the exclusive copyrights that are alleged to be infringed herein.
> 
> IP-Echelon has become aware that the below IP addresses have been using your service for distributing video files, which contain infringing video content that is exclusively owned by Paramount.
> 
> IP-Echelon has a good faith belief that the Paramount video content that is described in the below report has not been authorized for sharing or distribution by the copyright owner, its agent, or the law.  I also assert that the information contained in this notice is accurate to the best of our knowledge.
> 
> We are requesting your immediate assistance in removing and disabling access to the infringing material from your network.  We also ask that you ensure the user and/or IP address owner refrains from future use and sharing of Paramount materials and property.
> 
> In complying with this notice, YANDEX LLC should not destroy any evidence, which may be relevant in a lawsuit, relating to the infringement alleged, including all associated electronic documents and data relating to the presence of infringing items on your network, which shall be preserved while disabling public access, irrespective of any document retention or corporate policy to the contrary.
> 
> Please note that this letter is not intended as a full statement of the facts; and does not constitute a waiver of any rights to recover damages, incurred by virtue of any unauthorized or infringing activities, occurring on your network.  All such rights, as well as claims for other relief, are expressly reserved.
> 
> Should you need to contact me, I may be reached at the following address:
> 
> Adrian Leatherland
> On behalf of IP-Echelon as an agent for Paramount
> Address: 7083 Hollywood Blvd., Los Angeles, CA 90028, United States
> Email: p2p@copyright.ip-echelon.com
> 
> 
> Evidentiary Information:
> Protocol: BITTORRENT
> Infringed Work: Catch Me If You Can
> Infringing FileName: Catch.Me.If.You.Can.2002.BDRip.720p.mkv
> Infringing FileSize: 8482935848
> Infringer's IP Address: 5.255.233.198
> Infringer's Port: 63132
> Initial Infringement Timestamp: 2019-02-27T08:06:44Z
> 
> 
> <?xml version="1.0" encoding="UTF-8"?>
> <Infringement xmlns="http://www.acns.net/ACNS" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.acns.net/ACNS http://www.acns.net/v1.2/ACNS2v1_2.xsd">
>   <Case>
>     <ID>7b495bb01770da4a204a</ID>
>     <Status>Open</Status>
>     <Severity>Normal</Severity>
>   </Case>
>   <Complainant>
>     <Entity>Paramount Pictures Corporation</Entity>
>     <Contact>IP-Echelon - Compliance</Contact>
>     <Address>6715 Hollywood Blvd
> Los Angeles CA 90028
> United States of America</Address>
>     <Phone>+1 (310) 606 2747</Phone>
>     <Email>p2p@copyright.ip-echelon.com</Email>
>   </Complainant>
>   <Service_Provider>
>     <Entity>YANDEX LLC</Entity>
>     <Email>abuse@yandex.ru</Email>
>   </Service_Provider>
>   <Source>
>     <TimeStamp>2019-03-10T08:06:44Z</TimeStamp>
>     <IP_Address>37.140.183.11</IP_Address>
>     <Port>63132</Port>
>     <Type>BitTorrent</Type>
>     <SubType BaseType="P2P" Protocol="BITTORRENT"/>
>     <Number_Files>1</Number_Files>
>   </Source>
>   <Content>
>     <Item>
>       <TimeStamp>2019-02-27T08:06:44Z</TimeStamp>
>       <Title>Catch Me If You Can</Title>
>       <FileName>Catch.Me.If.You.Can.2002.BDRip.720p.mkv</FileName>
>       <FileSize>8482935848</FileSize>
>       <Hash Type="SHA1">03365a2ee134f06313fbd1091d0683435a9158de</Hash>
>     </Item>
>   </Content>
> </Infringement>
> -----BEGIN PGP SIGNATURE-----
> Version: GnuPG v1
> 
> iQEcBAEBAgAGBQJcdkUZAAoJEN5LM3Etqs/W1fwH/2KL9XPSRzK4gICp5QxIIWZG
> vZsTPZxkPxkt/ymDOLJmw2WZ8u7vH8k2j7DpH8TgbhnHZGe+eas5KpHkEambTU+e
> gcLRya8+S62rOzu+DmubvaX441n7O64ZHiTV/DkEfUw1Phh+x0/GyDX7Mxn1DHOM
> f3cfIGmQGMjHN/U0v72U+874ldX2xltp3IKOYgjAxV5pWgDp2dTilwBAtKT9gmbq
> hhCzS028VDG7AYpedjTTWIY579NW7wtXujkLFtSTyUs2/UHvDpI82kX55EjRBloR
> WpSUwBa2ktP5P5nMrBXE9AwHQJ3+pQWvm6/Hupu5e8/JmNRX3lMhDaqvG7vV/VA=
> =uR2O
> -----END PGP SIGNATURE-----
}>
'''


test_log = '''Mar 10 10:55:26 lead radiusd[74789]: (170684) rlm_rules_injector[1629201807]: Send ADD 37.140.183.11 ntutunik.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 11:37:45 lead radiusd[74789]: (184341) rlm_rules_injector[2688398696]: Send ADD 37.140.183.11 eberkovich.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 12:06:45 lead radiusd[74789]: (193929) rlm_rules_injector[3800262521]: Send ADD 37.140.183.11 nasneg.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 12:10:34 lead radiusd[74789]: (195202) rlm_rules_injector[1284196248]: Send ADD 37.140.183.11 osaulenko.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 12:22:31 lead radiusd[74789]: (199935) rlm_rules_injector[2121657440]: Send ADD 37.140.183.11 mamton.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 12:29:39 lead radiusd[74789]: (202627) rlm_rules_injector[4226526672]: Send ADD 37.140.183.11 nadiano.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 13:20:29 lead radiusd[74789]: (221242) rlm_rules_injector[2438132462]: Send ADD 37.140.183.11 nikitkorolev.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 13:26:43 lead radiusd[74789]: (223586) rlm_rules_injector[2403247130]: Send ADD 37.140.183.11 hatwhale.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 13:31:49 lead radiusd[74789]: (225337) rlm_rules_injector[2794212323]: Send ADD 37.140.183.11 maxim-shanti.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 13:38:39 lead radiusd[74789]: (227692) rlm_rules_injector[4101758268]: Send ADD 37.140.183.11 katyamokina.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 17:07:56 lead radiusd[74789]: (301504) rlm_rules_injector[3792860321]: Send ADD 37.140.183.11 exler.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 17:13:16 lead radiusd[74789]: (303320) rlm_rules_injector[1957584035]: Send ADD 37.140.183.11 borman.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 18:11:31 lead radiusd[74789]: (326568) rlm_rules_injector[191702474]: Send ADD 37.140.183.11 olegwoloschin.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 18:19:29 lead radiusd[74789]: (330014) rlm_rules_injector[2679045324]: Send ADD 37.140.183.11 alexgladkii.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 18:50:39 lead radiusd[74789]: (341887) rlm_rules_injector[3477098632]: Send ADD 37.140.183.11 svoropaev.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 20:07:43 lead radiusd[74789]: (370115) rlm_rules_injector[4094960769]: Send ADD 37.140.183.11 antipich.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 20:10:18 lead radiusd[74789]: (371134) rlm_rules_injector[3666210212]: Send ADD 37.140.183.11 egaponenko.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 20:33:39 lead radiusd[74789]: (378856) rlm_rules_injector[2377962722]: Send ADD 37.140.183.11 vchigrin.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 20:48:21 lead radiusd[74789]: (384017) rlm_rules_injector[4126310097]: Send ADD 37.140.183.11 sham.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 21:48:25 lead radiusd[74789]: (405995) rlm_rules_injector[2856849000]: Send ADD 37.140.183.11 mfadin.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 21:51:04 lead radiusd[74789]: (407015) rlm_rules_injector[231766778]: Send ADD 37.140.183.11 chelu.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 21:53:43 lead radiusd[74789]: (408074) rlm_rules_injector[1465978670]: Send ADD 37.140.183.11 juliakoval.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 21:59:02 lead radiusd[74789]: (409895) rlm_rules_injector[4002191058]: Send ADD 37.140.183.11 annash.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 23:23:20 lead radiusd[74789]: (437901) rlm_rules_injector[2372465902]: Send ADD 37.140.183.11 niyaz.vpn 0-> /tmp/firsthop-injector.socket
Mar 10 23:28:09 lead radiusd[74789]: (439405) rlm_rules_injector[2742074070]: Send ADD 37.140.183.11 lisitsyn.vpn 0-> /tmp/firsthop-injector.socket'''

test_data_field = [
  {
    'tag_start': 'TimeStamp>',
    'tag_end': '/TimeStamp>',
    'string': test_ticket_description,
    'result': '2019-03-10T08:06:44Z'
  },
  {
    'tag_start': 'IP_Address>',
    'tag_end': '/IP_Address>',
    'string': test_ticket_description,
    'result': '37.140.183.11'
  },
  {
    'tag_start': 'FileName>',
    'tag_end': '/FileName>',
    'string': test_ticket_description,
    'result': 'Catch.Me.If.You.Can.2002.BDRip.720p.mkv'
  },
]

test_data_str_parser = [
  [
    'Feb 28 18:52:32 violet radiusd[75475]: (4910509) rlm_rules_injector[545688613]: Send ADD 37.140.134.32 gg0sha.wireless 0-> /tmp/firsthop-injector.socket\n', 
    'Feb 28 20:06:08 violet radiusd[75475]: (5352500) rlm_rules_injector[1]: Send ADD 37.140.134.32 gg0sha.wireless 0-> /tmp/firsthop-injector.socket\n',
    'Feb 28 21:18:32 violet radiusd[75475]: (5640030) rlm_rules_injector[1]: Send ADD 37.140.134.32 gg0sha.wireless 0-> /tmp/firsthop-injector.socket\n'
  ],
  []
]

test_data_check_logs = [
  (
    '127.0.0.1',
    '2019-02-27T08:06:44Z'
  )
]

test_true_result = [
  'HDRFS-000111',
  'Привет, с Вашего компьютера обнаружена торрент-активность (Catch.Me.If.You.Can.2002.BDRip.720p.mkv). Напоминаем Вам, что скачивать торренты из нашей сети (включая VPN) запрещено. Убедительная просьба либо удалить торрент-клиент, либо контролировать его трафик.\n**Сообщите в данном тикете о проделанных операциях.**',
  'ntutunik'
]