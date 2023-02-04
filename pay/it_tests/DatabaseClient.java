package ru.yandex.darkspirit.it_tests;

import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@AllArgsConstructor
public class DatabaseClient {
    private final String url;
    static {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to found oracle driver", e);
        }
    }

    @SneakyThrows
    public <T> T getField(String serialNumber, String fieldName, Class<T> type) {
        val query = String.format("select %s from ds.T_CASH_REGISTER where serial_number = ?", fieldName);
        try (val conn = DriverManager.getConnection(url);
             val ps = conn.prepareStatement(query)
        ) {
            ps.setString(1, serialNumber);
            try (val rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException(String.format("Failed to find cash_register with serial number %s", serialNumber));
                }
                return rs.getObject(1, type);
            }
        }
    }

    @SneakyThrows
    public String getDsState(String serialNumber) {
        return getField(serialNumber, "ds_state", String.class);
    }

    @SneakyThrows
    public int getId(String serialNumber) {
        return this.getField(serialNumber, "id", Integer.class);
    }

    @SneakyThrows
    public void setLastRegistrationState(int cashRegisterId, String targetState) {
        try (val conn = DriverManager.getConnection(url);
             val ps = conn.prepareStatement(
                     "UPDATE ds.T_REGISTRATION set state=? " +
                             "where id =(SELECT id from " +
                             "(SELECT * from ds.t_registration WHERE cash_register_id = ? " +
                             "ORDER BY ds.t_registration.create_dt DESC, ds.t_registration.id DESC) where ROWNUM=1)\n"
             )
        ) {
            ps.setString(1, targetState);
            ps.setInt(2, cashRegisterId);
            ps.executeQuery();
        }
    }

    @SneakyThrows
    public void setLastRegistrationDocument(int cashRegisterId, int documentId) {
        try (val conn = DriverManager.getConnection(url);
             val ps = conn.prepareStatement(
                     "UPDATE ds.T_REGISTRATION set document_id=? " +
                             "where id =(SELECT id from " +
                             "(SELECT * from ds.t_registration WHERE cash_register_id = ? " +
                             "ORDER BY ds.t_registration.create_dt DESC, ds.t_registration.id DESC) where ROWNUM=1)\n"
             )
        ) {
            ps.setInt(1, documentId);
            ps.setInt(2, cashRegisterId);
            ps.executeQuery();
        }
    }

    @SneakyThrows
    public void setLastRegistrationDocumentDt(int cashRegisterId, String documentDt) {
        try (val conn = DriverManager.getConnection(url);
             val ps = conn.prepareStatement(
                     "UPDATE ds.T_REGISTRATION set start_dt=TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') " +
                             "where id =(SELECT id from " +
                             "(SELECT * from ds.t_registration WHERE cash_register_id = ? " +
                             "ORDER BY ds.t_registration.create_dt DESC, ds.t_registration.id DESC) where ROWNUM=1)\n"
             )
        ) {
            ps.setString(1, documentDt);
            ps.setInt(2, cashRegisterId);
            ps.executeQuery();
        }
    }

    @SneakyThrows
    public String addDocument(String documentType, int fiscalStorageNumber, int fiscalStorageId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        String dt = LocalDateTime.now().format(formatter);
        try (val conn = DriverManager.getConnection(url);
             val ps = conn.prepareStatement(
                     "INSERT INTO ds.t_DOCUMENT (ID, DOCUMENT_TYPE, FISCAL_STORAGE_NUMBER, FISCAL_STORAGE_SIGN," +
                             " FISCAL_STORAGE_ID, ARCHIVE, DT) VALUES (s_document_id.nextval," +
                             " ?, ?, 228, ?, 0, TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS'))"
             )
        ) {
            ps.setString(1, documentType);
            ps.setInt(2, fiscalStorageNumber);
            ps.setInt(3, fiscalStorageId);
            ps.setString(4, dt);
            ps.executeQuery();
        }
        return dt;
    }

    @SneakyThrows
    public int getDocumentId(int fiscalStorageNumber, int fiscalStorageId) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        String dt = LocalDateTime.now().format(formatter);
        try (val conn = DriverManager.getConnection(url);
             val ps = conn.prepareStatement(
                     "SELECT id from ds.t_DOCUMENT where FISCAL_STORAGE_NUMBER=? AND FISCAL_STORAGE_ID=?"
             )
        ) {
            ps.setInt(1, fiscalStorageNumber);
            ps.setInt(2, fiscalStorageId);
            try (val rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException(String.format("Failed to find document with fiscal storage id %s", fiscalStorageId));
                }
                return rs.getObject(1, Integer.class);
            }
        }
    }

    @SneakyThrows
    public String getProcessData(int cashRegisterId) {
        try (val conn = DriverManager.getConnection(url);
             val ps = conn.prepareStatement("select data from ds.T_CASH_REGISTER_PROCESS  where cash_register_id = ?")
        ) {
            ps.setInt(1, cashRegisterId);
            try (val rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new RuntimeException(String.format("Failed to find process with cash_register with id %s", cashRegisterId));
                }
                return rs.getObject(1, String.class);
            }
        }
    }

    @SneakyThrows
    public void setProcessData(String data, int cashRegisterId) {
        try (val conn = DriverManager.getConnection(url);
             val ps = conn.prepareStatement("update ds.T_CASH_REGISTER_PROCESS set data = ?  where cash_register_id = ?")
        ) {
            ps.setString(1, data);
            ps.setInt(2, cashRegisterId);
            ps.executeQuery();
        }
    }

    @SneakyThrows
    public void setOfd(String ofdInn, String newHost, Integer newPort) {
        try (val conn = DriverManager.getConnection(url);
             val ps1 = conn.prepareStatement("update ds.T_OFD set test_host = ? where inn = ?");
             val ps2 = conn.prepareStatement("update ds.T_OFD set test_port = ? where inn = ?")
        ) {
            ps1.setString(1, newHost);
            ps1.setString(2, ofdInn);
            ps1.executeQuery();
            ps2.setInt(1, newPort);
            ps2.setString(2, ofdInn);
            ps2.executeQuery();
        }
    }

    @SneakyThrows
    public void setOEBSAddressCodeInTCashRegister(String serialNumber, String addressCode) {
        try (val conn = DriverManager.getConnection(url);
             val ps = conn.prepareStatement("update ds.T_CASH_REGISTER set oebs_address_code = ? where serial_number = ?")
        ) {
            ps.setString(1, addressCode);
            ps.setString(2, serialNumber);
            ps.executeQuery();
        }
    }

    @SneakyThrows
    public void setSigner(String inn, String firstname, String lastname, String middlename) {
        try (val conn = DriverManager.getConnection(url);
             val ps = conn.prepareStatement(
                     "INSERT INTO DS.T_SIGNER (ID, FIRST_NAME, MIDDLE_NAME, LAST_NAME, MAIN, HIDDEN, FIRM_INN," +
                             " CERTIFICATE_EXPIRATION_DATE) VALUES (ds.s_signer_id.nextval, ?, ?, ?, 1, 0," +
                             " ?, TO_DATE('2023-08-12', 'YYYY-MM-DD HH24:MI:SS'))")
        ) {
            ps.setString(1, firstname);
            ps.setString(2, middlename);
            ps.setString(3, lastname);
            ps.setString(4, inn);
            ps.executeQuery();
        }
    }

    @SneakyThrows
    private void setVOEBSData(
            Integer id, String instanceNumber, String serialNumber, String OEBSCode, String firstAddressPart,
            String secondAddressPart
    ) {
        try (val conn = DriverManager.getConnection(url);
             val ps = conn.prepareStatement(
                     "INSERT INTO DS.V_OEBS_DATA (INSTANCE_ID, INSTANCE_NUMBER, SERIAL_NUMBER," +
                             " ITEM_CONCATENATED_SEGMENTS, SEGMENT2, SEGMENT3) " +
                             "VALUES (?, ?, ?," +
                             " ?, ?, ?)")
             ) {
            ps.setInt(1, id);
            ps.setString(2, instanceNumber);
            ps.setString(3, serialNumber);
            ps.setString(4, OEBSCode);
            ps.setString(5, firstAddressPart);
            ps.setString(6, secondAddressPart);
            ps.executeQuery();
        }
    }

    public void setOEBS(
            Integer VOEBSId, String instanceNumber, String serialNumber, String OEBSCode, String firstAddressPart,
            String secondAddressPart
    ) {
        setOEBSAddressCodeInTCashRegister(serialNumber, firstAddressPart + ">" + secondAddressPart);
        setVOEBSData(
                VOEBSId, instanceNumber, serialNumber, OEBSCode, firstAddressPart, secondAddressPart
        );
    }
}
