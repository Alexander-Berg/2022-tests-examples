package ru.yandex.navi;

import ru.yandex.navi.tf.SoundDecoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class Sounds implements SoundDecoder {
    private final Map<String, String> sounds = new HashMap<>();
    private final Map<String, String> annotations = new HashMap<>();

    private class Scheme {
        Scheme sound(String key, String hash) {
            sounds.put(hash, key);
            return this;
        }
    }

    public Sounds() {
        annotations.put("CrossRoadCamera", "впереди камера контроля перекрестка");
        annotations.put("Danger", "впереди опасный участок");
        annotations.put("Exit", "съезд");
        annotations.put("ForbiddenStopCamera", "впереди камера на остановку");
        annotations.put("Kilometer", "километр");
        annotations.put("Kilometers", "километров");
        annotations.put("Kilometers2_4", "километра");
        annotations.put("LandmarkAfterBridge", "после моста");
        annotations.put("LandmarkAfterTunnel", "после тоннеля");
        annotations.put("LandmarkAtTrafficLights", "на светофоре");
        annotations.put("LandmarkBeforeBridge", "перед мостом");
        annotations.put("LandmarkBeforeTrafficLights", "перед светофором");
        annotations.put("LandmarkBeforeTunnel", "перед тоннелем");
        annotations.put("LandmarkIntoCourtyard", "во двор");
        annotations.put("LandmarkIntoTunnel", "в тоннель");
        annotations.put("LandmarkToBridge", "на мост");
        annotations.put("LandmarkToFrontageRoad", "на дублер");
        annotations.put("LaneCamera", "впереди камера на полосу");
        annotations.put("Meters", "метров");
        annotations.put("MobileCamera", "впереди может быть мобильная засада");
        annotations.put("Over", "через");
        annotations.put("ParkingRouteCalculated", "начинаем поиск парковки");
        annotations.put("RoadMarkingCamera", "впереди камера на разметку");
        annotations.put("RouteCalculated", "маршрут построен");
        annotations.put("RouteFinished", "вы приехали");
        annotations.put("RouteLost", "вы ушли с маршрута");
        annotations.put("RouteRecalculated", "маршрут перестроен");
        annotations.put("RouteViaPoint", "вы приехали в промежуточную точку");
        annotations.put("RouteWillFinish", "до конца маршрута осталось");
        annotations.put("SchoolAhead", "внимание рядом школа");
        annotations.put("SpeedCamera", "впереди камера");
        annotations.put("TakeRight", "держитесь правее");
        annotations.put("Then", "а затем");
        annotations.put("TurnLeft", "поверните налево");
        annotations.put("TurnRight", "поверните направо");

        scheme("base")
            .sound("MercedesStatusPanelSound", "a28ddff83808f1c19aa0d478047db83d")
            .sound("RouteStarted", "f7b43690e0530871d4b53bf7f3e6df02");
        scheme("common")
            .sound("BgGuidanceOff", "fff91a4287ab9d1ca34e00bd19f78918")
            .sound("Command", "84327e75554a59d54db3c7da58b54bc6")
            .sound("Danger", "3513d60c7f443e9d2065631d1d2fa337")
            .sound("Error", "94d1bbf771cb107e4e3bf5bf724d3b4d")
            .sound("No", "e5d0cf2552825fe3f39ed938f0eb2833")
            .sound("Off", "613f0b810c5972660e9068c24c02eaad")
            .sound("On", "3fece6546d33c9c56f6bc8aad875896b")
            .sound("Yes", "77f08a0f389859ef8b266a4b1a11632d");
        scheme("ru_female")
            .sound("1", "2939cdd9e82b33d2302a3a7698fc74e1")
            .sound("10", "536672e11e96c76d27528a179925572e")
            .sound("100", "735ea2cb9bf9f57c9fb660852a24a2da")
            .sound("10th", "d1b8a9b1bf128d37e50209adcab73241")
            .sound("11", "bf444bf72f1a0f7b060541a4f7e4f240")
            .sound("11th", "8ad8788c05621155126a89a45a0abb7f")
            .sound("12", "18769d5b392197d7788d136aa99f2091")
            .sound("12th", "7c30451003f9ed62ae46079a140d9ffa")
            .sound("13", "26c053f8638e33f40631f9c3afc3c6c0")
            .sound("14", "1d0abf0d6773e8cde33870c68759bc40")
            .sound("15", "a2326361eb1e4391948bdca623b55e26")
            .sound("16", "7e8e85c2dab0004b71a2326374e41216")
            .sound("17", "9b1fe8a9874ec021343325fd435fae49")
            .sound("18", "f794646dc4ef957dcd249fd1df2f0444")
            .sound("19", "4d7af8b46bbae5d308fa3ff099cdb6f9")
            .sound("1st", "135b5e8ec7b854a26bc23eb6491d7691")
            .sound("2", "b4d0764b870c66aab9e759aff412da96")
            .sound("20", "e5eea859e8e1df63ba4e475a48feae8a")
            .sound("200", "cd09897d73e9553a212ad0ce0e2dcd38")
            .sound("2nd", "9b5f4e4c9f813a855a48fe4a4ef57acf")
            .sound("3", "2e2d4fae4cddeeecd9b5a82cad4fb8aa")
            .sound("30", "54bebd7c223394c6593aa9fd69ed3d64")
            .sound("300", "78f6e286c4e70adfbb3292d6171b7cd4")
            .sound("3rd", "76ad7a9efebe2c0522ad4ed6c547e759")
            .sound("4", "bd0b1ba53e3a5b77f3c08ea74891d495")
            .sound("40", "d1aa157c2f274983b6a0013004ec0cc2")
            .sound("400", "c108e582466d4f759b462a4f3b893599")
            .sound("4th", "c7131e77b318132d0617bde87a148446")
            .sound("5", "bf8a74b2f8a829a4dfddfe3151203c68")
            .sound("50", "6071ad7a14fb1fa2d6a92351fbd28dec")
            .sound("500", "9569dcdb243dab9f55891bd598eb3c43")
            .sound("5th", "20e909c58e5af9810d4c668b1315c292")
            .sound("6", "65e2334e391e04b6ff1ad305a079b8c5")
            .sound("60", "6132c09a9576f3e12c8fc47e518f4fc1")
            .sound("600", "64140f3f3d032f7aef0bb688ba36f44c")
            .sound("6th", "d93696b6c054b6a5cc8f1d13242f6a8f")
            .sound("7", "8630fdf839bb2f72821fc18459ce2166")
            .sound("70", "a435fbf64a7174fb1118471c5f2320d7")
            .sound("700", "f92a83644bc5aaa380b51c17bff00743")
            .sound("7th", "a9664393995b5c30f624ff37db41f728")
            .sound("8", "2f3bf1d84dc5ba2e8d33ed2d0f9f1ed1")
            .sound("80", "1f20128dd3f2899f896fab7417c8fac8")
            .sound("800", "4f558cbe9bac311dd7eb1c7395ff0a8e")
            .sound("8th", "e254995d545d216d853cf0bb3be09f8e")
            .sound("9", "f77c153cea4b1e33bf1831ccdb869bdd")
            .sound("90", "a8a86b90a9fc1069109047bac53f2494")
            .sound("900", "4f550b1a171abd08eb5ce6a592d4b74e")
            .sound("9th", "9d2a5e5a6ceafefd827e6934e1eacd08")
            .sound("Accident", "c262c77342401deda78d772978073072")
            .sound("Ahead", "26625fa948d529c314ff994a7f8ebc94")
            .sound("AndMiddle", "83bbca9029c0d117327673b53a49b432")
            .sound("AndRight", "5122b89fb1fe76c556910f23cc03df1e")
            .sound("AtLeft", "518db1f8db117c45a7568500ed5527d2")
            .sound("AtMiddle", "ca1f05a291877e86450c1e9c01f54323")
            .sound("AtRight", "165d1cf0e2fd659cdf468952dfd178b4")
            .sound("Begin", "daac372b8d10a4ea9ef0e45eb86bfc53")
            .sound("BoardFerry", "2b18db8316c878df39e415c48d7c0200")
            .sound("CrossRoadCamera", "6d1ebb44774a125a2bbb40b63a3e6dc3")
            .sound("Danger", "dc8d29094ae760baa811b3ca9abd54ab")
            .sound("Exit", "195da47f1da6c5849f759138a47b0ee5")
            .sound("FasterRouteAvailable", "344f9cb13c86dfcd5d08b5948e82f8de")
            .sound("ForbiddenStopCamera", "1d92b9bc44db470fd1e6d31e69b371ae")
            .sound("Forward", "3f0fe306a1b1b8753f846ef64b87bb6c")
            .sound("HardTurnLeft", "60736cae6d7a1a5343b2696a7399eb20")
            .sound("HardTurnRight", "6c48ac385c2cbe184152af3994e211cf")
            .sound("InCircularMovement", "1cef7eee9061d6798608828e22fedc8c")
            .sound("Kilometer", "1e06983392bc968b5a0eb94460e4c1b7")
            .sound("Kilometers", "30e50c535bda03bad2697897deab6d2a")
            .sound("Kilometers2_4", "6b280fc9bb9f7f19f265306510ebf30f")
            .sound("LandmarkAfterBridge", "1d387e1104db518887ed5a5a600d8648")
            .sound("LandmarkAfterTunnel", "c102bcc8f0c5c2bf61e007e8af34fe98")
            .sound("LandmarkAtTrafficLights", "bdb8c8092c24360f022d9e98a6583f7d")
            .sound("LandmarkBeforeBridge", "3cb91e8be7ec3d9cbaf80c23223b57da")
            .sound("LandmarkBeforeTrafficLights", "447690be59fb9f0cbab2026a88587f31")
            .sound("LandmarkBeforeTunnel", "046f41026ba82cbb13e43d8806e0ffc0")
            .sound("LandmarkIntoCourtyard", "26b8f481a6b8385052b135c95c3c70e8")
            .sound("LandmarkIntoTunnel", "07b4da12c082625bba3948cdd956eefd")
            .sound("LandmarkToBridge", "3bad72b552287e863e5e35b0d06decb2")
            .sound("LandmarkToFrontageRoad", "f7d63bb36a7960b72b98e4d29682b82c")
            .sound("LaneCamera", "0585bb547e54cd40edf13592ccf071a2")
            .sound("Meter", "e84ccaaa7ef0a9cfaee294ccb17af014")
            .sound("Meters", "7d1ec28b585c71502cda037ed2161240")
            .sound("Meters2_4", "8d057484c51b6f5dafbbc14b3d6b37e9")
            .sound("MobileCamera", "abb8632d0eb5f60fa680c18374ead725")
            .sound("OneAndHalf", "e46f6c0c053638f2f5e10c17ccd1c21f")
            .sound("Over", "2a63be5d2f5b4c36aff0c3aac43e4f65")
            .sound("ParkingRouteCalculated", "8c07759dcb987b073410f5907200f742")
            .sound("Reconstruction", "692938dbfbcfef40b796b368d9bc7deb")
            .sound("RoadMarkingCamera", "ce4eb828234a6d8fc191bb4683020828")
            .sound("RouteCalculated", "f558235934e4d1a476b89464303e3902")
            .sound("RouteFinished", "d6f32c35ae06f37ba5d7888f9ad77791")
            .sound("RouteLost", "aab4d559b15869a389872b9ada856521")
            .sound("RouteRecalculated", "d72859b7d20d82b5c85ff3aff9db2d50")
            .sound("RouteReturn", "54424299d21efeacd256585617a1d0e6")
            .sound("RouteViaPoint", "db4efc15c72ff017608b4cb37795af3b")
            .sound("RouteWillFinish", "d8c7f8c9f8f548b6cacf54f1bd62d959")
            .sound("Row", "0d22d542b0ef19b3200b6bd75a26d88e")
            .sound("SchoolAhead", "e966415826942145375e34ab18c06a66")
            .sound("Speed100", "4c503d725c6a05e95eba49129ae044d9")
            .sound("Speed110", "0b666394172c7c972f678f081cdaaa59")
            .sound("Speed120", "c2f58fa0ef171f9d5c8b741303e2ad62")
            .sound("Speed130", "956589f29e16a98e1e9fc95dcce45606")
            .sound("Speed30", "0e978ee01fcb2ac7ccaef8bf2dd42bfb")
            .sound("Speed40", "7b5206fcc441fa7b9c2cc4ef5f6fe7f7")
            .sound("Speed50", "8f76874bbc125fa3821515882fe961e3")
            .sound("Speed60", "079b13941d1273a2e5d2e9c1a880b5ef")
            .sound("Speed70", "18bfe01fdb23401e922cc3a0d9c56c99")
            .sound("Speed80", "de87e091d3d8e914f6fb5c08e6deeebd")
            .sound("Speed90", "7e01a04f2234d46f97875bc5159d9e8b")
            .sound("SpeedCamera", "62d98a42e371df649dd0633228097337")
            .sound("TakeLeft", "28eb1ec7b8b5804bc5462888740fbcc7")
            .sound("TakeRight", "c685bf23a12a4b53c229888cf9743fda")
            .sound("Then", "4a300facaf939bc16edf878d53d794f2")
            .sound("TurnBack", "faef218a3ae3ea8ca67f67e282ea7c8b")
            .sound("TurnLeft", "4557ed62b3e243a80df608880efdb9e3")
            .sound("TurnRight", "60dd88d5e30d72b401925a63c87c65bb");
    }

    private Scheme scheme(@SuppressWarnings("unused") String scheme) {
        return new Scheme();
    }

    public String decode(String items) {
        ArrayList<String> phrase = new ArrayList<>();
        for (String token : items.split(":")) {
            final String key = token.trim();
            String annotation = sounds.getOrDefault(key, key);
            annotation = annotations.getOrDefault(annotation, annotation);
            phrase.add(annotation);
        }
        return String.join(" ", phrase);
    }
}
