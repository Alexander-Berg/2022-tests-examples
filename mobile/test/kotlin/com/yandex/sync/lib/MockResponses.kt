package com.yandex.sync.lib

object MockResponses {
    val PRINCIPAL = """
        <?xml version='1.0' encoding='utf-8'?>
        <d:multistatus xmlns:d="DAV:">
            <d:response>
                <href xmlns="DAV:">/</href>
                <d:propstat>
                    <d:prop>
                        <d:current-user-principal>
                            <d:href>/principals/users/thevery%40yandex-team.ru/</d:href>
                        </d:current-user-principal>
                    </d:prop>
                    <status xmlns="DAV:">HTTP/1.1 200 OK</status>
                </d:propstat>
            </d:response>
        </d:multistatus>
        """.trimIndent()

    val USERINFO = """
        <?xml version='1.0' encoding='utf-8'?>
        <D:multistatus xmlns:D="DAV:">
            <D:response>
                <href xmlns="DAV:">/principals/users/thevery%40yandex-team.ru/</href>
                <D:propstat>
                    <D:prop>
                        <C:calendar-home-set xmlns:C="urn:ietf:params:xml:ns:caldav">
                            <D:href>/calendars/thevery%40yandex-team.ru/</D:href>
                        </C:calendar-home-set>
                        <email-address-set xmlns="http://calendarserver.org/ns/">
                            <D:href>mailto:thevery@yandex-team.ru</D:href>
                        </email-address-set>
                        <E:directory-gateway xmlns:E="urn:ietf:params:xml:ns:carddav">
                            <D:href>/directory/</D:href>
                        </E:directory-gateway>
                        <E:addressbook-home-set xmlns:E="urn:ietf:params:xml:ns:carddav">
                            <D:href>/addressbook/thevery%40yandex-team.ru/</D:href>
                        </E:addressbook-home-set>
                    </D:prop>
                    <status xmlns="DAV:">HTTP/1.1 200 OK</status>
                </D:propstat>
            </D:response>
        </D:multistatus>
        """.trimIndent()

    val CALENDARS = """
        <?xml version='1.0' encoding='utf-8'?>
        <D:multistatus xmlns:D="DAV:">
            <D:response>
                <href xmlns="DAV:">/calendars/thevery%40yandex-team.ru/</href>
                <D:propstat>
                    <D:prop>
                        <D:displayname>Ильдар Каримов</D:displayname>
                        <D:owner>
                            <D:href>/principals/users/thevery%40yandex-team.ru/</D:href>
                        </D:owner>
                        <D:sync-token>data:,1518313480268</D:sync-token>
                        <D:resourcetype>
                            <D:collection />
                        </D:resourcetype>
                    </D:prop>
                    <status xmlns="DAV:">HTTP/1.1 200 OK</status>
                </D:propstat>
                <D:propstat>
                    <D:prop>
                        <D:current-user-privilege-set />
                        <calendar-color xmlns="http://apple.com/ns/ical/" />
                        <C:supported-calendar-component-set xmlns:C="urn:ietf:params:xml:ns:caldav" />
                        <getctag xmlns="http://calendarserver.org/ns/" />
                    </D:prop>
                    <status xmlns="DAV:">HTTP/1.1 404 Not Found</status>
                </D:propstat>
            </D:response>
            <D:response>
                <href xmlns="DAV:">/calendars/thevery%40yandex-team.ru/inbox/</href>
                <D:propstat>
                    <D:prop>
                        <D:resourcetype>
                            <D:collection />
                            <C:schedule-inbox xmlns:C="urn:ietf:params:xml:ns:caldav" />
                        </D:resourcetype>
                    </D:prop>
                    <status xmlns="DAV:">HTTP/1.1 200 OK</status>
                </D:propstat>
                <D:propstat>
                    <D:prop>
                        <D:current-user-privilege-set />
                        <D:displayname />
                        <calendar-color xmlns="http://apple.com/ns/ical/" />
                        <D:owner />
                        <D:sync-token />
                        <C:supported-calendar-component-set xmlns:C="urn:ietf:params:xml:ns:caldav" />
                        <getctag xmlns="http://calendarserver.org/ns/" />
                    </D:prop>
                    <status xmlns="DAV:">HTTP/1.1 404 Not Found</status>
                </D:propstat>
            </D:response>
            <D:response>
                <href xmlns="DAV:">/calendars/thevery%40yandex-team.ru/outbox/</href>
                <D:propstat>
                    <D:prop>
                        <D:resourcetype>
                            <C:schedule-outbox xmlns:C="urn:ietf:params:xml:ns:caldav" />
                            <D:collection />
                        </D:resourcetype>
                    </D:prop>
                    <status xmlns="DAV:">HTTP/1.1 200 OK</status>
                </D:propstat>
                <D:propstat>
                    <D:prop>
                        <D:current-user-privilege-set />
                        <D:displayname />
                        <calendar-color xmlns="http://apple.com/ns/ical/" />
                        <D:owner />
                        <D:sync-token />
                        <C:supported-calendar-component-set xmlns:C="urn:ietf:params:xml:ns:caldav" />
                        <getctag xmlns="http://calendarserver.org/ns/" />
                    </D:prop>
                    <status xmlns="DAV:">HTTP/1.1 404 Not Found</status>
                </D:propstat>
            </D:response>
            <D:response>
                <href xmlns="DAV:">/calendars/thevery%40yandex-team.ru/events-12618/</href>
                <D:propstat>
                    <D:prop>
                        <D:current-user-privilege-set>
                            <D:privilege>
                                <D:read />
                            </D:privilege>
                        </D:current-user-privilege-set>
                        <D:displayname>Ильдар Каримов</D:displayname>
                        <calendar-color xmlns="http://apple.com/ns/ical/">#49c0a8ff</calendar-color>
                        <D:owner>
                            <D:href>/principals/users/thevery%40yandex-team.ru/</D:href>
                        </D:owner>
                        <D:sync-token>data:,1518307816957</D:sync-token>
                        <D:resourcetype>
                            <C:calendar xmlns:C="urn:ietf:params:xml:ns:caldav" />
                            <D:collection />
                        </D:resourcetype>
                        <C:supported-calendar-component-set xmlns:C="urn:ietf:params:xml:ns:caldav">
                            <C:comp name="VEVENT" />
                        </C:supported-calendar-component-set>
                        <getctag xmlns="http://calendarserver.org/ns/">1518307816957</getctag>
                    </D:prop>
                    <status xmlns="DAV:">HTTP/1.1 200 OK</status>
                </D:propstat>
            </D:response>
            <D:response>
                <href xmlns="DAV:">/calendars/thevery%40yandex-team.ru/events-12873/</href>
                <D:propstat>
                    <D:prop>
                        <D:current-user-privilege-set>
                            <D:privilege>
                                <D:read />
                            </D:privilege>
                            <D:privilege>
                                <D:write-properties />
                            </D:privilege>
                        </D:current-user-privilege-set>
                        <D:displayname>Отсутствия</D:displayname>
                        <calendar-color xmlns="http://apple.com/ns/ical/">#8f499eff</calendar-color>
                        <D:owner>
                            <D:href>/principals/users/thevery%40yandex-team.ru/</D:href>
                        </D:owner>
                        <D:sync-token>data:,1518313480268</D:sync-token>
                        <D:resourcetype>
                            <C:calendar xmlns:C="urn:ietf:params:xml:ns:caldav" />
                            <D:collection />
                        </D:resourcetype>
                        <C:supported-calendar-component-set xmlns:C="urn:ietf:params:xml:ns:caldav">
                            <C:comp name="VEVENT" />
                        </C:supported-calendar-component-set>
                        <getctag xmlns="http://calendarserver.org/ns/">1518313480268</getctag>
                    </D:prop>
                    <status xmlns="DAV:">HTTP/1.1 200 OK</status>
                </D:propstat>
            </D:response>
            <D:response>
                <href xmlns="DAV:">/calendars/thevery%40yandex-team.ru/todos-3946/</href>
                <D:propstat>
                    <D:prop>
                        <D:current-user-privilege-set>
                        </D:current-user-privilege-set>
                        <D:displayname>Не забыть</D:displayname>
                        <calendar-color xmlns="http://apple.com/ns/ical/">#b9b9b9ff</calendar-color>
                        <D:owner>
                            <D:href>/principals/users/thevery%40yandex-team.ru/</D:href>
                        </D:owner>
                        <D:sync-token>data:,1323249271000</D:sync-token>
                        <D:resourcetype>
                            <C:calendar xmlns:C="urn:ietf:params:xml:ns:caldav" />
                            <D:collection />
                        </D:resourcetype>
                        <C:supported-calendar-component-set xmlns:C="urn:ietf:params:xml:ns:caldav">
                            <C:comp name="VTODO" />
                        </C:supported-calendar-component-set>
                        <getctag xmlns="http://calendarserver.org/ns/">1323249271000</getctag>
                    </D:prop>
                    <status xmlns="DAV:">HTTP/1.1 200 OK</status>
                </D:propstat>
            </D:response>
            <D:response>
                <href xmlns="DAV:">/calendars/thevery%40yandex-team.ru/events-38152/</href>
                <D:propstat>
                    <D:prop>
                        <D:current-user-privilege-set>
                            <D:privilege>
                                <D:write />
                            </D:privilege>
                            <D:privilege>
                                <D:read />
                            </D:privilege>
                        </D:current-user-privilege-set>
                        <D:displayname>Школа Мобильной разработки 2017</D:displayname>
                        <calendar-color xmlns="http://apple.com/ns/ical/">#0d866aff</calendar-color>
                        <D:owner>
                            <D:href>/principals/users/thevery%40yandex-team.ru/</D:href>
                        </D:owner>
                        <D:sync-token>data:,1514287893608</D:sync-token>
                        <D:resourcetype>
                            <C:calendar xmlns:C="urn:ietf:params:xml:ns:caldav" />
                            <D:collection />
                        </D:resourcetype>
                        <C:supported-calendar-component-set xmlns:C="urn:ietf:params:xml:ns:caldav">
                            <C:comp name="VEVENT" />
                        </C:supported-calendar-component-set>
                        <getctag xmlns="http://calendarserver.org/ns/">1514287893608</getctag>
                    </D:prop>
                    <status xmlns="DAV:">HTTP/1.1 200 OK</status>
                </D:propstat>
            </D:response>
        </D:multistatus>
        """.trimIndent()

    val CALENDAR = """
<?xml version='1.0' encoding='utf-8'?>
<D:multistatus xmlns:D="DAV:"><D:response><href xmlns="DAV:">/calendars/ttqul%40yandex.ru/events-5632612/thhnxh8myandex.ru.ics</href><D:propstat><D:prop><D:getetag>1517258265195</D:getetag><C:calendar-data xmlns:C="urn:ietf:params:xml:ns:caldav">BEGIN:VCALENDAR
PRODID:-//Yandex LLC//Yandex Calendar//EN
VERSION:2.0
CALSCALE:GREGORIAN
METHOD:PUBLISH
BEGIN:VTIMEZONE
TZID:Europe/Moscow
TZURL:http://tzurl.org/zoneinfo/Europe/Moscow
X-LIC-LOCATION:Europe/Moscow
BEGIN:STANDARD
TZOFFSETFROM:+023017
TZOFFSETTO:+023017
TZNAME:MMT
DTSTART:18800101T000000
RDATE:18800101T000000
END:STANDARD
BEGIN:STANDARD
TZOFFSETFROM:+023017
TZOFFSETTO:+023119
TZNAME:MMT
DTSTART:19160703T000000
RDATE:19160703T000000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+023119
TZOFFSETTO:+033119
TZNAME:MST
DTSTART:19170701T230000
RDATE:19170701T230000
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+033119
TZOFFSETTO:+023119
TZNAME:MMT
DTSTART:19171228T000000
RDATE:19171228T000000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+023119
TZOFFSETTO:+043119
TZNAME:MDST
DTSTART:19180531T220000
RDATE:19180531T220000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+043119
TZOFFSETTO:+033119
TZNAME:MST
DTSTART:19180916T010000
RDATE:19180916T010000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+033119
TZOFFSETTO:+043119
TZNAME:MDST
DTSTART:19190531T230000
RDATE:19190531T230000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+043119
TZOFFSETTO:+0400
TZNAME:MSD
DTSTART:19190701T043119
RDATE:19190701T043119
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+0400
TZOFFSETTO:+0300
TZNAME:MSK
DTSTART:19190816T000000
RDATE:19190816T000000
RDATE:19211001T000000
RDATE:19811001T000000
RDATE:19821001T000000
RDATE:19831001T000000
RDATE:19840930T030000
RDATE:19850929T030000
RDATE:19860928T030000
RDATE:19870927T030000
RDATE:19880925T030000
RDATE:19890924T030000
RDATE:19900930T030000
RDATE:19920927T030000
RDATE:19930926T030000
RDATE:19940925T030000
RDATE:19950924T030000
RDATE:19961027T030000
RDATE:19971026T030000
RDATE:19981025T030000
RDATE:19991031T030000
RDATE:20001029T030000
RDATE:20011028T030000
RDATE:20021027T030000
RDATE:20031026T030000
RDATE:20041031T030000
RDATE:20051030T030000
RDATE:20061029T030000
RDATE:20071028T030000
RDATE:20081026T030000
RDATE:20091025T030000
RDATE:20101031T030000
RDATE:20141026T020000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+0300
TZOFFSETTO:+0400
TZNAME:MSD
DTSTART:19210214T230000
RDATE:19210214T230000
RDATE:19810401T000000
RDATE:19820401T000000
RDATE:19830401T000000
RDATE:19840401T000000
RDATE:19850331T020000
RDATE:19860330T020000
RDATE:19870329T020000
RDATE:19880327T020000
RDATE:19890326T020000
RDATE:19900325T020000
RDATE:19920329T020000
RDATE:19930328T020000
RDATE:19940327T020000
RDATE:19950326T020000
RDATE:19960331T020000
RDATE:19970330T020000
RDATE:19980329T020000
RDATE:19990328T020000
RDATE:20000326T020000
RDATE:20010325T020000
RDATE:20020331T020000
RDATE:20030330T020000
RDATE:20040328T020000
RDATE:20050327T020000
RDATE:20060326T020000
RDATE:20070325T020000
RDATE:20080330T020000
RDATE:20090329T020000
RDATE:20100328T020000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+0400
TZOFFSETTO:+0500
TZNAME:+05
DTSTART:19210320T230000
RDATE:19210320T230000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+0500
TZOFFSETTO:+0400
TZNAME:MSD
DTSTART:19210901T000000
RDATE:19210901T000000
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+0300
TZOFFSETTO:+0200
TZNAME:EET
DTSTART:19221001T000000
RDATE:19221001T000000
RDATE:19910929T030000
END:STANDARD
BEGIN:STANDARD
TZOFFSETFROM:+0200
TZOFFSETTO:+0300
TZNAME:MSK
DTSTART:19300621T000000
RDATE:19300621T000000
RDATE:19920119T020000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+0300
TZOFFSETTO:+0300
TZNAME:EEST
DTSTART:19910331T020000
RDATE:19910331T020000
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+0300
TZOFFSETTO:+0400
TZNAME:MSK
DTSTART:20110327T020000
RDATE:20110327T020000
END:STANDARD
END:VTIMEZONE
BEGIN:VEVENT
DTSTART;TZID=Europe/Moscow:20180129T234500
DTEND;TZID=Europe/Moscow:20180130T001500
SUMMARY:123
UID:thhnxh8myandex.ru
SEQUENCE:0
DTSTAMP:20181026T120621Z
CREATED:20180129T203745Z
URL:https://calendar.yandex.ru/event?event_id=490833420
TRANSP:OPAQUE
CATEGORIES:Мои события
LAST-MODIFIED:20180129T203745Z
END:VEVENT
END:VCALENDAR
</C:calendar-data></D:prop><status xmlns="DAV:">HTTP/1.1 200 OK</status></D:propstat></D:response><D:response><href xmlns="DAV:">/calendars/ttqul%40yandex.ru/events-5632612/uvow8ANsyandex.ru.ics</href><D:propstat><D:prop><D:getetag>1540395421512</D:getetag><C:calendar-data xmlns:C="urn:ietf:params:xml:ns:caldav">BEGIN:VCALENDAR
PRODID:-//Yandex LLC//Yandex Calendar//EN
VERSION:2.0
CALSCALE:GREGORIAN
METHOD:PUBLISH
BEGIN:VTIMEZONE
TZID:Europe/Moscow
TZURL:http://tzurl.org/zoneinfo/Europe/Moscow
X-LIC-LOCATION:Europe/Moscow
BEGIN:STANDARD
TZOFFSETFROM:+023017
TZOFFSETTO:+023017
TZNAME:MMT
DTSTART:18800101T000000
RDATE:18800101T000000
END:STANDARD
BEGIN:STANDARD
TZOFFSETFROM:+023017
TZOFFSETTO:+023119
TZNAME:MMT
DTSTART:19160703T000000
RDATE:19160703T000000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+023119
TZOFFSETTO:+033119
TZNAME:MST
DTSTART:19170701T230000
RDATE:19170701T230000
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+033119
TZOFFSETTO:+023119
TZNAME:MMT
DTSTART:19171228T000000
RDATE:19171228T000000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+023119
TZOFFSETTO:+043119
TZNAME:MDST
DTSTART:19180531T220000
RDATE:19180531T220000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+043119
TZOFFSETTO:+033119
TZNAME:MST
DTSTART:19180916T010000
RDATE:19180916T010000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+033119
TZOFFSETTO:+043119
TZNAME:MDST
DTSTART:19190531T230000
RDATE:19190531T230000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+043119
TZOFFSETTO:+0400
TZNAME:MSD
DTSTART:19190701T043119
RDATE:19190701T043119
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+0400
TZOFFSETTO:+0300
TZNAME:MSK
DTSTART:19190816T000000
RDATE:19190816T000000
RDATE:19211001T000000
RDATE:19811001T000000
RDATE:19821001T000000
RDATE:19831001T000000
RDATE:19840930T030000
RDATE:19850929T030000
RDATE:19860928T030000
RDATE:19870927T030000
RDATE:19880925T030000
RDATE:19890924T030000
RDATE:19900930T030000
RDATE:19920927T030000
RDATE:19930926T030000
RDATE:19940925T030000
RDATE:19950924T030000
RDATE:19961027T030000
RDATE:19971026T030000
RDATE:19981025T030000
RDATE:19991031T030000
RDATE:20001029T030000
RDATE:20011028T030000
RDATE:20021027T030000
RDATE:20031026T030000
RDATE:20041031T030000
RDATE:20051030T030000
RDATE:20061029T030000
RDATE:20071028T030000
RDATE:20081026T030000
RDATE:20091025T030000
RDATE:20101031T030000
RDATE:20141026T020000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+0300
TZOFFSETTO:+0400
TZNAME:MSD
DTSTART:19210214T230000
RDATE:19210214T230000
RDATE:19810401T000000
RDATE:19820401T000000
RDATE:19830401T000000
RDATE:19840401T000000
RDATE:19850331T020000
RDATE:19860330T020000
RDATE:19870329T020000
RDATE:19880327T020000
RDATE:19890326T020000
RDATE:19900325T020000
RDATE:19920329T020000
RDATE:19930328T020000
RDATE:19940327T020000
RDATE:19950326T020000
RDATE:19960331T020000
RDATE:19970330T020000
RDATE:19980329T020000
RDATE:19990328T020000
RDATE:20000326T020000
RDATE:20010325T020000
RDATE:20020331T020000
RDATE:20030330T020000
RDATE:20040328T020000
RDATE:20050327T020000
RDATE:20060326T020000
RDATE:20070325T020000
RDATE:20080330T020000
RDATE:20090329T020000
RDATE:20100328T020000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+0400
TZOFFSETTO:+0500
TZNAME:+05
DTSTART:19210320T230000
RDATE:19210320T230000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+0500
TZOFFSETTO:+0400
TZNAME:MSD
DTSTART:19210901T000000
RDATE:19210901T000000
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+0300
TZOFFSETTO:+0200
TZNAME:EET
DTSTART:19221001T000000
RDATE:19221001T000000
RDATE:19910929T030000
END:STANDARD
BEGIN:STANDARD
TZOFFSETFROM:+0200
TZOFFSETTO:+0300
TZNAME:MSK
DTSTART:19300621T000000
RDATE:19300621T000000
RDATE:19920119T020000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+0300
TZOFFSETTO:+0300
TZNAME:EEST
DTSTART:19910331T020000
RDATE:19910331T020000
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+0300
TZOFFSETTO:+0400
TZNAME:MSK
DTSTART:20110327T020000
RDATE:20110327T020000
END:STANDARD
END:VTIMEZONE
BEGIN:VEVENT
DTSTART;TZID=Europe/Moscow:20181025T171500
DTEND;TZID=Europe/Moscow:20181025T174500
SUMMARY:tetwe
UID:uvow8ANsyandex.ru
SEQUENCE:2
DTSTAMP:20181026T120621Z
CREATED:20181024T135940Z
URL:https://calendar.yandex.ru/event?event_id=716561829
TRANSP:OPAQUE
CATEGORIES:Мои события
LAST-MODIFIED:20181024T153701Z
END:VEVENT
END:VCALENDAR
</C:calendar-data></D:prop><status xmlns="DAV:">HTTP/1.1 200 OK</status></D:propstat></D:response><D:response><href xmlns="DAV:">/calendars/ttqul%40yandex.ru/events-5632612/4aiyLmbLyandex.ru.ics</href><D:propstat><D:prop><D:getetag>1540474466373</D:getetag><C:calendar-data xmlns:C="urn:ietf:params:xml:ns:caldav">BEGIN:VCALENDAR
PRODID:-//Yandex LLC//Yandex Calendar//EN
VERSION:2.0
CALSCALE:GREGORIAN
METHOD:PUBLISH
BEGIN:VTIMEZONE
TZID:Europe/Moscow
TZURL:http://tzurl.org/zoneinfo/Europe/Moscow
X-LIC-LOCATION:Europe/Moscow
BEGIN:STANDARD
TZOFFSETFROM:+023017
TZOFFSETTO:+023017
TZNAME:MMT
DTSTART:18800101T000000
RDATE:18800101T000000
END:STANDARD
BEGIN:STANDARD
TZOFFSETFROM:+023017
TZOFFSETTO:+023119
TZNAME:MMT
DTSTART:19160703T000000
RDATE:19160703T000000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+023119
TZOFFSETTO:+033119
TZNAME:MST
DTSTART:19170701T230000
RDATE:19170701T230000
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+033119
TZOFFSETTO:+023119
TZNAME:MMT
DTSTART:19171228T000000
RDATE:19171228T000000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+023119
TZOFFSETTO:+043119
TZNAME:MDST
DTSTART:19180531T220000
RDATE:19180531T220000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+043119
TZOFFSETTO:+033119
TZNAME:MST
DTSTART:19180916T010000
RDATE:19180916T010000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+033119
TZOFFSETTO:+043119
TZNAME:MDST
DTSTART:19190531T230000
RDATE:19190531T230000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+043119
TZOFFSETTO:+0400
TZNAME:MSD
DTSTART:19190701T043119
RDATE:19190701T043119
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+0400
TZOFFSETTO:+0300
TZNAME:MSK
DTSTART:19190816T000000
RDATE:19190816T000000
RDATE:19211001T000000
RDATE:19811001T000000
RDATE:19821001T000000
RDATE:19831001T000000
RDATE:19840930T030000
RDATE:19850929T030000
RDATE:19860928T030000
RDATE:19870927T030000
RDATE:19880925T030000
RDATE:19890924T030000
RDATE:19900930T030000
RDATE:19920927T030000
RDATE:19930926T030000
RDATE:19940925T030000
RDATE:19950924T030000
RDATE:19961027T030000
RDATE:19971026T030000
RDATE:19981025T030000
RDATE:19991031T030000
RDATE:20001029T030000
RDATE:20011028T030000
RDATE:20021027T030000
RDATE:20031026T030000
RDATE:20041031T030000
RDATE:20051030T030000
RDATE:20061029T030000
RDATE:20071028T030000
RDATE:20081026T030000
RDATE:20091025T030000
RDATE:20101031T030000
RDATE:20141026T020000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+0300
TZOFFSETTO:+0400
TZNAME:MSD
DTSTART:19210214T230000
RDATE:19210214T230000
RDATE:19810401T000000
RDATE:19820401T000000
RDATE:19830401T000000
RDATE:19840401T000000
RDATE:19850331T020000
RDATE:19860330T020000
RDATE:19870329T020000
RDATE:19880327T020000
RDATE:19890326T020000
RDATE:19900325T020000
RDATE:19920329T020000
RDATE:19930328T020000
RDATE:19940327T020000
RDATE:19950326T020000
RDATE:19960331T020000
RDATE:19970330T020000
RDATE:19980329T020000
RDATE:19990328T020000
RDATE:20000326T020000
RDATE:20010325T020000
RDATE:20020331T020000
RDATE:20030330T020000
RDATE:20040328T020000
RDATE:20050327T020000
RDATE:20060326T020000
RDATE:20070325T020000
RDATE:20080330T020000
RDATE:20090329T020000
RDATE:20100328T020000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+0400
TZOFFSETTO:+0500
TZNAME:+05
DTSTART:19210320T230000
RDATE:19210320T230000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+0500
TZOFFSETTO:+0400
TZNAME:MSD
DTSTART:19210901T000000
RDATE:19210901T000000
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+0300
TZOFFSETTO:+0200
TZNAME:EET
DTSTART:19221001T000000
RDATE:19221001T000000
RDATE:19910929T030000
END:STANDARD
BEGIN:STANDARD
TZOFFSETFROM:+0200
TZOFFSETTO:+0300
TZNAME:MSK
DTSTART:19300621T000000
RDATE:19300621T000000
RDATE:19920119T020000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+0300
TZOFFSETTO:+0300
TZNAME:EEST
DTSTART:19910331T020000
RDATE:19910331T020000
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+0300
TZOFFSETTO:+0400
TZNAME:MSK
DTSTART:20110327T020000
RDATE:20110327T020000
END:STANDARD
END:VTIMEZONE
BEGIN:VEVENT
DTSTART;TZID=Europe/Moscow:20181025T183000
DTEND;TZID=Europe/Moscow:20181025T190000
SUMMARY:dffdfd
UID:4aiyLmbLyandex.ru
SEQUENCE:0
DTSTAMP:20181026T120621Z
CREATED:20181025T133426Z
URL:https://calendar.yandex.ru/event?event_id=717702303
TRANSP:OPAQUE
CATEGORIES:Мои события
LAST-MODIFIED:20181025T133426Z
END:VEVENT
END:VCALENDAR
</C:calendar-data></D:prop><status xmlns="DAV:">HTTP/1.1 200 OK</status></D:propstat></D:response><D:response><href xmlns="DAV:">/calendars/ttqul%40yandex.ru/events-5632612/VzWIZQUmyandex.ru.ics</href><D:propstat><D:prop><D:getetag>1540389365361</D:getetag><C:calendar-data xmlns:C="urn:ietf:params:xml:ns:caldav">BEGIN:VCALENDAR
PRODID:-//Yandex LLC//Yandex Calendar//EN
VERSION:2.0
CALSCALE:GREGORIAN
METHOD:PUBLISH
BEGIN:VTIMEZONE
TZID:Europe/Moscow
TZURL:http://tzurl.org/zoneinfo/Europe/Moscow
X-LIC-LOCATION:Europe/Moscow
BEGIN:STANDARD
TZOFFSETFROM:+023017
TZOFFSETTO:+023017
TZNAME:MMT
DTSTART:18800101T000000
RDATE:18800101T000000
END:STANDARD
BEGIN:STANDARD
TZOFFSETFROM:+023017
TZOFFSETTO:+023119
TZNAME:MMT
DTSTART:19160703T000000
RDATE:19160703T000000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+023119
TZOFFSETTO:+033119
TZNAME:MST
DTSTART:19170701T230000
RDATE:19170701T230000
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+033119
TZOFFSETTO:+023119
TZNAME:MMT
DTSTART:19171228T000000
RDATE:19171228T000000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+023119
TZOFFSETTO:+043119
TZNAME:MDST
DTSTART:19180531T220000
RDATE:19180531T220000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+043119
TZOFFSETTO:+033119
TZNAME:MST
DTSTART:19180916T010000
RDATE:19180916T010000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+033119
TZOFFSETTO:+043119
TZNAME:MDST
DTSTART:19190531T230000
RDATE:19190531T230000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+043119
TZOFFSETTO:+0400
TZNAME:MSD
DTSTART:19190701T043119
RDATE:19190701T043119
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+0400
TZOFFSETTO:+0300
TZNAME:MSK
DTSTART:19190816T000000
RDATE:19190816T000000
RDATE:19211001T000000
RDATE:19811001T000000
RDATE:19821001T000000
RDATE:19831001T000000
RDATE:19840930T030000
RDATE:19850929T030000
RDATE:19860928T030000
RDATE:19870927T030000
RDATE:19880925T030000
RDATE:19890924T030000
RDATE:19900930T030000
RDATE:19920927T030000
RDATE:19930926T030000
RDATE:19940925T030000
RDATE:19950924T030000
RDATE:19961027T030000
RDATE:19971026T030000
RDATE:19981025T030000
RDATE:19991031T030000
RDATE:20001029T030000
RDATE:20011028T030000
RDATE:20021027T030000
RDATE:20031026T030000
RDATE:20041031T030000
RDATE:20051030T030000
RDATE:20061029T030000
RDATE:20071028T030000
RDATE:20081026T030000
RDATE:20091025T030000
RDATE:20101031T030000
RDATE:20141026T020000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+0300
TZOFFSETTO:+0400
TZNAME:MSD
DTSTART:19210214T230000
RDATE:19210214T230000
RDATE:19810401T000000
RDATE:19820401T000000
RDATE:19830401T000000
RDATE:19840401T000000
RDATE:19850331T020000
RDATE:19860330T020000
RDATE:19870329T020000
RDATE:19880327T020000
RDATE:19890326T020000
RDATE:19900325T020000
RDATE:19920329T020000
RDATE:19930328T020000
RDATE:19940327T020000
RDATE:19950326T020000
RDATE:19960331T020000
RDATE:19970330T020000
RDATE:19980329T020000
RDATE:19990328T020000
RDATE:20000326T020000
RDATE:20010325T020000
RDATE:20020331T020000
RDATE:20030330T020000
RDATE:20040328T020000
RDATE:20050327T020000
RDATE:20060326T020000
RDATE:20070325T020000
RDATE:20080330T020000
RDATE:20090329T020000
RDATE:20100328T020000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+0400
TZOFFSETTO:+0500
TZNAME:+05
DTSTART:19210320T230000
RDATE:19210320T230000
END:DAYLIGHT
BEGIN:DAYLIGHT
TZOFFSETFROM:+0500
TZOFFSETTO:+0400
TZNAME:MSD
DTSTART:19210901T000000
RDATE:19210901T000000
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+0300
TZOFFSETTO:+0200
TZNAME:EET
DTSTART:19221001T000000
RDATE:19221001T000000
RDATE:19910929T030000
END:STANDARD
BEGIN:STANDARD
TZOFFSETFROM:+0200
TZOFFSETTO:+0300
TZNAME:MSK
DTSTART:19300621T000000
RDATE:19300621T000000
RDATE:19920119T020000
END:STANDARD
BEGIN:DAYLIGHT
TZOFFSETFROM:+0300
TZOFFSETTO:+0300
TZNAME:EEST
DTSTART:19910331T020000
RDATE:19910331T020000
END:DAYLIGHT
BEGIN:STANDARD
TZOFFSETFROM:+0300
TZOFFSETTO:+0400
TZNAME:MSK
DTSTART:20110327T020000
RDATE:20110327T020000
END:STANDARD
END:VTIMEZONE
BEGIN:VEVENT
DTSTART;TZID=Europe/Moscow:20181025T153000
DTEND;TZID=Europe/Moscow:20181025T160000
SUMMARY:ttt
UID:VzWIZQUmyandex.ru
SEQUENCE:0
DTSTAMP:20181026T120621Z
CREATED:20181024T135605Z
URL:https://calendar.yandex.ru/event?event_id=716559441
TRANSP:OPAQUE
CATEGORIES:Мои события
LAST-MODIFIED:20181024T135605Z
END:VEVENT
END:VCALENDAR
</C:calendar-data></D:prop><status xmlns="DAV:">HTTP/1.1 200 OK</status></D:propstat></D:response><D:response><href xmlns="DAV:">/calendars/ttqul%40yandex.ru/events-5632612/7AIVkX2eyandex.ru.ics</href><D:propstat><D:prop><D:getetag>1540283746856</D:getetag><C:calendar-data xmlns:C="urn:ietf:params:xml:ns:caldav"/></D:prop><status xmlns="DAV:">HTTP/1.1 404 Not Found</status></D:propstat></D:response><D:sync-token>data:,1540474466373</D:sync-token></D:multistatus>
        """.trimIndent()
}
