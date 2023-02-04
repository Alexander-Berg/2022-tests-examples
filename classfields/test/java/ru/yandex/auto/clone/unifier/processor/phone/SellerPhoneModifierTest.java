package ru.yandex.auto.clone.unifier.processor.phone;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.common.util.collections.CollectionFactory.newList;

import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import ru.yandex.auto.clone.unifier.UnifierException;
import ru.yandex.auto.clone.unifier.modifier.FieldFilter;
import ru.yandex.auto.clone.unifier.modifier.ValidModifier;
import ru.yandex.auto.clone.unifier.modifier.valid.*;
import ru.yandex.auto.core.model.UnifiedCarInfo;
import ru.yandex.auto.core.model.enums.Valid;
import ru.yandex.auto.core.phone.BannedPhonesListHolder;
import ru.yandex.auto.core.phone.BannedPhonesListProvider;
import ru.yandex.auto.core.phone.PhoneNumber;

/** @author Anton Irinev (airinev@yandex-team.ru) */
public class SellerPhoneModifierTest {
  @NotNull private final SellerPhoneModifier modifier;
  @NotNull private final ValidModifier validModifier;

  public SellerPhoneModifierTest() throws Exception {
    modifier = new SellerPhoneModifier();
    modifier.afterPropertiesSet();

    validModifier = new ValidModifier();
    List<FieldFilter<Valid>> filters = new LinkedList<FieldFilter<Valid>>();
    filters.add(new MarkFilter());
    filters.add(new ModelFilter());
    filters.add(new YearFilter());
    filters.add(new SellerCityFilter());
    filters.add(new PhonesFilter());
    filters.add(new CreationDateFilter());
    filters.add(new FutureDateFilter());
    filters.add(new PriceRunStateDisplacementFilter());
    filters.add(new WrongYearFilter());
    filters.add(new WrongStateFilter());
    filters.add(new BuyFilter());
    PhonesBanFilter phonesBanFilter = new PhonesBanFilter();

    BannedPhonesListProvider bannedPhonesListProvider = mock(BannedPhonesListProvider.class);

    when(bannedPhonesListProvider.getData())
        .thenReturn(new BannedPhonesListHolder(new HashSet<String>(Arrays.asList("(812)7778899"))));
    phonesBanFilter.setBannedPhonesListProvider(bannedPhonesListProvider);

    filters.add(phonesBanFilter);

    OldFilter oldFilter = new OldFilter();
    filters.add(oldFilter);

    validModifier.setFilters(filters);
  }

  @Test
  public void testShouldNotBeCorrected() throws UnifierException {
    UnifiedCarInfo carInfo =
        constructCarInfo("8(812)111-11-11\n+7(921)222-22-22\n+7 921 333-33-33", "2");

    modifier.modify(carInfo);

    Set<PhoneNumber> correctedPhones = carInfo.getCorrectedPhone();
    assertEquals("[(812)1111111, (921)2222222, (921)3333333]", sort(correctedPhones).toString());
  }

  @Test
  public void testAmbiguousCorrection() throws UnifierException {
    UnifiedCarInfo carInfo = constructCarInfo("111-11-11", "1");

    modifier.modify(carInfo);

    Set<PhoneNumber> correctedPhones = carInfo.getCorrectedPhone();
    assertEquals("[(?)1111111]", sort(correctedPhones).toString());
  }

  @Test
  public void testLeadingZeroAbsence() throws UnifierException {
    UnifiedCarInfo carInfo = constructCarInfo("(44)111-11-11", "143");

    modifier.modify(carInfo);

    Set<PhoneNumber> correctedPhones = carInfo.getCorrectedPhone();
    assertEquals("[(044)1111111]", sort(correctedPhones).toString());
  }

  @Test
  public void testLeadingZeroPresence() throws UnifierException {
    UnifiedCarInfo carInfo = constructCarInfo("(044)111-11-11", "143");

    modifier.modify(carInfo);

    Set<PhoneNumber> correctedPhones = carInfo.getCorrectedPhone();
    assertEquals("[(044)1111111]", sort(correctedPhones).toString());
  }

  @Test
  public void testNotBanValidCar() throws UnifierException {
    UnifiedCarInfo carInfo = constructCarInfo("(812)1234567", "2");
    modifier.modify(carInfo);
    validModifier.modify(carInfo);
    assertEquals(1, (long) carInfo.getValid());
  }

  @Test
  public void testNotBanTheShortestPossibleNumber() throws UnifierException {
    UnifiedCarInfo carInfo = constructCarInfo("12345", "2");
    modifier.modify(carInfo);
    validModifier.modify(carInfo);
    assertEquals(1, (long) carInfo.getValid());
  }

  @Test
  public void testBanEmptyPhone() throws UnifierException {
    UnifiedCarInfo carInfo = constructCarInfo("", "213");
    modifier.modify(carInfo);
    validModifier.modify(carInfo);
    assertEquals(-10, (long) carInfo.getValid());
  }

  @Test
  public void testBanTooShortNumber() throws UnifierException {
    UnifiedCarInfo carInfo = constructCarInfo("1234", "1");
    modifier.modify(carInfo);
    validModifier.modify(carInfo);
    assertEquals(-10, (long) carInfo.getValid());
  }

  @Test
  public void testStopPhone() throws UnifierException {
    UnifiedCarInfo carInfo = constructCarInfo("(812)7778899", "2");
    modifier.modify(carInfo);
    validModifier.modify(carInfo);
    assertEquals(-7, (long) carInfo.getValid());
  }

  // utility stuff

  @NotNull
  private UnifiedCarInfo constructCarInfo(String sellerPhone, String city) {
    UnifiedCarInfo carInfo = new UnifiedCarInfo("777");
    carInfo.setSellerPhone(sellerPhone);
    carInfo.setSellerCity(city);
    carInfo.setValid(1);
    carInfo.setState("NEW");
    carInfo.setMark("FORD");
    carInfo.setModel("FOCUS");
    carInfo.setYear(Calendar.getInstance().get(Calendar.YEAR) - 1);
    carInfo.setCreationDate(System.currentTimeMillis());
    carInfo.setPrice(1000000);
    carInfo.setDisplacement(1800);
    carInfo.setResourceId(123123L);
    return carInfo;
  }

  @NotNull
  private static List<PhoneNumber> sort(@NotNull Set<PhoneNumber> phonesSet) {
    List<PhoneNumber> phones = newList(phonesSet);
    Collections.sort(
        phones,
        new Comparator<PhoneNumber>() {
          public int compare(@NotNull PhoneNumber a, @NotNull PhoneNumber b) {
            return a.toString().compareTo(b.toString());
          }
        });

    return phones;
  }
}
