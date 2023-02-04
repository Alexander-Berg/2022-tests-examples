package ru.yandex.realty.unification.cluster.core;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.realty.unification.cluster.core.ClusterUtils.hasTheSameIntervals;

/**
 * @author Anton Irinev (airinev@yandex-team.ru)
 */
public class ClusterUtilsTest {

    @Test
    public void testTheSameIntervalNearEndPoints() {
        assertTrue(ClusterUtils.hasTheSameIntervals(3.1f, 12.9f, 3, 10));
    }

    @Test
    public void testNotTheSameIntervalNearEndPoints() {
        assertFalse(ClusterUtils.hasTheSameIntervals(2.9f, 12.9f, 3, 10));
    }

    @Test
    public void testTheSameIntervalBecauseEqualPoints() {
        assertTrue(ClusterUtils.hasTheSameIntervals(5f, 5f, 3, 10));
    }

    @Test
    public void testNotTheSameIntervalButVeryClosePoints() {
        assertFalse(ClusterUtils.hasTheSameIntervals(12.9f, 13.1f, 3, 10));
    }

    /**
     *     <bean id="dataSource" class="ru.yandex.common.util.db.NamedDataSource">
     <property name="moduleName" value="${module.name}"/>
     <property name="nativeJdbcExtractor">
     <bean class="org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor"/>
     </property>
     <property name="driverClassName" value="${realty.storage.jdbc.driverClassName}"/>
     <property name="url" value="${realty.storage.jdbc.url}"/>
     <property name="username" value="${realty.storage.jdbc.username}"/>
     <property name="password" value="${realty.storage.jdbc.password}"/>
     <property name="validationQuery" value="SELECT 1 from dual"/>
     <property name="maxWait" value ="${realty.storage.jdbc.maxWait}"/>
     <property name="maxActive" value="${realty.storage.jdbc.maxActive}"/>
     <property name="maxIdle" value="${realty.storage.jdbc.maxIdle}"/>
     <property name="removeAbandoned" value="${realty.storage.jdbc.removeAbandoned}"/>
     <property name="removeAbandonedTimeout" value="${realty.storage.jdbc.removeAbandonedTimeout}"/>
     </bean>

     <bean id="jdbcTemplate" class="org.springframework.jdbc.core.simple.SimpleJdbcTemplate">
     <constructor-arg index="0" ref="dataSource"/>
     </bean>

     <bean id="transactionTemplate" class="org.springframework.transaction.support.TransactionTemplate" />

     <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager" />

     dbc:oracle:thin:@
     (description =
     (failover=on)
     (address = (protocol = tcp)(host = hosteldb1e.yandex.ru)(port = 1521))
     (address = (protocol = tcp)(host = hosteldb1f.yandex.ru)(port = 1521))
     (connect_data =
     (service_name = hosteldb)))

     realty.storage.jdbc.host=markettestdbh-sas.yandex.ru
     realty.storage.jdbc.username=realty
     realty.storage.jdbc.password=Ahthaip7

     realty.storage.jdbc.url=jdbc:oracle:thin:@${realty.storage.jdbc.host}

     realty.storage.jdbc.maxActive=20
     realty.storage.jdbc.maxIdle=4
     realty.storage.jdbc.removeAbandoned=false
     realty.storage.jdbc.removeAbandonedTimeout=300
     realty.storage.jdbc.maxWait=300

     realty.externals.partnerdata.serverurl=http://common-partner-transport.http.yandex.net:36178

     realty.storage.jdbc.host}:${realty.storage.jdbc.port}:${realty.storage.jdbc.oracleSchema}
     * @throws Exception
     */

//    @Test
//    public void testSHA() throws Exception {
//        NamedDataSource dataSource = new NamedDataSource();
//        dataSource.setModuleName("realty");
//        dataSource.setNativeJdbcExtractor(new CommonsDbcpNativeJdbcExtractor());
//        dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
//        dataSource.setUrl("jdbc:oracle:thin:@hosteldb1f.yandex.ru:1521:hostel");
//        dataSource.setUsername("realty");
//        dataSource.setPassword("Ahthaip7");
//        dataSource.setValidationQuery("SELECT 1 from dual");
//        dataSource.setMaxWait(300);
//        dataSource.setMaxActive(20);
//        dataSource.setMaxIdle(4);
//        dataSource.setRemoveAbandoned(false);
//        dataSource.setRemoveAbandonedTimeout(300);
//        SimpleJdbcTemplate simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
//        PreparedStatementSetter preparedStatementSetter = new PreparedStatementSetter() {
//            public void setValues(final PreparedStatement ps) throws SQLException {
//                ps.setFetchSize(5000);
//            }
//        };
//        try (PrintWriter printWriter = new PrintWriter(new File("/home/rmuzhikov/realty_id_old_to_new.txt"))){
//            final MutableInt counter = new MutableInt(0);
//            simpleJdbcTemplate.getJdbcOperations().query("select partner_id, partner_internal_id, offer_id from offer_to_id_mapping",
//                    preparedStatementSetter,
//                    new RowCallbackHandler(){
//                        @Override
//                        public void processRow(ResultSet rs) throws SQLException {
//                            counter.increment();
//                            if (counter.intValue() % 5000 == 0 ) {
//                                System.out.println("Readed " + counter.intValue());
//                            }
//                            long partnerId = rs.getLong("partner_id");
//                            String internalId = rs.getString("partner_internal_id");
//                            long oldId = rs.getLong("offer_id");
//                            long newId = getId(partnerId, internalId);
//                            printWriter.println(oldId + " " + newId);
//                        }
//                    });
//        }
//    }
//
//
//    public static long getId(
//            final long partnerId,
//            final String partnerInternalId) {
//        final CharSequence charSequence = partnerId + ":" + partnerInternalId;
//        long id = 0;
//        for (int i = 0; i < charSequence.length(); i++) {
//            id = 31 * id + charSequence.charAt(i);
//        }
//        return Math.abs(id);
//    }
//
//
//    public Query buildQuery(String... ids) {
//        BooleanQuery booleanQuery = new BooleanQuery();
//        for (String i : ids) {
//            booleanQuery.add( new TermQuery(new Term("ID", i)), BooleanClause.Occur.SHOULD);
//        }
//        return booleanQuery;
//    }
//
//    @Test
//    public void testPartition() throws Exception {
////        Query q = buildQuery("40963252352425801"); //457
//        Query q = buildQuery("1268536055799878472");    //3436
////        Query q = new TermQuery(new Term("ID", "40963252352425801"));
////        Query q = new TermQuery(new Term("ID", "1268536055799878472"));
//
//        for (File partition : new File("/home/rmuzhikov/v3").listFiles()) {
//            for (File timestamp : partition.listFiles()) {
//                System.out.println("Checking partition " + timestamp);
//                MMapDirectory dir = new MMapDirectory(new File(timestamp, "index"));
//                IndexReader reader = IndexReader.open(dir);
//                IndexSearcher searcher = new IndexSearcher(reader);
//                TopDocs td = searcher.search(q, 100);
//                if (td.totalHits > 0) {
//                    System.out.println("Found in partition " + timestamp);
//                    return;
//                }
//                searcher.close();
//                reader.close();
//                dir.close();
//            }
//        }
//
//
//    }
}
