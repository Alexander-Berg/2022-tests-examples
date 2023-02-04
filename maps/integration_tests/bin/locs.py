MSK_LOCS = [
    [55.756567, 37.602243],
    [55.754776, 37.611298],
]

VLADI_LOCS = [
    [43.118605, 131.921211],
    [43.124930, 131.922686]
]

OMSK_LOCS = [
    [54.977474, 73.313070],
    [54.983471, 73.384052]
]

NOVOSIB_LOCS = [
    [55.011931, 82.960397],
    [54.972254, 82.884866],
]

ROSTOV_LOCS = [
    [47.238514, 39.578442],
    [47.288366, 39.847930],
]


NIZHNY_NOVGOROD_LOCS = [
    [56.214225, 43.827601],
    [56.301847, 44.082533],
]

NIZHNY_NOVG_SARANSK_LOCS = [
    [56.298052, 43.985216],
    [54.203726, 45.18975],
]

VORONEZH_LIPETSK_LOCS = [
    [51.661535, 39.200287],
    [52.608782, 39.599346],
]

VORONEZH_TAMBOV_LOCS = [
    [51.661535, 39.200287],
    [52.721219, 41.4522743],
]

SAMARA_LOCS = [
    [53.070591, 50.028842],
    [53.309482, 50.290386],
]

KRASNODAR_LOCS = [
    [45.161538, 38.983824],
    [45.087708, 38.976778],
]

KAZAN_LOCS = [
    [55.798551, 49.106324],
    [55.856840, 48.873185],
]

KYIV_LOCS = [
    [50.45, 30.523333],
    [50.539496, 30.266108],
]

BASHKORTOSTAN_MELEUZ_LOCS = [
    [52.965733, 55.942081],
    [52.961557, 55.905369],
]

BASHKORTOSTAN_UFA_LOCS = [
    [54.731262, 55.991998],
    [54.731808, 55.923591]
]

BASHKORTOSTAN_UFA_MELEUZ_LOCS = [
    [54.731262, 55.991998],
    [52.961557, 55.905369],
]

BELGORODSKAYA_OBLAST_BELGOROD = [
    [50.560526, 36.588679],
    [50.631934, 36.565391]
]

BELGORODSKAYA_OBLAST_STARIY_OSKOL = [
    [51.335223, 37.910522],
    [51.296281, 37.842716],
]

BELGORODSKAYA_OBLAST_BELGOROD_STARIY_OSKOL = [
    [50.560526, 36.588679],
    [51.335223, 37.910522],
]

BRYANSKAYA_OBLAST_BRYANSK = [
    [53.263182, 34.335675],
    [53.236572, 34.474776]
]

BRYANSKAYA_OBLAST_KLINCY = [
    [52.756554, 32.233740],
    [52.764182, 32.286784],
]

BRYANSKAYA_OBLAST_BRYANSK_KLINCY = [
    [53.244195, 34.333792],
    [52.756554, 32.233740],
]

KALUZHSKAYA_OBLAST_KALUGA_KIROV = [
    [54.520722, 36.248540],
    [54.421717, 36.326938]
]

KURSKAYA_OBLAST_KURSK_KURCHATOV = [
    [51.725712, 36.167146],
    [51.658076, 35.657901],
]

ORENBURSKAYA_OBLAST_ORENBURG = [
    [51.784583, 55.128545],
    [51.870068, 55.139198],
]

ORENBURSKAYA_OBLAST_ORENBURG_GAI = [
    [51.784583, 55.128545],
    [51.465818, 58.449334]
]

ORLOVSKAYA_OBLAST_OREL_LIVNY = [
    [52.966791, 36.094048],
    [52.426371, 37.587709],
]

PENZENSKAYA_OBLAST_PENZA_KUZNECK = [
    [53.212724, 44.960455],
    [53.113680, 46.618063]
]

RYAZANSKAYA_OBLAST_RYAZAN_SKOPIN = [
    [54.620689, 39.773820],
    [53.814196, 39.542513]
]

SAMARSKAYA_OBLAST_SAMARA = [
    [53.218053, 50.219963],
    [53.285745, 50.302618]
]

SAMARSKAYA_OBLAST_SAMARA_NEFTEGORSK = [
    [53.218053, 50.219963],
    [52.805852, 51.162911]
]

SARATOVSKAYA_OBLAST_SARATOV_MARKS = [
    [51.535307, 45.969309],
    [51.711885, 46.752603]
]

SMOLENSKAYA_OBLAST_SMOLENSK_ROSLAVL = [
    [54.766970, 32.040997],
    [53.935293, 32.850587]
]

TAMBOVSKAYA_OBLAST_TAMBOV_RASKAZOVO = [
    [52.729616, 41.471125],
    [52.657967, 41.897148],
]

TULSKAYA_OBLAST_TULA_ALEKSIN = [
    [54.191004, 37.607198],
    [54.488755, 37.008216],
]

SVERDLOVSK_OBLAST_EKATERINBURG_NIZHNY_TAGIL = [
    [56.833333, 60.583333],
    [57.916667, 59.966667],
]

SVERDLOVSK_OBLAST_EKATERINBURG_KRASNOTURINSK = [
    [56.833333, 60.583333],
    [59.770908, 60.190351],
]

CHELYABINSK_OBLAST_CHELYABNSK_ZLAOUST = [
    [55.155813, 61.501296],
    [55.172969, 59.670027],
]

CHELYABINSK_OBLAST_ZLAOUST_BELORETSK = [
    [55.172969, 59.670027],
    [53.970995, 58.406591],
]

CHELYABINSK_OBLAST_BELORETSK_MAGNITOGORSK = [
    [53.957708, 58.406947],
    [53.422174, 58.984082],
]

CHELYABINSK_OBLAST_MAGNITOGORSK_CHELYABINSK = [
    [53.422174, 58.984082],
    [55.155813, 61.501296],
]


def list_to_dict(l):
    return dict(
        lat=l[0],
        lon=l[1]
    )


class Region:
    def __init__(self, name, locations, time_zone_shift_hours):
        self.name = name
        self.locations = list(map(list_to_dict, locations))
        self.time_zone_shift_hours = time_zone_shift_hours


REGIONS = [
    Region("Moscow", MSK_LOCS, +3),
    Region("Vladivostok", VLADI_LOCS, +10),
    Region("Omsk", OMSK_LOCS, +6),
    Region("Novosibirsk", NOVOSIB_LOCS, +6),
    Region("Rostov", ROSTOV_LOCS, +3),
    Region("Nizhny Novgorod", NIZHNY_NOVGOROD_LOCS, +3),
    Region("Nizhny Novgorod - Saransk", NIZHNY_NOVG_SARANSK_LOCS, +3),
    Region("Voronezh - Lipetsk", VORONEZH_LIPETSK_LOCS, +3),
    Region("Voronezh - Tambov", VORONEZH_TAMBOV_LOCS, +3),
    Region("Samara", SAMARA_LOCS, +4),
    Region("Krasnodar", KRASNODAR_LOCS, +3),
    Region("Kazan", KAZAN_LOCS, +3),
    Region("Kyiv", KYIV_LOCS, +2),

    Region("Bashkortostan - Meleuz",  BASHKORTOSTAN_MELEUZ_LOCS, +5),
    Region("Bashkortostan - Ufa",  BASHKORTOSTAN_UFA_LOCS, +5),
    Region("Bashkortostan - Ufa - Meleuz",  BASHKORTOSTAN_UFA_MELEUZ_LOCS, +5),

    Region("Belgorodskaya Oblast - Belgorod", BELGORODSKAYA_OBLAST_BELGOROD, +3),
    Region("Belgorodskaya Oblast - Stariy Oskol", BELGORODSKAYA_OBLAST_STARIY_OSKOL, +3),
    Region("Belgorodskaya Oblast - Belgorod - Stariy Oskol", BELGORODSKAYA_OBLAST_BELGOROD_STARIY_OSKOL, +3),

    Region("Bryanskaya Oblast - Bryansk", BRYANSKAYA_OBLAST_BRYANSK, +3),
    Region("Bryanskaya Oblast - Klincy", BRYANSKAYA_OBLAST_KLINCY, +3),
    Region("Bryanskaya Oblast - Bryansk - Klincy", BRYANSKAYA_OBLAST_BRYANSK_KLINCY, +3),

    Region("Kaluzhskaya Oblast - Kaluga - Kirov", KALUZHSKAYA_OBLAST_KALUGA_KIROV, +3),

    Region("Kurskaya Oblast - Kursk - Kurchatov", KURSKAYA_OBLAST_KURSK_KURCHATOV, +3),

    Region("Orenburgskaya Oblast - Orenburg", ORENBURSKAYA_OBLAST_ORENBURG, +5),
    Region("Orenburgskaya Oblast - Orenburg - Gai", ORENBURSKAYA_OBLAST_ORENBURG_GAI, +5),

    Region("Orlovskaya Oblast - Orel - Livny", ORLOVSKAYA_OBLAST_OREL_LIVNY, +3),

    Region("Penzenskaya Oblast - Penza - Kuzneck", PENZENSKAYA_OBLAST_PENZA_KUZNECK, +3),

    Region("Ryazanskaya Oblast - Ryazan - Skopin", RYAZANSKAYA_OBLAST_RYAZAN_SKOPIN, +3),

    Region("Samarskaya Oblast - Samara", SAMARSKAYA_OBLAST_SAMARA, +4),
    Region("Samarskaya Oblast - Samara - Neftegorsk", SAMARSKAYA_OBLAST_SAMARA_NEFTEGORSK, +4),

    Region("Saratovskaya Oblast - Saratov - Marks", SARATOVSKAYA_OBLAST_SARATOV_MARKS, +4),

    Region("Smolenskaya Oblast - Smolensk - Roslav", SMOLENSKAYA_OBLAST_SMOLENSK_ROSLAVL, +3),

    Region("Tambovskaya Oblast - Tambov - Raskazovo", TAMBOVSKAYA_OBLAST_TAMBOV_RASKAZOVO, +3),

    Region("Tulskaya Oblast - Tula - Aleksin", TULSKAYA_OBLAST_TULA_ALEKSIN, +3),

    Region("Sverdlovsk Oblast - Ekaterinburg - Nizhny Tagil", SVERDLOVSK_OBLAST_EKATERINBURG_NIZHNY_TAGIL, +5),
    Region("Sverdlovsk Oblast - Ekaterinburg - Krasnoturinsk", SVERDLOVSK_OBLAST_EKATERINBURG_KRASNOTURINSK, +5),

    Region("Chelyabinsk oblast - Chelyabinsk - Zlatoust", CHELYABINSK_OBLAST_CHELYABNSK_ZLAOUST, +5),
    Region("Chelyabinsk oblast - Zlatoust - Beloretsk", CHELYABINSK_OBLAST_ZLAOUST_BELORETSK, +5),
    Region("Chelyabinsk oblast - Beloretsk - Magnitogorsk", CHELYABINSK_OBLAST_BELORETSK_MAGNITOGORSK, +5),
    Region("Chelyabinsk oblast - Magnitogorsk - Chelyabinsk", CHELYABINSK_OBLAST_MAGNITOGORSK_CHELYABINSK, +5),
]
