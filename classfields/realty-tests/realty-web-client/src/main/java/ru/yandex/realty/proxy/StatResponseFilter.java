package ru.yandex.realty.proxy;

import io.netty.handler.codec.http.HttpResponse;
import net.lightbody.bmp.filters.ResponseFilter;
import net.lightbody.bmp.filters.ResponseFilterAdapter;
import net.lightbody.bmp.util.HttpMessageContents;
import net.lightbody.bmp.util.HttpMessageInfo;
import org.hamcrest.Matcher;

import java.util.function.Function;

/**
 * @author kurau (Yuri Kalinin)
 */
public class StatResponseFilter implements ResponseFilter {

    private Matcher matcher;
    private Function<String, String> formatter;

    public static ResponseFilterAdapter.FilterSource statResponseFilter(Matcher matcher,
                                                                        Function<String, String> formatter) {
        return new ResponseFilterAdapter.FilterSource(new StatResponseFilter(matcher, formatter), 16777216);
    }

    private StatResponseFilter(Matcher matcher, Function<String, String> formatter) {
        this.matcher = matcher;
        this.formatter = formatter;
    }

    @Override
    public void filterResponse(HttpResponse httpResponse, HttpMessageContents httpMessageContents,
                               HttpMessageInfo httpMessageInfo) {
        if (matcher.matches(httpMessageInfo.getOriginalUrl())) {
            httpMessageContents.setTextContents(formatter.apply(httpMessageContents.getTextContents()));
        }
    }
}
