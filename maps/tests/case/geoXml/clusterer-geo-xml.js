<!doctype html>
<html>
    <head>
        <title>API 2.0</title>
        <meta http-equiv="Content-Type" content="text/html; 
charset=utf-8"/>

<script src="/helper.js"></script>
        <script src="http://localhost:8080/2.0/?ns=ym&load=package.full&mode=debug&lang=ru-RU"></script>
        <script type="text/javascript">
            ymaps.ready(function() {
                var map = new ymaps.Map("name_map", {center: [97.759643,59.269582], zoom: 3, type: "yandex#map"});
                map.controls.add("zoomControl")
                    .add("mapTools")
                    .add(new ymaps.control.TypeSelector(["yandex#map", "yandex#satellite", "yandex#hybrid", "yandex#publicMap"]));
                
                var clusterer = new ymaps.Clusterer();
                ymaps.geoXml.load('data.xml')
                     .then(function (res) {
                    res.geoObjects.each(function (geoObject) {
                    clusterer.add(geoObject);
                });
                map.geoObjects.add(clusterer);
            });
<body style="position: relative; padding: 0; margin: 0;">
    <div id="map" style="height: 500px; width: 100%; overflow: hidden;
float: left">
    </div>

</body>
</html>
