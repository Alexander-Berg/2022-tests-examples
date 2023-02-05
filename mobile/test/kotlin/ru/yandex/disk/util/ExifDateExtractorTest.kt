package ru.yandex.disk.util

import org.mockito.kotlin.mock
import org.junit.Test
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.util.Path
import java.io.InputStream
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private val FILE_WITHOUT_DATES = Path("img_without_dates.jpg")
private val FILE_WITH_DATETIME = Path("img_with_datetime.jpg")
private val FILE_WITH_DATETIME_ORIGINAL = Path("img_with_datetime_original.jpg")
private val FILE_WITH_DATETIME_DIGITIZED = Path("img_with_datetime_digitized.jpg")

class ExifDateExtractorTest : AndroidTestCase2() {

    private val localTimeZoneOffset = TimeUnit.MILLISECONDS.toHours(
        TimeZone.getDefault().rawOffset.toLong()).toInt()

    private val exifDateExtractor = ExifDateExtractor(mock(), ExifDateExtractorToggle(false))

    // yyyy:MM:dd kk:mm:ss
    @Test
    fun `should parse date format #1`() {
        "2011:03:07 10:30:20".assertAsCorrectDate(2011, 3, 7, 10, 30, 20)
        "2007:06:06 21:53:17".assertAsCorrectDate(2007, 6, 6, 21, 53, 17)
        "2017:12:31 14:41:12".assertAsCorrectDate(2017, 12, 31, 14, 41, 12)
        "2014:11:12 04:41:15".assertAsCorrectDate(2014, 11, 12, 4, 41, 15)
        "2018:12:15 08:31:43".assertAsCorrectDate(2018, 12, 15, 8, 31, 43)
        "2015:07:15 13:07:12".assertAsCorrectDate(2015, 7, 15, 13, 7, 12)
        "1997:08:03 18:30:00".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003:06:01 01:31:56".assertAsCorrectDate(2003, 6, 1, 1, 31, 56)
        "2019:11:20 23:01:12".assertAsCorrectDate(2019, 11, 20, 23, 1, 12)
        "2012:12:12 12:12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 12)
        "1995:09:10 08:41:28".assertAsCorrectDate(1995, 9, 10, 8, 41, 28)
    }

    // yyyy:MM:dd kk:mm
    @Test
    fun `should parse date format #2`() {
        "2011:03:07 10:30".assertAsCorrectDate(2011, 3, 7, 10, 30, 0)
        "2007:06:06 21:53".assertAsCorrectDate(2007, 6, 6, 21, 53, 0)
        "2017:12:31 14:41".assertAsCorrectDate(2017, 12, 31, 14, 41, 0)
        "2014:11:12 04:41".assertAsCorrectDate(2014, 11, 12, 4, 41, 0)
        "2018:12:15 08:31".assertAsCorrectDate(2018, 12, 15, 8, 31, 0)
        "2015:07:15 13:07".assertAsCorrectDate(2015, 7, 15, 13, 7, 0)
        "1997:08:03 18:30".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003:06:01 01:31".assertAsCorrectDate(2003, 6, 1, 1, 31, 0)
        "2019:11:20 23:01".assertAsCorrectDate(2019, 11, 20, 23, 1, 0)
        "2012:12:12 12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 0)
        "1995:09:10 08:41".assertAsCorrectDate(1995, 9, 10, 8, 41, 0)
    }

    // MM.dd.yyyy kk:mm:ss
    @Test
    fun `should parse date format #3`() {
        "03.19.2008 19:29:41".assertAsCorrectDate(2008, 3, 19, 19, 29, 41)
        "10.08.2007 21:27:39".assertAsCorrectDate(2007, 10, 8, 21, 27, 39)
        "05.19.2011 19:02:55".assertAsCorrectDate(2011, 5, 19, 19, 2, 55)
        "01.01.2001 17:42:10".assertAsCorrectDate(2001, 1, 1, 17, 42, 10)
        "03.24.2018 21:12:01".assertAsCorrectDate(2018, 3, 24, 21, 12, 1)
        "02.11.2004 12:20:17".assertAsCorrectDate(2004, 2, 11, 12, 20, 17)
        "09.20.2008 09:25:08".assertAsCorrectDate(2008, 9, 20, 9, 25, 8)
        "03.15.2012 20:39:32".assertAsCorrectDate(2012, 3, 15, 20, 39, 32)
        "12.19.2015 21:40:21".assertAsCorrectDate(2015, 12, 19, 21, 40, 21)
        "10.01.2007 08:26:00".assertAsCorrectDate(2007, 10, 1, 8, 26, 0)
        "03.21.2006 18:46:13".assertAsCorrectDate(2006, 3, 21, 18, 46, 13)
        "07.08.2007 20:26:26".assertAsCorrectDate(2007, 7, 8, 20, 26, 26)
        "03.19.2017 20:37:47".assertAsCorrectDate(2017, 3, 19, 20, 37, 47)
        "11.19.2008 20:04:02".assertAsCorrectDate(2008, 11, 19, 20, 4, 2)
    }

    // MM.dd.yyyy kk:mm
    @Test
    fun `should parse date format #4`() {
        "03.19.2008 19:29".assertAsCorrectDate(2008, 3, 19, 19, 29, 0)
        "10.08.2007 21:27".assertAsCorrectDate(2007, 10, 8, 21, 27, 0)
        "05.19.2011 19:02".assertAsCorrectDate(2011, 5, 19, 19, 2, 0)
        "01.01.2001 17:42".assertAsCorrectDate(2001, 1, 1, 17, 42, 0)
        "03.24.2018 21:12".assertAsCorrectDate(2018, 3, 24, 21, 12, 0)
        "02.11.2004 12:20".assertAsCorrectDate(2004, 2, 11, 12, 20, 0)
        "09.20.2008 09:25".assertAsCorrectDate(2008, 9, 20, 9, 25, 0)
        "03.15.2012 20:39".assertAsCorrectDate(2012, 3, 15, 20, 39, 0)
        "12.19.2015 21:40".assertAsCorrectDate(2015, 12, 19, 21, 40, 0)
        "10.01.2007 08:26".assertAsCorrectDate(2007, 10, 1, 8, 26, 0)
        "03.21.2006 18:46".assertAsCorrectDate(2006, 3, 21, 18, 46, 0)
        "07.08.2007 20:26".assertAsCorrectDate(2007, 7, 8, 20, 26, 0)
        "03.19.2017 20:37".assertAsCorrectDate(2017, 3, 19, 20, 37, 0)
        "11.19.2008 20:04".assertAsCorrectDate(2008, 11, 19, 20, 4, 0)
    }

    // yyyy.MM.dd kk:mm:ss
    @Test
    fun `should parse date format #5`() {
        "2011.03.07 10:30:20".assertAsCorrectDate(2011, 3, 7, 10, 30, 20)
        "2007.06.06 21:53:17".assertAsCorrectDate(2007, 6, 6, 21, 53, 17)
        "2017.12.31 14:41:12".assertAsCorrectDate(2017, 12, 31, 14, 41, 12)
        "2014.11.12 04:41:15".assertAsCorrectDate(2014, 11, 12, 4, 41, 15)
        "2018.12.15 08:31:43".assertAsCorrectDate(2018, 12, 15, 8, 31, 43)
        "2015.07.15 13:07:12".assertAsCorrectDate(2015, 7, 15, 13, 7, 12)
        "1997.08.03 18:30:00".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003.06.01 01:31:56".assertAsCorrectDate(2003, 6, 1, 1, 31, 56)
        "2019.11.20 23:01:12".assertAsCorrectDate(2019, 11, 20, 23, 1, 12)
        "2012.12.12 12:12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 12)
        "1995.09.10 08:41:28".assertAsCorrectDate(1995, 9, 10, 8, 41, 28)
        "2018.04.18 14:15:53".assertAsCorrectDate(2018, 4, 18, 14, 15, 53)
        "2018.04.18 14:15:34".assertAsCorrectDate(2018, 4, 18, 14, 15, 34)
        "2011.01.01 00:02:42".assertAsCorrectDate(2011, 1, 1, 0, 2, 42)
        "2013.04.26 18:12:19".assertAsCorrectDate(2013, 4, 26, 18, 12, 19)
    }

    // yyyy.MM.dd kk:mm
    @Test
    fun `should parse date format #6`() {
        "2011.03.07 10:30".assertAsCorrectDate(2011, 3, 7, 10, 30, 0)
        "2007.06.06 21:53".assertAsCorrectDate(2007, 6, 6, 21, 53, 0)
        "2017.12.31 14:41".assertAsCorrectDate(2017, 12, 31, 14, 41, 0)
        "2014.11.12 04:41".assertAsCorrectDate(2014, 11, 12, 4, 41, 0)
        "2018.12.15 08:31".assertAsCorrectDate(2018, 12, 15, 8, 31, 0)
        "2015.07.15 13:07".assertAsCorrectDate(2015, 7, 15, 13, 7, 0)
        "1997.08.03 18:30".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003.06.01 01:31".assertAsCorrectDate(2003, 6, 1, 1, 31, 0)
        "2019.11.20 23:01".assertAsCorrectDate(2019, 11, 20, 23, 1, 0)
        "2012.12.12 12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 0)
        "1995.09.10 08:41".assertAsCorrectDate(1995, 9, 10, 8, 41, 0)
        "2018.04.18 14:15".assertAsCorrectDate(2018, 4, 18, 14, 15, 0)
        "2018.04.18 14:15".assertAsCorrectDate(2018, 4, 18, 14, 15, 0)
        "2011.01.01 00:02".assertAsCorrectDate(2011, 1, 1, 0, 2, 0)
        "2013.04.26 18:12".assertAsCorrectDate(2013, 4, 26, 18, 12, 0)
        "2010.08.03 12:00".assertAsCorrectDate(2010, 8, 3, 12, 0, 0)
    }

    // yyyy.MM.dd kk.mm.ss
    @Test
    fun `should parse date format #7`() {
        "2011.03.07 10.30.20".assertAsCorrectDate(2011, 3, 7, 10, 30, 20)
        "2007.06.06 21.53.17".assertAsCorrectDate(2007, 6, 6, 21, 53, 17)
        "2017.12.31 14.41.12".assertAsCorrectDate(2017, 12, 31, 14, 41, 12)
        "2014.11.12 04.41.15".assertAsCorrectDate(2014, 11, 12, 4, 41, 15)
        "2018.12.15 08.31.43".assertAsCorrectDate(2018, 12, 15, 8, 31, 43)
        "2015.07.15 13.07.12".assertAsCorrectDate(2015, 7, 15, 13, 7, 12)
        "1997.08.03 18.30.00".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003.06.01 01.31.56".assertAsCorrectDate(2003, 6, 1, 1, 31, 56)
        "2019.11.20 23.01.12".assertAsCorrectDate(2019, 11, 20, 23, 1, 12)
        "2012.12.12 12.12.12".assertAsCorrectDate(2012, 12, 12, 12, 12, 12)
        "1995.09.10 08.41.28".assertAsCorrectDate(1995, 9, 10, 8, 41, 28)
        "2018.04.18 14.15.53".assertAsCorrectDate(2018, 4, 18, 14, 15, 53)
        "2018.04.18 14.15.34".assertAsCorrectDate(2018, 4, 18, 14, 15, 34)
        "2011.01.01 00.02.42".assertAsCorrectDate(2011, 1, 1, 0, 2, 42)
        "2013.04.26 18.12.19".assertAsCorrectDate(2013, 4, 26, 18, 12, 19)
    }

    // yyyy.MM.dd kk.mm
    @Test
    fun `should parse date format #8`() {
        "2011.03.07 10.30".assertAsCorrectDate(2011, 3, 7, 10, 30, 0)
        "2007.06.06 21.53".assertAsCorrectDate(2007, 6, 6, 21, 53, 0)
        "2017.12.31 14.41".assertAsCorrectDate(2017, 12, 31, 14, 41, 0)
        "2014.11.12 04.41".assertAsCorrectDate(2014, 11, 12, 4, 41, 0)
        "2018.12.15 08.31".assertAsCorrectDate(2018, 12, 15, 8, 31, 0)
        "2015.07.15 13.07".assertAsCorrectDate(2015, 7, 15, 13, 7, 0)
        "1997.08.03 18.30".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003.06.01 01.31".assertAsCorrectDate(2003, 6, 1, 1, 31, 0)
        "2019.11.20 23.01".assertAsCorrectDate(2019, 11, 20, 23, 1, 0)
        "2012.12.12 12.12".assertAsCorrectDate(2012, 12, 12, 12, 12, 0)
        "1995.09.10 08.41".assertAsCorrectDate(1995, 9, 10, 8, 41, 0)
        "2018.04.18 14.15".assertAsCorrectDate(2018, 4, 18, 14, 15, 0)
        "2018.04.18 14.15".assertAsCorrectDate(2018, 4, 18, 14, 15, 0)
        "2011.01.01 00.02".assertAsCorrectDate(2011, 1, 1, 0, 2, 0)
        "2013.04.26 18.12".assertAsCorrectDate(2013, 4, 26, 18, 12, 0)
    }

    // dd/MM/yyyy kk:mm:ss
    @Test
    fun `should parse date format #9`() {
        "07/03/2011 10:30:20".assertAsCorrectDate(2011, 3, 7, 10, 30, 20)
        "06/06/2007 21:53:17".assertAsCorrectDate(2007, 6, 6, 21, 53, 17)
        "31/12/2017 14:41:12".assertAsCorrectDate(2017, 12, 31, 14, 41, 12)
        "12/11/2014 04:41:15".assertAsCorrectDate(2014, 11, 12, 4, 41, 15)
        "15/12/2018 08:31:43".assertAsCorrectDate(2018, 12, 15, 8, 31, 43)
        "15/07/2015 13:07:12".assertAsCorrectDate(2015, 7, 15, 13, 7, 12)
        "03/08/1997 18:30:00".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "01/06/2003 01:31:56".assertAsCorrectDate(2003, 6, 1, 1, 31, 56)
        "20/11/2019 23:01:12".assertAsCorrectDate(2019, 11, 20, 23, 1, 12)
        "12/12/2012 12:12:28".assertAsCorrectDate(2012, 12, 12, 12, 12, 28)
        "10/09/1995 08:41:53".assertAsCorrectDate(1995, 9, 10, 8, 41, 53)
        "24/02/2018 10:46:34".assertAsCorrectDate(2018, 2, 24, 10, 46, 34)
    }

    // dd/MM/yyyy kk:mm
    @Test
    fun `should parse date format #10`() {
        "07/03/2011 10:30".assertAsCorrectDate(2011, 3, 7, 10, 30, 0)
        "06/06/2007 21:53".assertAsCorrectDate(2007, 6, 6, 21, 53, 0)
        "31/12/2017 14:41".assertAsCorrectDate(2017, 12, 31, 14, 41, 0)
        "12/11/2014 04:41".assertAsCorrectDate(2014, 11, 12, 4, 41, 0)
        "15/12/2018 08:31".assertAsCorrectDate(2018, 12, 15, 8, 31, 0)
        "15/07/2015 13:07".assertAsCorrectDate(2015, 7, 15, 13, 7, 0)
        "03/08/1997 18:30".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "01/06/2003 01:31".assertAsCorrectDate(2003, 6, 1, 1, 31, 0)
        "20/11/2019 23:01".assertAsCorrectDate(2019, 11, 20, 23, 1, 0)
        "12/12/2012 12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 0)
        "10/09/1995 08:41".assertAsCorrectDate(1995, 9, 10, 8, 41, 0)
        "24/02/2018 10:46".assertAsCorrectDate(2018, 2, 24, 10, 46, 0)
    }

    // yyyy/MM/dd kk:mm:ss
    @Test
    fun `should parse date format #11`() {
        "2011/03/07 10:30:20".assertAsCorrectDate(2011, 3, 7, 10, 30, 20)
        "2007/06/06 21:53:17".assertAsCorrectDate(2007, 6, 6, 21, 53, 17)
        "2017/12/31 14:41:12".assertAsCorrectDate(2017, 12, 31, 14, 41, 12)
        "2014/11/12 04:41:15".assertAsCorrectDate(2014, 11, 12, 4, 41, 15)
        "2018/12/15 08:31:43".assertAsCorrectDate(2018, 12, 15, 8, 31, 43)
        "2015/07/15 13:07:12".assertAsCorrectDate(2015, 7, 15, 13, 7, 12)
        "1997/08/03 18:30:00".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003/06/01 01:31:56".assertAsCorrectDate(2003, 6, 1, 1, 31, 56)
        "2019/11/20 23:01:12".assertAsCorrectDate(2019, 11, 20, 23, 1, 12)
        "2012/12/12 12:12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 12)
        "1995/09/10 08:41:28".assertAsCorrectDate(1995, 9, 10, 8, 41, 28)
        "2018/04/18 14:15:53".assertAsCorrectDate(2018, 4, 18, 14, 15, 53)
        "2018/04/18 14:15:34".assertAsCorrectDate(2018, 4, 18, 14, 15, 34)
    }

    // yyyy/MM/dd kk:mm
    @Test
    fun `should parse date format #12`() {
        "2011/03/07 10:30".assertAsCorrectDate(2011, 3, 7, 10, 30, 0)
        "2007/06/06 21:53".assertAsCorrectDate(2007, 6, 6, 21, 53, 0)
        "2017/12/31 14:41".assertAsCorrectDate(2017, 12, 31, 14, 41, 0)
        "2014/11/12 04:41".assertAsCorrectDate(2014, 11, 12, 4, 41, 0)
        "2018/12/15 08:31".assertAsCorrectDate(2018, 12, 15, 8, 31, 0)
        "2015/07/15 13:07".assertAsCorrectDate(2015, 7, 15, 13, 7, 0)
        "1997/08/03 18:30".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003/06/01 01:31".assertAsCorrectDate(2003, 6, 1, 1, 31, 0)
        "2019/11/20 23:01".assertAsCorrectDate(2019, 11, 20, 23, 1, 0)
        "2012/12/12 12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 0)
        "1995/09/10 08:41".assertAsCorrectDate(1995, 9, 10, 8, 41, 0)
        "2018/04/18 14:15".assertAsCorrectDate(2018, 4, 18, 14, 15, 0)
        "2018/04/18 14:15".assertAsCorrectDate(2018, 4, 18, 14, 15, 0)
    }

    // yyyy-MM-dd kk:mm:ss
    @Test
    fun `should parse date format #13`() {
        "2011-03-07 10:30:20".assertAsCorrectDate(2011, 3, 7, 10, 30, 20)
        "2007-06-06 21:53:17".assertAsCorrectDate(2007, 6, 6, 21, 53, 17)
        "2017-12-31 14:41:12".assertAsCorrectDate(2017, 12, 31, 14, 41, 12)
        "2014-11-12 04:41:15".assertAsCorrectDate(2014, 11, 12, 4, 41, 15)
        "2018-12-15 08:31:43".assertAsCorrectDate(2018, 12, 15, 8, 31, 43)
        "2015-07-15 13:07:12".assertAsCorrectDate(2015, 7, 15, 13, 7, 12)
        "1997-08-03 18:30:00".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003-06-01 01:31:56".assertAsCorrectDate(2003, 6, 1, 1, 31, 56)
        "2019-11-20 23:01:12".assertAsCorrectDate(2019, 11, 20, 23, 1, 12)
        "2012-12-12 12:12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 12)
        "1995-09-10 08:41:28".assertAsCorrectDate(1995, 9, 10, 8, 41, 28)
    }

    // yyyy-MM-dd kk:mm
    @Test
    fun `should parse date format #14`() {
        "2011-03-07 10:30".assertAsCorrectDate(2011, 3, 7, 10, 30, 0)
        "2007-06-06 21:53".assertAsCorrectDate(2007, 6, 6, 21, 53, 0)
        "2017-12-31 14:41".assertAsCorrectDate(2017, 12, 31, 14, 41, 0)
        "2014-11-12 04:41".assertAsCorrectDate(2014, 11, 12, 4, 41, 0)
        "2018-12-15 08:31".assertAsCorrectDate(2018, 12, 15, 8, 31, 0)
        "2015-07-15 13:07".assertAsCorrectDate(2015, 7, 15, 13, 7, 0)
        "1997-08-03 18:30".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003-06-01 01:31".assertAsCorrectDate(2003, 6, 1, 1, 31, 0)
        "2019-11-20 23:01".assertAsCorrectDate(2019, 11, 20, 23, 1, 0)
        "2012-12-12 12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 0)
        "1995-09-10 08:41".assertAsCorrectDate(1995, 9, 10, 8, 41, 0)
    }

    // yyyy-MM-dd'T'kk:mm:ssZ
    @Test
    fun `should parse date format #15`() {
        "2011-03-07T10:30:20+0300".assertAsCorrectDate(2011, 3, 7, 10, 30, 20, 3)
        "2007-11-06T21:53:17+0300".assertAsCorrectDate(2007, 11, 6, 21, 53, 17, 3)
        "2017-12-31T14:41:12-0800".assertAsCorrectDate(2017, 12, 31, 14, 41, 12, -8)
        "2014-11-12T04:41:15-0100".assertAsCorrectDate(2014, 11, 12, 4, 41, 15, -1)
        "2018-12-15T08:31:43-0000".assertAsCorrectDate(2018, 12, 15, 8, 31, 43, 0)
        "2015-07-15T13:07:12+1300".assertAsCorrectDate(2015, 7, 15, 13, 7, 12, 13)
        "1997-02-03T18:30:00+0300".assertAsCorrectDate(1997, 2, 3, 18, 30, 0, 3)
        "2003-03-01T01:31:56-0300".assertAsCorrectDate(2003, 3, 1, 1, 31, 56, -3)
        "2019-11-20T23:01:12-2200".assertAsCorrectDate(2019, 11, 20, 23, 1, 12, -22)
        "2015-12-12T12:12:12+0500".assertAsCorrectDate(2015, 12, 12, 12, 12, 12, 5)
        "1995-01-10T08:41:28+0300".assertAsCorrectDate(1995, 1, 10, 8, 41, 28, 3)
    }

    // yyyy-MM-dd'T'kk:mmZ
    @Test
    fun `should parse date format #16`() {
        "2011-03-07T10:30+0300".assertAsCorrectDate(2011, 3, 7, 10, 30, 0, 3)
        "2007-11-06T21:53+0300".assertAsCorrectDate(2007, 11, 6, 21, 53, 0, 3)
        "2017-12-31T14:41-0800".assertAsCorrectDate(2017, 12, 31, 14, 41, 0, -8)
        "2014-11-12T04:41-0100".assertAsCorrectDate(2014, 11, 12, 4, 41, 0, -1)
        "2018-12-15T08:31-0000".assertAsCorrectDate(2018, 12, 15, 8, 31, 0, 0)
        "2015-07-15T13:07+1300".assertAsCorrectDate(2015, 7, 15, 13, 7, 0, 13)
        "1997-02-03T18:30+0300".assertAsCorrectDate(1997, 2, 3, 18, 30, 0, 3)
        "2003-03-01T01:31-0300".assertAsCorrectDate(2003, 3, 1, 1, 31, 0, -3)
        "2019-11-20T23:01-2200".assertAsCorrectDate(2019, 11, 20, 23, 1, 0, -22)
        "2015-12-12T12:12+0500".assertAsCorrectDate(2015, 12, 12, 12, 12, 0, 5)
        "1995-01-10T08:41+0300".assertAsCorrectDate(1995, 1, 10, 8, 41, 0, 3)
    }

    // yyyy-MM-dd'T'kk:mm:ssXXX
    @Test
    fun `should parse date format #17`() {
        "2011-03-07T10:30:20+03:00".assertAsCorrectDate(2011, 3, 7, 10, 30, 20, 3)
        "2007-11-06T21:53:17+03:00".assertAsCorrectDate(2007, 11, 6, 21, 53, 17, 3)
        "2017-12-31T14:41:12-08:00".assertAsCorrectDate(2017, 12, 31, 14, 41, 12, -8)
        "2014-11-12T04:41:15-01:00".assertAsCorrectDate(2014, 11, 12, 4, 41, 15, -1)
        "2018-12-15T08:31:43-00:00".assertAsCorrectDate(2018, 12, 15, 8, 31, 43, 0)
        "2015-07-15T13:07:12+13:00".assertAsCorrectDate(2015, 7, 15, 13, 7, 12, 13)
        "1997-02-03T18:30:00+03:00".assertAsCorrectDate(1997, 2, 3, 18, 30, 0, 3)
        "2003-03-01T01:31:56-03:00".assertAsCorrectDate(2003, 3, 1, 1, 31, 56, -3)
        "2019-11-20T23:01:12-22:00".assertAsCorrectDate(2019, 11, 20, 23, 1, 12, -22)
        "2015-12-12T12:12:12+05:00".assertAsCorrectDate(2015, 12, 12, 12, 12, 12, 5)
        "1995-01-10T08:41:28+03:00".assertAsCorrectDate(1995, 1, 10, 8, 41, 28, 3)
    }

    // yyyy-MM-dd'T'kk:mmXXX
    @Test
    fun `should parse date format #18`() {
        "2011-03-07T10:30+03:00".assertAsCorrectDate(2011, 3, 7, 10, 30, 0, 3)
        "2007-11-06T21:53+03:00".assertAsCorrectDate(2007, 11, 6, 21, 53, 0, 3)
        "2017-12-31T14:41-08:00".assertAsCorrectDate(2017, 12, 31, 14, 41, 0, -8)
        "2014-11-12T04:41-01:00".assertAsCorrectDate(2014, 11, 12, 4, 41, 0, -1)
        "2018-12-15T08:31-00:00".assertAsCorrectDate(2018, 12, 15, 8, 31, 0, 0)
        "2015-07-15T13:07+13:00".assertAsCorrectDate(2015, 7, 15, 13, 7, 0, 13)
        "1997-02-03T18:30+03:00".assertAsCorrectDate(1997, 2, 3, 18, 30, 0, 3)
        "2003-03-01T01:31-03:00".assertAsCorrectDate(2003, 3, 1, 1, 31, 0, -3)
        "2019-11-20T23:01-22:00".assertAsCorrectDate(2019, 11, 20, 23, 1, 0, -22)
        "2015-12-12T12:12+05:00".assertAsCorrectDate(2015, 12, 12, 12, 12, 0, 5)
        "1995-01-10T08:41+03:00".assertAsCorrectDate(1995, 1, 10, 8, 41, 0, 3)
    }

    // yyyy-MM-dd'T'kk:mm:ss
    @Test
    fun `should parse date format #19`() {
        "2011-03-07T10:30:20".assertAsCorrectDate(2011, 3, 7, 10, 30, 20)
        "2007-06-06T21:53:17".assertAsCorrectDate(2007, 6, 6, 21, 53, 17)
        "2017-12-31T14:41:12".assertAsCorrectDate(2017, 12, 31, 14, 41, 12)
        "2014-11-12T04:41:15".assertAsCorrectDate(2014, 11, 12, 4, 41, 15)
        "2018-12-15T08:31:43".assertAsCorrectDate(2018, 12, 15, 8, 31, 43)
        "2015-07-15T13:07:12".assertAsCorrectDate(2015, 7, 15, 13, 7, 12)
        "1997-08-03T18:30:00".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003-06-01T01:31:56".assertAsCorrectDate(2003, 6, 1, 1, 31, 56)
        "2019-11-20T23:01:12".assertAsCorrectDate(2019, 11, 20, 23, 1, 12)
        "2012-12-12T12:12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 12)
        "1995-09-10T08:41:28".assertAsCorrectDate(1995, 9, 10, 8, 41, 28)
    }

    // yyyy-MM-dd'T'kk:mm
    @Test
    fun `should parse date format #20`() {
        "2011-03-07T10:30".assertAsCorrectDate(2011, 3, 7, 10, 30, 0)
        "2007-06-06T21:53".assertAsCorrectDate(2007, 6, 6, 21, 53, 0)
        "2017-12-31T14:41".assertAsCorrectDate(2017, 12, 31, 14, 41, 0)
        "2014-11-12T04:41".assertAsCorrectDate(2014, 11, 12, 4, 41, 0)
        "2018-12-15T08:31".assertAsCorrectDate(2018, 12, 15, 8, 31, 0)
        "2015-07-15T13:07".assertAsCorrectDate(2015, 7, 15, 13, 7, 0)
        "1997-08-03T18:30".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003-06-01T01:31".assertAsCorrectDate(2003, 6, 1, 1, 31, 0)
        "2019-11-20T23:01".assertAsCorrectDate(2019, 11, 20, 23, 1, 0)
        "2012-12-12T12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 0)
        "1995-09-10T08:41".assertAsCorrectDate(1995, 9, 10, 8, 41, 0)
    }

    // yyyy:MM:dd:kk:mm:ss
    @Test
    fun `should parse date format #21`() {
        "2011:03:07:10:30:20".assertAsCorrectDate(2011, 3, 7, 10, 30, 20)
        "2007:06:06:21:53:17".assertAsCorrectDate(2007, 6, 6, 21, 53, 17)
        "2017:12:31:14:41:12".assertAsCorrectDate(2017, 12, 31, 14, 41, 12)
        "2014:11:12:04:41:15".assertAsCorrectDate(2014, 11, 12, 4, 41, 15)
        "2018:12:15:08:31:43".assertAsCorrectDate(2018, 12, 15, 8, 31, 43)
        "2015:07:15:13:07:12".assertAsCorrectDate(2015, 7, 15, 13, 7, 12)
        "1997:08:03:18:30:00".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003:06:01:01:31:56".assertAsCorrectDate(2003, 6, 1, 1, 31, 56)
        "2019:11:20:23:01:12".assertAsCorrectDate(2019, 11, 20, 23, 1, 12)
        "2012:12:12:12:12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 12)
        "1995:09:10:08:41:28".assertAsCorrectDate(1995, 9, 10, 8, 41, 28)
    }

    // yyyy:MM:dd:kk:mm
    @Test
    fun `should parse date format #22`() {
        "2011:03:07:10:30".assertAsCorrectDate(2011, 3, 7, 10, 30, 0)
        "2007:06:06:21:53".assertAsCorrectDate(2007, 6, 6, 21, 53, 0)
        "2017:12:31:14:41".assertAsCorrectDate(2017, 12, 31, 14, 41, 0)
        "2014:11:12:04:41".assertAsCorrectDate(2014, 11, 12, 4, 41, 0)
        "2018:12:15:08:31".assertAsCorrectDate(2018, 12, 15, 8, 31, 0)
        "2015:07:15:13:07".assertAsCorrectDate(2015, 7, 15, 13, 7, 0)
        "1997:08:03:18:30".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003:06:01:01:31".assertAsCorrectDate(2003, 6, 1, 1, 31, 0)
        "2019:11:20:23:01".assertAsCorrectDate(2019, 11, 20, 23, 1, 0)
        "2012:12:12:12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 0)
        "1995:09:10:08:41".assertAsCorrectDate(1995, 9, 10, 8, 41, 0)
    }

    // yyyy-MM-dd_kk:mm:ss
    @Test
    fun `should parse date format #23`() {
        "2011-03-07_10:30:20".assertAsCorrectDate(2011, 3, 7, 10, 30, 20)
        "2007-06-06_21:53:17".assertAsCorrectDate(2007, 6, 6, 21, 53, 17)
        "2017-12-31_14:41:12".assertAsCorrectDate(2017, 12, 31, 14, 41, 12)
        "2014-11-12_04:41:15".assertAsCorrectDate(2014, 11, 12, 4, 41, 15)
        "2018-12-15_08:31:43".assertAsCorrectDate(2018, 12, 15, 8, 31, 43)
        "2015-07-15_13:07:12".assertAsCorrectDate(2015, 7, 15, 13, 7, 12)
        "1997-08-03_18:30:00".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003-06-01_01:31:56".assertAsCorrectDate(2003, 6, 1, 1, 31, 56)
        "2019-11-20_23:01:12".assertAsCorrectDate(2019, 11, 20, 23, 1, 12)
        "2012-12-12_12:12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 12)
        "1995-09-10_08:41:28".assertAsCorrectDate(1995, 9, 10, 8, 41, 28)
    }

    // yyyy-MM-dd_kk:mm
    @Test
    fun `should parse date format #24`() {
        "2011-03-07_10:30".assertAsCorrectDate(2011, 3, 7, 10, 30, 0)
        "2007-06-06_21:53".assertAsCorrectDate(2007, 6, 6, 21, 53, 0)
        "2017-12-31_14:41".assertAsCorrectDate(2017, 12, 31, 14, 41, 0)
        "2014-11-12_04:41".assertAsCorrectDate(2014, 11, 12, 4, 41, 0)
        "2018-12-15_08:31".assertAsCorrectDate(2018, 12, 15, 8, 31, 0)
        "2015-07-15_13:07".assertAsCorrectDate(2015, 7, 15, 13, 7, 0)
        "1997-08-03_18:30".assertAsCorrectDate(1997, 8, 3, 18, 30, 0)
        "2003-06-01_01:31".assertAsCorrectDate(2003, 6, 1, 1, 31, 0)
        "2019-11-20_23:01".assertAsCorrectDate(2019, 11, 20, 23, 1, 0)
        "2012-12-12_12:12".assertAsCorrectDate(2012, 12, 12, 12, 12, 0)
        "1995-09-10_08:41".assertAsCorrectDate(1995, 9, 10, 8, 41, 0)
    }

    @Test
    fun `should parse timestamp`() {
        "1563545799017".assertAsCorrectTimestamp(1563545799017)
        "1563689913028".assertAsCorrectTimestamp(1563689913028)
        "1563689945123".assertAsCorrectTimestamp(1563689945123)
        "1563689982538".assertAsCorrectTimestamp(1563689982538)
        "1563690011663".assertAsCorrectTimestamp(1563690011663)
        "1563690043863".assertAsCorrectTimestamp(1563690043863)
        "1563654481428".assertAsCorrectTimestamp(1563654481428)
        "1563654303198".assertAsCorrectTimestamp(1563654303198)
        "1563710154965".assertAsCorrectTimestamp(1563710154965)
        "1563710247265".assertAsCorrectTimestamp(1563710247265)
        "1563710258615".assertAsCorrectTimestamp(1563710258615)
        "1563123916489".assertAsCorrectTimestamp(1563123916489)
        "1563724492669".assertAsCorrectTimestamp(1563724492669)
        "1563710085431".assertAsCorrectTimestamp(1563710085431)
        "1563710094950".assertAsCorrectTimestamp(1563710094950)
    }

    @Test
    fun `should read null when DateTimeOriginal, DateTimeDigitized and DateTime are all null`() {
        getResource(FILE_WITHOUT_DATES).use { input ->
            val actualDate = exifDateExtractor.extract(input)
            assertDateExtractedSuccessful(FILE_WITHOUT_DATES, null, actualDate)
        }
    }

    @Test
    fun `should read DateTime when DateTimeOriginal and DateTimeDigitized are both null`() {
        FILE_WITH_DATETIME.assertFileCreationTime(2017, 6, 15, 4, 33, 51)
    }

    @Test
    fun `should read DateTimeDigitized when DateTimeOriginal is null`() {
        FILE_WITH_DATETIME_DIGITIZED.assertFileCreationTime(2015, 11, 21, 20, 46, 10)
    }

    @Test
    fun `should read DateTimeOriginal`() {
        FILE_WITH_DATETIME_ORIGINAL.assertFileCreationTime(2014, 12, 13, 17, 29, 24)
    }

    private fun Path.assertFileCreationTime(year: Int, month: Int, day: Int,
                                            hour: Int, minute: Int, second: Int) {
        getResource(this).use { input ->
            val calendar = GregorianCalendar(year, month - 1, day, hour, minute, second)
            val expectedDate = calendar.time
            val actualDate = exifDateExtractor.extract(input)
            assertDateExtractedSuccessful(this, expectedDate, actualDate)
        }
    }

    private fun assertDateExtractedSuccessful(path: Path, expectedDate: Date?, actualDate: Date?) {
        assertThat("Result of extracting exif datetime from $path is '$actualDate'." +
            " Expected is '$expectedDate'.", actualDate == expectedDate)
    }

    private fun String.assertAsCorrectDate(year: Int, month: Int, day: Int,
                                           hour: Int, minute: Int, second: Int,
                                           timeZoneOffset: Int? = null) {
        val calendar = GregorianCalendar(year, month - 1, day, hour, minute, second)
        if (timeZoneOffset != null) {
            calendar.add(Calendar.HOUR, localTimeZoneOffset - timeZoneOffset)
        }
        val expectedDate = calendar.time
        val actualDate = exifDateExtractor.parseFormat(this)
        assertDateParsedSuccessful(this, expectedDate, actualDate)
    }

    private fun String.assertAsCorrectTimestamp(timestamp: Long) {
        val expectedDate = Date(timestamp)
        val actualDate = exifDateExtractor.parseFormat(this)
        assertDateParsedSuccessful(this, expectedDate, actualDate)
    }

    private fun assertDateParsedSuccessful(string: String, expectedDate: Date?, actualDate: Date?) {
        assertThat("Result of parsing '$string' is '$actualDate' (${actualDate?.time})." +
            " Expected is '$expectedDate' (${expectedDate?.time}).",
            actualDate == expectedDate)
    }

    private fun getResource(path: Path): InputStream {
        val resource = this::class.java.getResourceAsStream(path.path)
        assertThat("Resource $path should be readable.", resource != null)
        return resource!!
    }
}
