package ru.yandex.webmaster3.storage.turbo.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.webmaster3.storage.util.ydb.ThreadLocalYdbTransactionManager;

import static org.mockito.Mockito.verify;


/**
 * ishalaru
 * 16.06.2020
 **/
@RunWith(MockitoJUnitRunner.class)
public class TurboCmntStatisticsSimpleYdbDao2Test {
    private @Mock
    ThreadLocalYdbTransactionManager transactionManager;
    private @InjectMocks
    TurboCmntStatisticsYDao turboCmntStatisticsYdbDao2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test() {
/*        ArgumentCaptor<Session> session = ArgumentCaptor.forClass(Session.class);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        //when(transactionManager.prepareQuery(session.capture(), sql.capture())).thenReturn(null);
        final List<TurboCmntStatisticsYdbDao.TurboCmntStatistics> select = turboCmntStatisticsYdbDao2.select("test", LocalDate.now(), LocalDate.now(), "test", false, 100);
        verify(transactionManager,times(1)).prepareQuery(session.capture(),sql.capture());
        System.out.println(sql.getValue())*/;
    }
}