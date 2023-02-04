from infra.cauth.server.master.importers.servers.sources import BotAbcGroups, GroupResult

SERVICE_TO_FQDNS = {
    1: ['iva1.search.yandex.net', 'sas1.search.yandex.net', 'sas3.search.yandex.net'],
    2: ['iva3.search.yandex.net', 'balancer.search.yandex.net', 'lpd3.adfox.ru'],
}
PRODUCT_1 = 'prod1'
PRODUCT_2 = 'prod2'
SERVICE_TO_SLUG = {
    1: 'market',
    2: 'ysearch',
}
RESPONSIBLES = ['resp1', 'resp2']


def test_bot_import(mock_bot_https):
    mock_bot_https(BOT_ANSWER, ABC_ANSWER)
    BotAbcGroups.SERVICES_PER_REQUEST = 1
    result = BotAbcGroups(None).fetch()
    expected_result = [
        GroupResult(
            name='.'.join((BotAbcGroups.PREFIX, SERVICE_TO_SLUG[1])),
            responsibles=set(RESPONSIBLES),
            source=BotAbcGroups.PREFIX,
            hosts=[
                GroupResult.Host(hostname=fqdn, is_baremetal=True)
                for fqdn in SERVICE_TO_FQDNS[1]
            ],
        ),
        GroupResult(
            name='.'.join((BotAbcGroups.PREFIX, SERVICE_TO_SLUG[2])),
            responsibles={PRODUCT_2},
            source=BotAbcGroups.PREFIX,
            hosts=[
                GroupResult.Host(hostname=fqdn, is_baremetal=True)
                for fqdn in SERVICE_TO_FQDNS[2]
            ],
        ),
    ]
    assert all(it in expected_result for it in result)


BOT_ANSWER = (
'''362600	{fqdn1}	OPERATION	RU	IVA	IVNIT	IVA-3	60	45	-	0025906C0036	0025906C0037			90E2BAEABF06	Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud > SRE RTC	DP/SM/SYS6017RNTF/4T3.5/1U/1P	XEONE5-2660	SERVERS	SRV	X9DRW-IF	0025906FF783	{service_1_id}	{lacky_value}
900275	{fqdn2}	OPERATION	RU	SAS	SASTA	SAS-1.3.2	25	1	-	902B34CF39F4	902B34CF39F5				Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud	DP/GB/7PPSH/6T3.5/1U/N	XEONE5-2660	NODES	SRV	GA-7PPSH	902B34CF39E4	{service_1_id}	37
900158	{fqdn3}	OPERATION	RU	SAS	SASTA	SAS-1.3.1	10	1	-	902B34CF292A	902B34CF292B				Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud	DP/GB/7PPSH/6T3.5/1U/N	XEONE5-2660	NODES	SRV	GA-7PPSH	902B34CF2924	{service_2_id}	31
299070	{fqdn4}	OPERATION	RU	IVA	IVNIT	IVA-4	29	45	-	0025909243C8	0025909243C9				Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud	IR/SM/SYS6017RNTF/4T3.5/1U/1P	XEONE5-2660	SERVERS	SRV	X9DRW-IF	0025909D7EA4	{service_1_id}	{lacky_value}
900213	{fqdn5}	OPERATION	RU	SAS	SASTA	SAS-1.3.1	16	1	-	902B34CF3C7E	902B34CF3C7F				Search Portal > Personal and Infrastructure Services > Infra Search > Infra cloud	DP/GB/7PPSH/6T3.5/1U/N	XEONE5-2660	NODES	SRV	GA-7PPSH	902B34CF3C6E	{service_2_id}	48
238709	{fqdn6}	OPERATION	RU	IVA	IVNIT	IVA-4	13	33	-	0025900D6C2C	0025900D6C2D				Search Portal > Advertising Services > RSY > ADFOX	DP/SM/SYS6016TNTF/4T3.5/1U/1P	XEON5645	SERVERS	SRV	X8DTU-F	0025900D814D	{service_2_id}	'''  # noqa
    .format(
        fqdn1=SERVICE_TO_FQDNS[1][0],
        fqdn2=SERVICE_TO_FQDNS[1][1],
        fqdn3=SERVICE_TO_FQDNS[2][0],
        fqdn4=SERVICE_TO_FQDNS[1][2],
        fqdn5=SERVICE_TO_FQDNS[2][1],
        fqdn6=SERVICE_TO_FQDNS[2][2],
        service_1_id=1,
        service_2_id=2,
        lacky_value='',
    ).encode()
)

ABC_ANSWER = [
    {
        "person": {
            "login": RESPONSIBLES[0],
        },
        "service": {
            "id": 1,
            "slug": SERVICE_TO_SLUG[1],
        },
        "role": {
            "id": BotAbcGroups.HARDWARE_MANAGER_ID,
        }
    },
    {
        "person": {
            "login": RESPONSIBLES[1],
        },
        "service": {
            "id": 1,
            "slug": SERVICE_TO_SLUG[1],
        },
        "role": {
            "id": BotAbcGroups.HARDWARE_MANAGER_ID,
        }
    },
    {
        "person": {
            "login": PRODUCT_2
        },
        "service": {
            "id": 2,
            "slug": SERVICE_TO_SLUG[2],
        },
        "role": {
            "id": BotAbcGroups.PRODUCT_HEAD_ID,
        }
    }
]
