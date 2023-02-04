package ru.yandex.solomon.yasm.alert.converter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.ambry.dto.YasmAlertDto;
import ru.yandex.solomon.alert.protobuf.TAlert;
import ru.yandex.solomon.expression.analytics.Program;
import ru.yandex.solomon.expression.compile.DeprOpts;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class YasmAlertConverterTest {
    private static final ObjectMapper mapper = new ObjectMapper();
    private YasmAlertConverter converter;

    @Before
    public void setUp() {
        this.converter = new YasmAlertConverter("yasm_");
    }

    @Test
    public void simple() throws IOException {
        String yasmAlertJson = "{\"updated\": \"2020-06-23T12:29:37.029000\", \"name\": \"1\", \"tags\": \"ctype=prod;" +
                "geo=man;itype=mmeta;prj=imgs-main\", \"signal\": \"quant(unistat-market-comm-docs-validated-perc_dhhh, 1)\", " +
                "\"warn\": [4, 5], \"crit\": [null, 4], \"mgroups\": [\"ASEARCH\"]}";

        YasmAlertDto yasmAlert = mapper.readValue(yasmAlertJson, YasmAlertDto.class);

        TAlert alert = converter.convertAlert(yasmAlert).build();

        Assert.assertEquals(alert.getPeriodMillis(), TimeUnit.MINUTES.toMillis(5));
        String expectedProgram = """
                use {project='yasm_mmeta', geo='man', ctype='prod', prj='imgs-main'};
                // quant(unistat-market-comm-docs-validated-perc_dhhh, 1)
                let signal = histogram_percentile(10, '', {signal='unistat-market-comm-docs-validated-perc_dhhh'});
                let value = last(signal);
                no_data_if(value != value);
                warn_if(value >= 4.0 && value <= 5.0);
                alarm_if(value <= 4.0);""";

        Assert.assertEquals(expectedProgram, alert.getExpression().getProgram());
        Program.fromSource(alert.getExpression().getProgram())
                .withDeprOpts(DeprOpts.ALERTING)
                .compile();
    }

    @Test
    public void withUnits() throws IOException {
        String yasmAlertJson = "{\"name\": \"withUnits\", \"tags\": \"ctype=prod;" +
                "geo=man;itype=mmeta;prj=imgs-main\", \"signal\": \"perc(conv(foo, Gi), conv(bar, Mi, Ki))\", " +
                "\"warn\": [4, 5], \"crit\": [null, 4], \"mgroups\": [\"ASEARCH\"]}";

        YasmAlertDto yasmAlert = mapper.readValue(yasmAlertJson, YasmAlertDto.class);

        TAlert alert = converter.convertAlert(yasmAlert).build();

        Assert.assertEquals(alert.getPeriodMillis(), TimeUnit.MINUTES.toMillis(5));
        String expectedProgram = """
                use {project='yasm_mmeta', geo='man', ctype='prod', prj='imgs-main'};
                let Mi = 1048576.0;
                let Gi = 1.073741824E9;
                let Ki = 1024.0;
                // perc(conv(foo, Gi), conv(bar, Mi, Ki))
                let signal = 100 * (({signal='foo'} / Gi) / ({signal='bar'} * (Mi / Ki)));
                let value = last(signal);
                no_data_if(value != value);
                warn_if(value >= 4.0 && value <= 5.0);
                alarm_if(value <= 4.0);""";

        Assert.assertEquals(expectedProgram, alert.getExpression().getProgram());
        Program.fromSource(alert.getExpression().getProgram())
                .withDeprOpts(DeprOpts.ALERTING)
                .compile();
    }

    @Test
    public void trend() throws IOException {
        String yasmAlertJson = "{\"disaster\": false, \"warn_perc\": 10, \"description\": \"\", \"tags\": {\"itype\":" +
                " [\"mdsproxy\"], \"ctype\": [\"prestable\", \"production\"], \"prj\": [\"adfox-content\"]}, " +
                "\"trend\": \"down\", \"signal\": \"mdsproxy_unistat-s3mds_nginx_2xx_dmmm\", \"interval\": 600, " +
                "\"updated\": \"2019-11-18T09:17:12.439000\", \"juggler_check\": {\"host\": " +
                "\"yasm_ADFOX_MDS_2xx_trend\", \"service\": \"ADFOX_MDS\"}, \"interval_modify\": {\"type\": \"aver\"}," +
                "\"crit_perc\": 33, \"mgroups\": [\"CON\"], \"name\": \"ADFOX_MDS_2xx_trend\"}";

        YasmAlertDto yasmAlert = mapper.readValue(yasmAlertJson, YasmAlertDto.class);

        TAlert alert = converter.convertAlert(yasmAlert).build();

        Assert.assertEquals(alert.getPeriodMillis(), TimeUnit.MINUTES.toMillis(10));
        String expectedProgram = """
                use {project='yasm_mdsproxy', ctype='prestable|production', prj='adfox-content'};
                // mdsproxy_unistat-s3mds_nginx_2xx_dmmm
                let signal = {signal='mdsproxy_unistat-s3mds_nginx_2xx_dmmm'};
                let train = signal;
                let test = signal;
                let trainValue = avg(train);
                let testValue = last(test);
                let deltaPerc = 100 * (trainValue - testValue) / trainValue;
                alarm_if(deltaPerc >= 33.0);
                warn_if(deltaPerc >= 10.0);""";
        Assert.assertEquals(expectedProgram, alert.getExpression().getProgram());
        Program.fromSource(alert.getExpression().getProgram())
                .withDeprOpts(DeprOpts.ALERTING)
                .compile();
    }
}
