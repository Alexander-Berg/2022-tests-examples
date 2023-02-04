package ru.yandex.partner.core.service;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcStreamTransport;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.partner.core.entity.balance.model.ActiveContract;
import ru.yandex.partner.core.entity.balance.model.BalanceBank;
import ru.yandex.partner.core.entity.balance.model.BalanceContract;
import ru.yandex.partner.core.entity.balance.model.BalancePerson;
import ru.yandex.partner.core.service.integration.balance.BalanceDocumentService;
import ru.yandex.partner.libs.extservice.balance.BalanceResponseConverter;
import ru.yandex.partner.libs.extservice.balance.BalanceService;
import ru.yandex.partner.libs.extservice.balance.method.partnercontract.BalancePartnerContract;
import ru.yandex.partner.libs.extservice.balance.method.partnercontract.Bank;
import ru.yandex.partner.libs.extservice.balance.method.partnercontract.Collateral;
import ru.yandex.partner.libs.extservice.balance.method.partnercontract.Contract;
import ru.yandex.partner.libs.extservice.balance.method.partnercontract.Person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class BalanceDocumentServiceTest {

    private static final long FAKE_PERSON_ID = 4321L;
    private static final long FAKE_CLIENT_ID = 1234L;
    private static final String FAKE_LOGIN = "FAKE_LOGIN";


    private static final List<BalancePartnerContract> FAKE_PARTNER_CONTRACTS =
            List.of(
                    new BalancePartnerContract(
                            new Person.Builder().build(),
                            new Contract.Builder()
                                    .withIsFaxed(LocalDate.of(2010, 6, 29))
                                    .withIsSigned(LocalDate.of(2010, 6, 29))
                                    .withDt(LocalDate.of(2009, 9, 23))
                                    .withContractType(1)
                                    .withType("WRONG")
                                    .build(),
                            List.of(
                                    new Collateral.Builder()
                                            .withIsFaxed(LocalDate.of(2010, 6, 29))
                                            .withIsSigned(LocalDate.of(2010, 6, 29))
                                            .withDt(LocalDate.of(2010, 5, 31))
                                            .withCollateralTypeId(2070)
                                            .withBalanceClassType("COLLATERAL")
                                            .build(),
                                    new Collateral.Builder()
                                            .withDt(LocalDate.of(2013, 5, 31))
                                            .withCollateralTypeId(2090)
                                            .withEndReason(2)
                                            .withEndDt(LocalDate.of(2013, 5, 31))
                                            .withBalanceClassType("ANNOUNCEMENT")
                                            .build()
                            )
                    ),

                    new BalancePartnerContract(
                            new Person.Builder().build(),
                            new Contract.Builder()
                                    .withDt(LocalDate.of(2008, 7, 22))
                                    .withEndDt(LocalDate.of(2009, 8, 28))
                                    .withContractType(1)
                                    .withType("PARTNERS")
                                    .build(),
                            Collections.emptyList()
                    ),

                    new BalancePartnerContract(
                            new Person.Builder().build(),
                            new Contract.Builder()
                                    .withIsFaxed(LocalDate.of(2013, 4, 30))
                                    .withDt(LocalDate.of(2013, 6, 1))
                                    .withContractType(5)
                                    .withType("PARTNERS")
                                    .build(),
                            List.of(
                                    new Collateral.Builder()
                                            .withPartnerPct(BigDecimal.valueOf(60L))
                                            .withIsSigned(LocalDate.of(2014, 1, 30))
                                            .withDt(LocalDate.of(2014, 1, 1))
                                            .withCollateralTypeId(2020)
                                            .withBalanceClassType("COLLATERAL")
                                            .build(),

                                    new Collateral.Builder()
                                            .withIsSigned(LocalDate.of(2014, 12, 24))
                                            .withDt(LocalDate.of(2014, 10, 23))
                                            .withCollateralTypeId(2040)
                                            .withBalanceClassType("COLLATERAL")
                                            .build(),

                                    new Collateral.Builder()
                                            .withIsSigned(LocalDate.of(2014, 12, 29))
                                            .withDt(LocalDate.of(2014, 12, 25))
                                            .withCollateralTypeId(9999)
                                            .withBalanceClassType("COLLATERAL")
                                            .build(),

                                    new Collateral.Builder()
                                            .withPartnerPct(BigDecimal.valueOf(43L))
                                            .withIsSigned(LocalDate.of(2015, 1, 15))
                                            .withDt(LocalDate.of(2015, 1, 1))
                                            .withCollateralTypeId(2020)
                                            .withBalanceClassType("COLLATERAL")
                                            .build(),

                                    new Collateral.Builder()
                                            .withPartnerPct(BigDecimal.valueOf(43L))
                                            .withEndDt(LocalDate.of(2016, 5, 31))
                                            .withDt(LocalDate.of(2016, 5, 31))
                                            .withCollateralTypeId(2090)
                                            .withEndReason(1)
                                            .withBalanceClassType("ANNOUNCEMENT")
                                            .build()
                            )
                    ),

                    new BalancePartnerContract(
                            new Person.Builder()
                                    .withSignerPersonName("FAKE_SIGNER_PERSON_NAME")
                                    .withPersonAccount("FAKE_PERSON_ACCOUNT")
                                    .withLongname("FAKE_LONGNAME")
                                    .withEmail("FAKE_EMAIL")
                                    .withLegaladdress("FAKE_LEGALADDRESS")
                                    .withAuthorityDocType("FAKE_AUTHORITY_DOC_TYPE")
                                    .withFax("FAKE_FAX")
                                    .withRepresentative("FAKE_REPRESENTATIVE")
                                    .withId(FAKE_PERSON_ID)
                                    .withBenBank("FAKE_BEN_BANK")
                                    .withBenAccount("FAKE_BEN_ACCOUNT")
                                    .withSwift("FAKE_SWIFT")
                                    .withPostaddress("FAKE_POSTADDRESS")
                                    .withAccount("FAKE_ACCOUNT")
                                    .withName("FAKE_NAME")
                                    .withClientId(FAKE_CLIENT_ID)
                                    .withPhone("FAKE_PHONE")
                                    .withKpp("FAKE_KPP")
                                    .withInn("FAKE_INN")
                                    .withBik("FAKE_BIK")
                                    .withYamoneyWallet("FAKE_YAMONEY_WALLET")
                                    .withIban("FAKE_IBAN")
                                    .withOther("FAKE_OTHER")
                                    .withSignerPositionName("FAKE_SIGNER_POSITION_NAME")
                                    .withLogin(FAKE_LOGIN)
                                    .build(),

                            new Contract.Builder()
                                    .withIsSigned(LocalDate.of(2016, 6, 3))
                                    .withDt(LocalDate.of(2016, 6, 1))
                                    .withContractType(6)
                                    .withPayTo(1)
                                    .withRewardType(1)
                                    .withCurrency(643)
                                    .withExternalId("FAKE_EXTERNAL_ID")
                                    .withType("PARTNERS")
                                    .build(),

                            List.of(
                                    new Collateral.Builder()
                                            .withPartnerPct(BigDecimal.valueOf(50L))
                                            .withIsSigned(LocalDate.of(2016, 7, 20))
                                            .withDt(LocalDate.of(2016, 7, 1))
                                            .withCollateralTypeId(2020)
                                            .withBalanceClassType("COLLATERAL")
                                            .build(),
                                    new Collateral.Builder()
                                            .withPartnerPct(BigDecimal.valueOf(50L))
                                            .withIsSigned(LocalDate.of(2017, 5, 12))
                                            .withDt(LocalDate.of(2017, 5, 2))
                                            .withCollateralTypeId(2040)
                                            .withBalanceClassType("COLLATERAL")
                                            .build()
                            )
                    )
            );

    private static final Bank FAKE_BANK = new Bank(true, "bankId", "bankIdType", "name");

    private static final Contract UPDATED_CONTRACT_1 = new Contract.Builder()
            .withIsFaxed(LocalDate.of(2013, 4, 30))
            .withDt(LocalDate.of(2013, 6, 1))
            .withContractType(5)
            .withType("PARTNERS")
            .build();

    private static final Contract UPDATED_CONTRACT_2 = new Contract.Builder()
            .withIsSigned(LocalDate.of(2016, 6, 3))
            .withDt(LocalDate.of(2016, 6, 1))
            .withContractType(6)
            .withPayTo(1)
            .withRewardType(1)
            .withCurrency(643)
            .withExternalId("FAKE_EXTERNAL_ID")
            .withType("PARTNERS")
            .build();

    static {
        UPDATED_CONTRACT_1.setCollateralPartnerPct(BigDecimal.valueOf(43L));
        UPDATED_CONTRACT_1.setCollateralEndDt(LocalDate.of(2016, 5, 31));
        UPDATED_CONTRACT_1.setCollateralAgregatorPct(null);
        UPDATED_CONTRACT_1.setCollateralEndReason(1);

        UPDATED_CONTRACT_2.setCollateralPartnerPct(BigDecimal.valueOf(50L));
        UPDATED_CONTRACT_2.setCollateralAgregatorPct(null);
    }

    private static final List<BalancePartnerContract> EXPECTED_PARTNER_CONTRACTS =
            List.of(
                    new BalancePartnerContract(
                            new Person.Builder()
                                    .withSignerPersonName("FAKE_SIGNER_PERSON_NAME")
                                    .withPersonAccount("FAKE_PERSON_ACCOUNT")
                                    .withLongname("FAKE_LONGNAME")
                                    .withEmail("FAKE_EMAIL")
                                    .withLegaladdress("FAKE_LEGALADDRESS")
                                    .withAuthorityDocType("FAKE_AUTHORITY_DOC_TYPE")
                                    .withFax("FAKE_FAX")
                                    .withRepresentative("FAKE_REPRESENTATIVE")
                                    .withId(FAKE_PERSON_ID)
                                    .withBenBank("FAKE_BEN_BANK")
                                    .withBenAccount("FAKE_BEN_ACCOUNT")
                                    .withSwift("FAKE_SWIFT")
                                    .withPostaddress("FAKE_POSTADDRESS")
                                    .withAccount("FAKE_ACCOUNT")
                                    .withName("FAKE_NAME")
                                    .withClientId(FAKE_CLIENT_ID)
                                    .withLogin(FAKE_LOGIN)
                                    .withPhone("FAKE_PHONE")
                                    .withKpp("FAKE_KPP")
                                    .withInn("FAKE_INN")
                                    .withBik("FAKE_BIK")
                                    .withYamoneyWallet("FAKE_YAMONEY_WALLET")
                                    .withIban("FAKE_IBAN")
                                    .withOther("FAKE_OTHER")
                                    .withSignerPositionName("FAKE_SIGNER_POSITION_NAME")
                                    .build(),
                            UPDATED_CONTRACT_2,
                            List.of(
                                    new Collateral.Builder()
                                            .withPartnerPct(BigDecimal.valueOf(50L))
                                            .withIsSigned(LocalDate.of(2016, 7, 20))
                                            .withDt(LocalDate.of(2016, 7, 1))
                                            .withCollateralTypeId(2020)
                                            .withBalanceClassType("COLLATERAL")
                                            .build(),
                                    new Collateral.Builder()
                                            .withPartnerPct(BigDecimal.valueOf(50L))
                                            .withIsSigned(LocalDate.of(2017, 5, 12))
                                            .withDt(LocalDate.of(2017, 5, 2))
                                            .withCollateralTypeId(2040)
                                            .withBalanceClassType("COLLATERAL")
                                            .build()
                            )
                    ),
                    new BalancePartnerContract(
                            new Person.Builder().build(),
                            UPDATED_CONTRACT_1,
                            List.of(
                                    new Collateral.Builder()
                                            .withPartnerPct(BigDecimal.valueOf(60L))
                                            .withIsSigned(LocalDate.of(2014, 1, 30))
                                            .withDt(LocalDate.of(2014, 1, 1))
                                            .withCollateralTypeId(2020)
                                            .withBalanceClassType("COLLATERAL")
                                            .build(),

                                    new Collateral.Builder()
                                            .withIsSigned(LocalDate.of(2014, 12, 24))
                                            .withDt(LocalDate.of(2014, 10, 23))
                                            .withCollateralTypeId(2040)
                                            .withBalanceClassType("COLLATERAL")
                                            .build(),

                                    new Collateral.Builder()
                                            .withIsSigned(LocalDate.of(2014, 12, 29))
                                            .withDt(LocalDate.of(2014, 12, 25))
                                            .withCollateralTypeId(9999)
                                            .withBalanceClassType("COLLATERAL")
                                            .build(),

                                    new Collateral.Builder()
                                            .withPartnerPct(BigDecimal.valueOf(43L))
                                            .withIsSigned(LocalDate.of(2015, 1, 15))
                                            .withDt(LocalDate.of(2015, 1, 1))
                                            .withCollateralTypeId(2020)
                                            .withBalanceClassType("COLLATERAL")
                                            .build(),

                                    new Collateral.Builder()
                                            .withPartnerPct(BigDecimal.valueOf(43L))
                                            .withEndDt(LocalDate.of(2016, 5, 31))
                                            .withDt(LocalDate.of(2016, 5, 31))
                                            .withCollateralTypeId(2090)
                                            .withEndReason(1)
                                            .withBalanceClassType("ANNOUNCEMENT")
                                            .build()
                            )
                    ),
                    new BalancePartnerContract(
                            new Person.Builder().build(),
                            new Contract.Builder()
                                    .withDt(LocalDate.of(2008, 7, 22))
                                    .withEndDt(LocalDate.of(2009, 8, 28))
                                    .withContractType(1)
                                    .withType("PARTNERS")
                                    .build(),
                            Collections.emptyList()
                    )
            );


    private static final ActiveContract EXPECTED_ACTIVE_CONTRACT =
            new ActiveContract()
                    .withContract(
                            new BalanceContract()
                                    .withDt(LocalDate.of(2016, 6, 1))
                                    .withStatus("Signed 03.06.2016")
                                    .withCurrency(643)
                                    .withExternalId("FAKE_EXTERNAL_ID")
                                    .withIsSigned(LocalDate.of(2016, 6, 3))
                                    .withRewardType(1)
                                    .withContractType(6)
                    )
                    .withPerson(
                            new BalancePerson()
                                    .withId(FAKE_PERSON_ID)
                                    .withClientId(FAKE_CLIENT_ID)
                                    .withLogin(FAKE_LOGIN)
                                    .withSignerPersonName("FAKE_SIGNER_PERSON_NAME")
                                    .withPersonAccount("FAKE_PERSON_ACCOUNT")
                                    .withLongname("FAKE_LONGNAME")
                                    .withEmail("FAKE_EMAIL")
                                    .withBenAccount("FAKE_BEN_ACCOUNT")
                                    .withLegalAddress("FAKE_LEGALADDRESS")
                                    .withAuthorityDocType("FAKE_AUTHORITY_DOC_TYPE")
                                    .withFax("FAKE_FAX")
                                    .withRepresentative("FAKE_REPRESENTATIVE")
                                    .withBenBank("FAKE_BEN_BANK")
                                    .withSwift("FAKE_SWIFT")
                                    .withPostAddress("FAKE_POSTADDRESS")
                                    .withAccount("FAKE_ACCOUNT")
                                    .withName("FAKE_NAME")
                                    .withPhone("FAKE_PHONE")
                                    .withKpp("FAKE_KPP")
                                    .withInn("FAKE_INN")
                                    .withBik("FAKE_BIK")
                                    .withIban("FAKE_IBAN")
                                    .withOther("FAKE_OTHER")
                                    .withSignerPositionName("FAKE_SIGNER_POSITION_NAME")
                                    .withBank(new BalanceBank()
                                            .withActive(true)
                                            .withBankId("bankId")
                                            .withBankIdType("bankIdType")
                                            .withName("name")
                                    )
                    );

    private BalanceDocumentService balanceDocumentService;

    @BeforeEach
    void setUp() {
        BalanceService balanceService = mock(BalanceService.class);
        balanceDocumentService = new BalanceDocumentService(balanceService);

        doReturn(FAKE_PARTNER_CONTRACTS).when(balanceService).getPartnerContracts(FAKE_CLIENT_ID);
        doReturn(FAKE_BANK).when(balanceService).getBankByBik(anyString());
        doReturn(FAKE_BANK).when(balanceService).getBankBySwift(anyString());
    }

    @Test
    void getActiveContract() {
        List<ActiveContract> actualActiveContracts = balanceDocumentService.getActiveContracts(List.of(FAKE_CLIENT_ID));

        assertEquals(1, actualActiveContracts.size());
        assertEquals(EXPECTED_ACTIVE_CONTRACT, actualActiveContracts.get(0));
    }

    @Test
    void getPartnerContracts() {
        List<BalancePartnerContract> actual = balanceDocumentService.getPartnerContracts(FAKE_CLIENT_ID);

        assertEquals(EXPECTED_PARTNER_CONTRACTS, actual);
    }


    @Test
    void getPartnerContracts509629() throws IOException {
        var response = new ClassPathResource("/balance/Balance.GetPartnerContracts.509629.xml").getInputStream();

        XmlRpcClient client = new XmlRpcClient();
        var transport = new XmlRpcStreamTransport(client) {
            @Override
            protected void close() { }

            @Override
            protected boolean isResponseGzipCompressed(XmlRpcStreamRequestConfig pConfig) {
                return false;
            }

            @Override
            protected InputStream getInputStream() {
                return response;
            }
            @Override
            protected void writeRequest(ReqWriter pWriter) { }
        };
        client.setTransportFactory(() -> transport);

        BalanceDocumentService balance = new BalanceDocumentService(
                new BalanceService() {
                    @Override
                    public List<BalancePartnerContract> getPartnerContracts(long clientId) {
                        try {
                            return Arrays.stream(((Object[]) client.execute("", new Object[]{})))
                                    .map(BalanceResponseConverter::convertPartnerContract).toList();
                        } catch (XmlRpcException e) {
                            throw new RuntimeException();
                        }
                    }

                    @Override
                    public List<BalancePartnerContract> getPartnerContracts(String externalId) {
                        return notGoingToBeTested();
                    }

                    @Override
                    public Bank getBankByBik(String bik) {
                        return null;
                    }

                    @Override
                    public Bank getBankBySwift(String swift) {
                        return null;
                    }
                }
        );

        List<ActiveContract> contracts = balance.getActiveContracts(List.of(509629L));
        Assertions.assertEquals(1, contracts.size());

        ActiveContract activeContract = contracts.get(0);

        BalanceContract balanceContract = activeContract.getContract();
        Assertions.assertNotNull(balanceContract);

        Assertions.assertEquals(LocalDate.parse("2019-10-19"), balanceContract.getDt(),
                "active contract must be the last actual contract"
        );

    }

    private <T> T notGoingToBeTested() {
        throw new IllegalStateException();
    }
}
