package com.yandex.mobile.job.guaranty;

import android.database.sqlite.SQLiteDatabase;

import com.octo.android.robospice.persistence.exception.SpiceException;
import com.yandex.mobile.job.BaseTest;
import com.yandex.mobile.job.JMockitRobolectricTestRunner;
import com.yandex.mobile.job.network.request.EventRequest;
import com.yandex.mobile.job.provider.RawSQLiteDBHolder;
import com.yandex.mobile.job.service.FilterCountService;
import com.yandex.mobile.job.service.GuarantySendIntentService;
import com.yandex.mobile.job.service.GuarantySendIntentService_;
import com.yandex.mobile.job.service.handler.InternalEventHandler;
import com.yandex.mobile.job.utils.Analytic;
import com.yandex.mobile.job.utils.AppHelper;
import com.yandex.mobile.job.utils.RxHelper;
import com.yandex.mobile.job.utils.SQLHelper;

import org.hamcrest.Matcher;
import org.hamcrest.core.AllOf;
import org.hamcrest.core.Every;
import org.hamcrest.core.IsEqual;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import mockit.Delegate;
import mockit.Expectations;
import mockit.FullVerifications;
import mockit.FullVerificationsInOrder;
import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Tested;
import mockit.Verifications;
import mockit.VerificationsInOrder;
import nl.qbusict.cupboard.CupboardFactory;
import nl.qbusict.cupboard.DatabaseCompartment;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

import static mockit.Deencapsulation.invoke;
import static mockit.Deencapsulation.setField;

/**
 *
 * @author ironbcc on 28.04.2015.
 */
@RunWith(JMockitRobolectricTestRunner.class)
public class GuarantySendServiceTest extends BaseTest {
    @Tested @Mocked InternalEventHandler eventHandler;
    @Tested @Mocked("scheduleSending") GuarantySendIntentService service;
    private SQLiteDatabase database;


    @Before
    public void before() {
        database = AppHelper.bean(RawSQLiteDBHolder.class).getReadableDatabase();
        new NonStrictExpectations(eventHandler) {{
            invoke(eventHandler, "getHandledClass");
            result = EventRequest.class;
            invoke(eventHandler, "getUniqueIdentifier");
            result = 0;
        }};
        new NonStrictExpectations(service) {{
            invoke(service, "scheduleSending");
            result = new Delegate<Void>() {
                public void delegate() {
                    service.sendObjects();
                }
            };
        }};
        GuarantySendIntentService.register(eventHandler);
    }

    @Test
    @Ignore
    public void singEventTest() {
        Assert.assertEquals(0,
                SQLHelper.selectCount(GuarantySendIntentService.InnerSendWrapper.class, database, null, null));
        service.send(new EventRequest("1", Analytic.Type.CALL));
        Assert.assertEquals(1,
                SQLHelper.selectCount(GuarantySendIntentService.InnerSendWrapper.class, database, null, null));
        new VerificationsInOrder() {{
            service.sendObjects();maxTimes = 1;
            eventHandler.getUniqueIdentifier();maxTimes = 1;
            eventHandler.handleSend(withEqual(Arrays.asList(new EventRequest("1", Analytic.Type.CALL))));maxTimes = 1;
        }};

    }
}
