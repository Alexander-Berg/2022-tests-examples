package com.yandex.maps.testapp.common_routing;


import com.yandex.mapkit.geometry.Subpolyline;

public interface ConstructionResolver
{
    Subpolyline subpolyline(int index);
    Integer id(int index);
}
