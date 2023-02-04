package ru.yandex.realty.graph.util;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.extdata.provider.loader.DataLoadException;
import ru.yandex.extdata.provider.loader.DataLoader;
import ru.yandex.realty.context.ExtDataLoaders;
import ru.yandex.realty.graph.core.GeoObjectType;
import ru.yandex.realty.graph.core.Name;
import ru.yandex.realty.storage.verba.VerbaStorage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.InflaterInputStream;

import static org.junit.Assert.assertEquals;
import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.common.util.collections.CollectionFactory.set;

/**
 * @author berkut
 */
public class NameSplitterTest {

    @Test
    public void testSimple() throws Exception {
        Cut firstCut = new Cut("станция метро", list("ст.м", "ст.м."), set(GeoObjectType.METRO_STATION));
        Cut secondCut = new Cut("площадь", list("пл", "пл."), Collections.<GeoObjectType>emptySet());
        NameSplitter nameSplitter = new NameSplitter(Arrays.asList(firstCut, secondCut));
        Name name = nameSplitter.unify("ст.м пл Ленина", null);
        assertEquals("станция метро площадь ленина", name.getName());
//        assertEquals("", Name.getBase(name));
        name = nameSplitter.unify("ст.м. пл Ленина", GeoObjectType.METRO_STATION);
        assertEquals("площадь ленина", name.getName());
        assertEquals("станция метро", Name.getBase(name));
    }

    @Test
    public void testComplexNames() throws Exception {
        VerbaStorage verba = ExtDataLoaders.createVerbaStorage(new DataLoader() {
            @Override
            public InputStream load() throws DataLoadException {
                try {
                    return new InflaterInputStream(new FileInputStream(new File("../realty-common/test-data/verba2-3")));
                } catch (FileNotFoundException e) {
                    throw new DataLoadException(e);
                }
            }
        });

        NameSplitter nameSplitter = NameSplitterBuilder.build(verba);
        String metroStringName = nameSplitter.getName("Крестовский остров ст м");
        Assert.assertEquals("крестовский", metroStringName);
        Name metroName = nameSplitter.unify("Крестовский остров ст м", GeoObjectType.METRO_STATION);
        Assert.assertEquals("крестовский остров", metroName.getName());
        Assert.assertEquals("метро", Name.getBase(metroName));
//        Assert.assertEquals(metroName.getName(), "крестовский остров");

        String regionStringName = nameSplitter.getName("Еврейская Автономная область");
        Assert.assertEquals("еврейская", regionStringName);
        Name regionName = nameSplitter.unify("Еврейская Автономная область", GeoObjectType.SUBJECT_FEDERATION);
        Assert.assertEquals("еврейская", regionName.getName());
        Assert.assertEquals("автономная область", Name.getBase(regionName));
//        Assert.assertEquals(regionName.getName(), "еврейская");

        String okrugStringName = nameSplitter.getName("центральный адм ок");
        Assert.assertEquals("центральный", okrugStringName);
        Name okrugName = nameSplitter.unify("центральный адм ок", GeoObjectType.CITY_DISTRICT);
        Assert.assertEquals("центральный", okrugName.getName());
        Assert.assertEquals("административный округ", Name.getBase(okrugName));
//        Assert.assertEquals(okrugName.getFullName(), "центральный");


    }
}
