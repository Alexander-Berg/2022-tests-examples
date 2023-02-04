# !/usr/bin/python3

from ads.bsyeti.big_rt.lib.serializable_profile.codegen import profiles

if __name__ == "__main__":
    profiles.main("NBigRT::NTests", "ads/bsyeti/big_rt/lib/serializable_profile/tests/profiles/proto", generated_is_empty=True)
