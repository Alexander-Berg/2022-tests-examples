#!/bin/bash -x
set -e

PKG_ROOT=/usr

function gen_proto {
    name=$1
    protoc -I $PKG_ROOT/share/proto/ \
        --encode yandex.maps.proto.offline.mrc.indoor.IndoorTrack \
        $PKG_ROOT/share/proto/yandex/maps/proto/common2/geometry.proto \
        $PKG_ROOT/share/proto/yandex/maps/proto/offline-mrc/indoor.proto \
        <$name.pb.txt 1>$name.pb

}

gen_proto indoor_tracks_1
gen_proto indoor_tracks_1_valid
gen_proto indoor_tracks_1_invalid
gen_proto indoor_tracks_2
gen_proto indoor_tracks_3
gen_proto indoor_tracks_4_valid
gen_proto indoor_tracks_4_invalid
gen_proto indoor_tracks_5
gen_proto indoor_tracks_6
