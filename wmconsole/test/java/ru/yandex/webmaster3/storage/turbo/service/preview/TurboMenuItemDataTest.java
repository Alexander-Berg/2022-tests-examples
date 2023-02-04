package ru.yandex.webmaster3.storage.turbo.service.preview;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.webmaster3.core.turbo.model.menu.TurboMenuItem;

/**
 * Created by Oleg Bazdyrev on 19/02/2020.
 */
public class TurboMenuItemDataTest {

    @Test
    public void testConversion() throws Exception {
        List<TurboMenuItem> topMenu = new ArrayList<>();
        topMenu.add(createItem("top 1", "http://site.com/top1"));
        topMenu.add(createItem("top 2", "http://site.com/top2"));
        topMenu.add(createItem("top 3", "http://site.com/top3"));

        List<TurboMenuItem> menu = new ArrayList<>();
        TurboMenuItem menu1 = createItem("label 1", "http://site.com/menu1");
        menu.add(menu1);
        TurboMenuItem menu1_1 = createItem("label 1.1", "http://site.com/menu1_1");
        TurboMenuItem menu1_2 = createItem("label 1.2", "http://site.com/menu1_2");
        TurboMenuItem menu1_3 = createItem("label 1.3", "http://site.com/menu1_3");
        menu1.getSubmenu().add(menu1_1);
        menu1.getSubmenu().add(menu1_2);
        menu1.getSubmenu().add(menu1_3);
        TurboMenuItem menu1_2_1 = createItem("label 1.2.1", "http://site.com/menu1_2_1");
        TurboMenuItem menu1_2_2 = createItem("label 1.2.2", "http://site.com/menu1_2_2");
        menu1_2.getSubmenu().add(menu1_2_1);
        menu1_2.getSubmenu().add(menu1_2_2);
        TurboMenuItem menu2 = createItem("label 2", "http://site.com/menu2");
        menu.add(menu2);
        TurboMenuItem menu3 = createItem("label 3", "http://site.com/menu3");
        menu.add(menu3);
        TurboMenuItem menu3_1 = createItem("label 3.1", "http://site.com/menu3_1");
        TurboMenuItem menu3_2 = createItem("label 3.2", "http://site.com/menu3_2");
        menu3.getSubmenu().add(menu3_1);
        menu3.getSubmenu().add(menu3_2);
        TurboMenuItem menu4 = createItem("label 4", "http://site.com/menu4");
        menu.add(menu4);

        List<TurboMenuItemData> res = new ArrayList<>();
        res.addAll(TurboMenuItemData.fromMenu(topMenu, false));
        res.addAll(TurboMenuItemData.fromMenu(menu, false));
        // yml saves only first depth level
        Assert.assertEquals(7, res.size());
        checkItem(res.get(0), topMenu.get(0).getLabel(), null);
        checkItem(res.get(1), topMenu.get(1).getLabel(), null);
        checkItem(res.get(2), topMenu.get(2).getLabel(), null);
        checkItem(res.get(3), menu1.getLabel(), null);
        checkItem(res.get(4), menu2.getLabel(), null);
        checkItem(res.get(5), menu3.getLabel(), null);
        checkItem(res.get(6), menu4.getLabel(), null);

        res = new ArrayList<>();
        res.addAll(TurboMenuItemData.fromMenu(topMenu, false));
        res.addAll(TurboMenuItemData.fromMenu(menu, true));
        // non-yml saves all elements
        Assert.assertEquals(14, res.size());
        checkItem(res.get(0), topMenu.get(0).getLabel(), null);
        checkItem(res.get(1), topMenu.get(1).getLabel(), null);
        checkItem(res.get(2), topMenu.get(2).getLabel(), null);
        checkItem(res.get(3), menu1.getLabel(), 1L);
        checkItem(res.get(4), menu1_1.getLabel(), 2L);
        checkItem(res.get(5), menu1_2.getLabel(), 3L);
        checkItem(res.get(6), menu1_2_1.getLabel(), 4L);
        checkItem(res.get(7), menu1_2_2.getLabel(), 5L);
        checkItem(res.get(8), menu1_3.getLabel(), 6L);
        checkItem(res.get(9), menu2.getLabel(), 7L);
        checkItem(res.get(10), menu3.getLabel(), 8L);
        checkItem(res.get(11), menu3_1.getLabel(), 9L);
        checkItem(res.get(12), menu3_2.getLabel(), 10L);
        checkItem(res.get(13), menu4.getLabel(), 11L);
    }

    private static void checkItem(TurboMenuItemData item, String expectedLabel, Long expectedId) {
        Assert.assertEquals(expectedLabel, item.getLabel());
        Assert.assertEquals(expectedId, item.getId());
    }

    private static TurboMenuItem createItem(String label, String url) {
        return new TurboMenuItem(null, label, url, null, new ArrayList<>());
    }
}
