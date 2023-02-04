package ru.yandex.webmaster3.storage.searchquery.download;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.util.TimeUtils;
import ru.yandex.webmaster3.proto.WebmasterCommonProto;
import ru.yandex.webmaster3.proto.searchquery.download.CsvRequest;

/**
 * @author aherman
 */
public class CSVRequestEncoderTest {
    @Test
    public void decode() throws Exception {
        Instant instant = Instant.parse("2016-12-06T10:11:12");

        Instant dateFrom = Instant.parse("2016-11-01");
        Instant dateTo = Instant.parse("2016-12-05");

        long userId = 12345L;
        int requestDate = TimeUtils.instantToUnixTimestamp(instant);

        CsvRequest.CSVRequest request = CsvRequest.CSVRequest.newBuilder()
                .setHostId("http:example.com:80")
                .setUserId(userId)
                .setUserIp(ByteString.copyFrom(new byte[]{127, 0, 0, 1}))
                .setCsvMode(CsvRequest.CSVRequest.CSVMode.ONE_GROUP__ALL_INDICATORS__BY_DATES)
                .setRequestDate(requestDate)
                .setBalancerRequestId("1481058052737840-17214221759468028480")
                .setDateFrom(TimeUtils.instantToUnixTimestamp(dateFrom))
                .setDateTo(TimeUtils.instantToUnixTimestamp(dateTo))
                .setAggregatePeriod(2)
                .addRegions(1)
                .addRegions(2)
                .addRegions(3)
                .addRegions(4)
                .addRegions(5)
                .addRegions(6)
                .addRegions(7)
                .addRegions(8)
                .addRegions(9)
                .addRegions(10)
                .addRegions(11)
                .addRegions(12)
                .addRegions(13)
                .addRegions(14)
                .addRegions(15)
                .addRegions(16)
                .addRegions(17)
                .addRegions(18)
                .addRegions(19)
                .addRegions(20)
                .addRegions(21)
                .setRegionInclusion(1)
                .addIndicators(1)
                .addIndicators(2)
                .addIndicators(3)
                .addIndicators(4)
                .addIndicators(5)
                .addIndicators(6)
                .addIndicators(7)
                .addIndicators(8)
                .addIndicators(9)
                .addIndicators(10)
                .addIndicators(11)
                .addIndicators(12)
                .setGroupId(WebmasterCommonProto.UUID.newBuilder().setLeastSignificantBits(0).setMostSignificantBits(1))
                .setQueryId(30L)
                .setOrderBy(10)
                .setOrderDirection(2)
                .build();

        EncodedCSVRequest encoded = CSVRequestEncoder.encode(userId, instant, request);

        Optional<CsvRequest.CSVRequestOrBuilder> decodedO =
                CSVRequestEncoder.decode(userId, instant.plus(Duration.standardMinutes(5)),
                        (int) TimeUnit.MINUTES.toSeconds(10), encoded);

        Assert.assertTrue(decodedO.isPresent());
        Assert.assertEquals(request, decodedO.get());
    }
}