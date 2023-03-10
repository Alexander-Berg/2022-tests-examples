/**
 * User: agoryunova
 * Date: 18.04.14
 * Time: 14:33
 */
function getPlacemarks() {

    var placemarks = [
        new ymaps.Placemark([55.788309250957795, 37.692083928462495], {balloonContent: 0, hintContent: 0, clusterCaption: 0}, {}),
        new ymaps.Placemark([55.92449441730343, 37.90669745357573], {balloonContent: 1, hintContent: 1, clusterCaption: 1}, {}),
        new ymaps.Placemark([55.7663972850163, 37.73537355087083], {balloonContent: 2, hintContent: 2, clusterCaption: 2}, {}),
        new ymaps.Placemark([55.88459804742949, 37.50490579211456], {balloonContent: 3, hintContent: 3, clusterCaption: 3}, {}),
        new ymaps.Placemark([55.63074662110954, 37.4710024164173], {balloonContent: 4, hintContent: 4, clusterCaption: 4}, {}),
        new ymaps.Placemark([55.85877468511649, 37.64508371708271], {balloonContent: 5, hintContent: 5, clusterCaption: 5}, {}),
        new ymaps.Placemark([55.62427568039548, 37.42880883376839], {balloonContent: 6, hintContent: 6, clusterCaption: 6}, {}),
        new ymaps.Placemark([55.86034067657231, 37.8017066677543], {balloonContent: 7, hintContent: 7, clusterCaption: 7}, {}),
        new ymaps.Placemark([55.75973882246323, 37.96166437530101], {balloonContent: 8, hintContent: 8, clusterCaption: 8}, {}),
        new ymaps.Placemark([55.764770806358044, 37.60114026235298], {balloonContent: 9, hintContent: 9, clusterCaption: 9}, {}),
        new ymaps.Placemark([55.644809766520154, 37.488446341024805], {balloonContent: 10, hintContent: 10, clusterCaption: 10}, {}),
        new ymaps.Placemark([55.89792775024167, 37.366009372556995], {balloonContent: 11, hintContent: 11, clusterCaption: 11}, {}),
        new ymaps.Placemark([55.75090353551447, 37.73293941672219], {balloonContent: 12, hintContent: 12, clusterCaption: 12}, {}),
        new ymaps.Placemark([55.79167955324749, 37.96291071846409], {balloonContent: 13, hintContent: 13, clusterCaption: 13}, {}),
        new ymaps.Placemark([55.65150675925323, 37.46385305017012], {balloonContent: 14, hintContent: 14, clusterCaption: 14}, {}),
        new ymaps.Placemark([55.80753844796887, 37.7453816545798], {balloonContent: 15, hintContent: 15, clusterCaption: 15}, {}),
        new ymaps.Placemark([55.92502521150429, 37.563586638710525], {balloonContent: 16, hintContent: 16, clusterCaption: 16}, {}),
        new ymaps.Placemark([55.79103938764434, 37.36123322514087], {balloonContent: 17, hintContent: 17, clusterCaption: 17}, {}),
        new ymaps.Placemark([55.71836947325461, 37.43051578199212], {balloonContent: 18, hintContent: 18, clusterCaption: 18}, {}),
        new ymaps.Placemark([55.898478723934986, 37.85718706142998], {balloonContent: 19, hintContent: 19, clusterCaption: 19}, {}),
        new ymaps.Placemark([55.563433483685934, 37.86866002468142], {balloonContent: 20, hintContent: 20, clusterCaption: 20}, {}),
        new ymaps.Placemark([55.550192616261334, 37.85705864477987], {balloonContent: 21, hintContent: 21, clusterCaption: 21}, {}),
        new ymaps.Placemark([55.688642150262645, 37.84518142594007], {balloonContent: 22, hintContent: 22, clusterCaption: 22}, {}),
        new ymaps.Placemark([55.72464542964483, 37.95820224181935], {balloonContent: 23, hintContent: 23, clusterCaption: 23}, {}),
        new ymaps.Placemark([55.786463074303555, 37.97839103589777], {balloonContent: 24, hintContent: 24, clusterCaption: 24}, {}),
        new ymaps.Placemark([55.59245294600664, 37.82557941550604], {balloonContent: 25, hintContent: 25, clusterCaption: 25}, {}),
        new ymaps.Placemark([55.73786180032246, 37.35061231148703], {balloonContent: 26, hintContent: 26, clusterCaption: 26}, {}),
        new ymaps.Placemark([55.55110251566694, 37.39254342515179], {balloonContent: 27, hintContent: 27, clusterCaption: 27}, {}),
        new ymaps.Placemark([55.809738305513264, 37.92431946988782], {balloonContent: 28, hintContent: 28, clusterCaption: 28}, {}),
        new ymaps.Placemark([55.5462650657083, 37.92003418276491], {balloonContent: 29, hintContent: 29, clusterCaption: 29}, {}),
        new ymaps.Placemark([55.576482820867454, 37.48049921813928], {balloonContent: 30, hintContent: 30, clusterCaption: 30}, {}),
        new ymaps.Placemark([55.637440071532055, 37.77981714410399], {balloonContent: 31, hintContent: 31, clusterCaption: 31}, {}),
        new ymaps.Placemark([55.65650036771664, 37.38465236213725], {balloonContent: 32, hintContent: 32, clusterCaption: 32}, {}),
        new ymaps.Placemark([55.75407004593004, 37.41895765582141], {balloonContent: 33, hintContent: 33, clusterCaption: 33}, {}),
        new ymaps.Placemark([55.57078377532657, 37.87950503817538], {balloonContent: 34, hintContent: 34, clusterCaption: 34}, {}),
        new ymaps.Placemark([55.591956352536144, 37.45283378394871], {balloonContent: 35, hintContent: 35, clusterCaption: 35}, {}),
        new ymaps.Placemark([55.65122437283704, 37.591060618281894], {balloonContent: 36, hintContent: 36, clusterCaption: 36}, {}),
        new ymaps.Placemark([55.82461764983121, 37.472933173897594], {balloonContent: 37, hintContent: 37, clusterCaption: 37}, {}),
        new ymaps.Placemark([55.59076354223214, 37.53691625789853], {balloonContent: 38, hintContent: 38, clusterCaption: 38}, {}),
        new ymaps.Placemark([55.746549552835724, 37.85609572241211], {balloonContent: 39, hintContent: 39, clusterCaption: 39}, {}),
        new ymaps.Placemark([55.73368461840849, 37.936138974085594], {balloonContent: 40, hintContent: 40, clusterCaption: 40}, {}),
        new ymaps.Placemark([55.768054534621996, 37.729161157470486], {balloonContent: 41, hintContent: 41, clusterCaption: 41}, {}),
        new ymaps.Placemark([55.876811987011294, 37.90529911523973], {balloonContent: 42, hintContent: 42, clusterCaption: 42}, {}),
        new ymaps.Placemark([55.876342966874375, 37.788332357025325], {balloonContent: 43, hintContent: 43, clusterCaption: 43}, {}),
        new ymaps.Placemark([55.62738087406382, 37.39648159736445], {balloonContent: 44, hintContent: 44, clusterCaption: 44}, {}),
        new ymaps.Placemark([55.87832837771203, 37.96584343276973], {balloonContent: 45, hintContent: 45, clusterCaption: 45}, {}),
        new ymaps.Placemark([55.89837617043192, 37.50307531150012], {balloonContent: 46, hintContent: 46, clusterCaption: 46}, {}),
        new ymaps.Placemark([55.558811113194054, 37.492840885109594], {balloonContent: 47, hintContent: 47, clusterCaption: 47}, {}),
        new ymaps.Placemark([55.861753421014015, 37.991646268595474], {balloonContent: 48, hintContent: 48, clusterCaption: 48}, {}),
        new ymaps.Placemark([55.63111315601755, 37.80859280164205], {balloonContent: 49, hintContent: 49, clusterCaption: 49}, {}),
        new ymaps.Placemark([55.780756588266, 37.87027959713069], {balloonContent: 50, hintContent: 50, clusterCaption: 50}, {}),
        new ymaps.Placemark([55.78973370243011, 37.84396096517722], {balloonContent: 51, hintContent: 51, clusterCaption: 51}, {}),
        new ymaps.Placemark([55.551615279301956, 37.3191627842729], {balloonContent: 52, hintContent: 52, clusterCaption: 52}, {}),
        new ymaps.Placemark([55.65577227008808, 37.88173270394819], {balloonContent: 53, hintContent: 53, clusterCaption: 53}, {}),
        new ymaps.Placemark([55.83508673407616, 37.79903790858353], {balloonContent: 54, hintContent: 54, clusterCaption: 54}, {}),
        new ymaps.Placemark([55.69386355418418, 37.827349216216504], {balloonContent: 55, hintContent: 55, clusterCaption: 55}, {}),
        new ymaps.Placemark([55.79614653472771, 37.419870437055046], {balloonContent: 56, hintContent: 56, clusterCaption: 56}, {}),
        new ymaps.Placemark([55.65711716766949, 37.78972751924212], {balloonContent: 57, hintContent: 57, clusterCaption: 57}, {}),
        new ymaps.Placemark([55.719629259484655, 37.84081572034975], {balloonContent: 58, hintContent: 58, clusterCaption: 58}, {}),
        new ymaps.Placemark([55.800813341529405, 37.96089109987716], {balloonContent: 59, hintContent: 59, clusterCaption: 59}, {}),
        new ymaps.Placemark([55.55271664575778, 37.827267399346425], {balloonContent: 60, hintContent: 60, clusterCaption: 60}, {}),
        new ymaps.Placemark([55.72061656062101, 37.41432324540566], {balloonContent: 61, hintContent: 61, clusterCaption: 61}, {}),
        new ymaps.Placemark([55.60342384323012, 37.73115702089254], {balloonContent: 62, hintContent: 62, clusterCaption: 62}, {}),
        new ymaps.Placemark([55.6652132756053, 37.923124945712885], {balloonContent: 63, hintContent: 63, clusterCaption: 63}, {}),
        new ymaps.Placemark([55.74728533828152, 37.56634264367077], {balloonContent: 64, hintContent: 64, clusterCaption: 64}, {}),
        new ymaps.Placemark([55.854608538947616, 37.4475541143269], {balloonContent: 65, hintContent: 65, clusterCaption: 65}, {}),
        new ymaps.Placemark([55.5695231640787, 37.64566651654005], {balloonContent: 66, hintContent: 66, clusterCaption: 66}, {}),
        new ymaps.Placemark([55.78730941453947, 37.75210585795631], {balloonContent: 67, hintContent: 67, clusterCaption: 67}, {}),
        new ymaps.Placemark([55.7659899076611, 37.573337645383994], {balloonContent: 68, hintContent: 68, clusterCaption: 68}, {}),
        new ymaps.Placemark([55.66711004645433, 37.36715952879732], {balloonContent: 69, hintContent: 69, clusterCaption: 69}, {}),
        new ymaps.Placemark([55.759021496444724, 37.30485698134854], {balloonContent: 70, hintContent: 70, clusterCaption: 70}, {}),
        new ymaps.Placemark([55.650061659330966, 37.877351467444356], {balloonContent: 71, hintContent: 71, clusterCaption: 71}, {}),
        new ymaps.Placemark([55.56909310520052, 37.76157285579157], {balloonContent: 72, hintContent: 72, clusterCaption: 72}, {}),
        new ymaps.Placemark([55.729770639999124, 37.433154552284954], {balloonContent: 73, hintContent: 73, clusterCaption: 73}, {}),
        new ymaps.Placemark([55.82221483577789, 37.98783347819463], {balloonContent: 74, hintContent: 74, clusterCaption: 74}, {}),
        new ymaps.Placemark([55.63661330297311, 37.47043880939001], {balloonContent: 75, hintContent: 75, clusterCaption: 75}, {}),
        new ymaps.Placemark([55.81034476918461, 37.552312790024565], {balloonContent: 76, hintContent: 76, clusterCaption: 76}, {}),
        new ymaps.Placemark([55.79699020386449, 37.77711511958242], {balloonContent: 77, hintContent: 77, clusterCaption: 77}, {}),
        new ymaps.Placemark([55.63360376859675, 37.41611123484601], {balloonContent: 78, hintContent: 78, clusterCaption: 78}, {}),
        new ymaps.Placemark([55.66449123026314, 37.855761349304345], {balloonContent: 79, hintContent: 79, clusterCaption: 79}, {}),
        new ymaps.Placemark([55.5480071684431, 37.605629084185615], {balloonContent: 80, hintContent: 80, clusterCaption: 80}, {}),
        new ymaps.Placemark([55.847703551830826, 37.70451708112287], {balloonContent: 81, hintContent: 81, clusterCaption: 81}, {}),
        new ymaps.Placemark([55.71713766639995, 37.29952458480822], {balloonContent: 82, hintContent: 82, clusterCaption: 82}, {}),
        new ymaps.Placemark([55.88981981073729, 37.528754913704155], {balloonContent: 83, hintContent: 83, clusterCaption: 83}, {}),
        new ymaps.Placemark([55.56292147744129, 37.57891225715267], {balloonContent: 84, hintContent: 84, clusterCaption: 84}, {}),
        new ymaps.Placemark([55.71651188871957, 37.80526460082311], {balloonContent: 85, hintContent: 85, clusterCaption: 85}, {}),
        new ymaps.Placemark([55.82542700681864, 37.738466228559815], {balloonContent: 86, hintContent: 86, clusterCaption: 86}, {}),
        new ymaps.Placemark([55.73442967324824, 37.60393953332224], {balloonContent: 87, hintContent: 87, clusterCaption: 87}, {}),
        new ymaps.Placemark([55.62925831954019, 37.62246767036542], {balloonContent: 88, hintContent: 88, clusterCaption: 88}, {}),
        new ymaps.Placemark([55.67146934241415, 37.30534644520359], {balloonContent: 89, hintContent: 89, clusterCaption: 89}, {}),
        new ymaps.Placemark([55.759739537078325, 37.82325444304061], {balloonContent: 90, hintContent: 90, clusterCaption: 90}, {}),
        new ymaps.Placemark([55.88846208925625, 37.353765061719436], {balloonContent: 91, hintContent: 91, clusterCaption: 91}, {}),
        new ymaps.Placemark([55.89668503615252, 37.77281994132264], {balloonContent: 92, hintContent: 92, clusterCaption: 92}, {}),
        new ymaps.Placemark([55.72786522013628, 37.39217669740666], {balloonContent: 93, hintContent: 93, clusterCaption: 93}, {}),
        new ymaps.Placemark([55.86217399098622, 37.526430657138384], {balloonContent: 94, hintContent: 94, clusterCaption: 94}, {}),
        new ymaps.Placemark([55.714487519949124, 37.30913826300222], {balloonContent: 95, hintContent: 95, clusterCaption: 95}, {}),
        new ymaps.Placemark([55.65415262956186, 37.3203040563507], {balloonContent: 96, hintContent: 96, clusterCaption: 96}, {}),
        new ymaps.Placemark([55.87705630546071, 37.65379395560868], {balloonContent: 97, hintContent: 97, clusterCaption: 97}, {}),
        new ymaps.Placemark([55.81773117203082, 37.93250716829189], {balloonContent: 98, hintContent: 98, clusterCaption: 98}, {}),
        new ymaps.Placemark([55.667614267228444, 37.763502070314104], {balloonContent: 99, hintContent: 99, clusterCaption: 99}, {}),
        new ymaps.Placemark([55.69365924170811, 37.67595234926685], {balloonContent: 100, hintContent: 100, clusterCaption: 100}, {}),
        new ymaps.Placemark([55.92476598379105, 37.747690342742686], {balloonContent: 101, hintContent: 101, clusterCaption: 101}, {}),
        new ymaps.Placemark([55.76118699851016, 37.69273171631849], {balloonContent: 102, hintContent: 102, clusterCaption: 102}, {}),
        new ymaps.Placemark([55.643622328809805, 37.71142796323457], {balloonContent: 103, hintContent: 103, clusterCaption: 103}, {}),
        new ymaps.Placemark([55.828144476386655, 37.426418813477916], {balloonContent: 104, hintContent: 104, clusterCaption: 104}, {}),
        new ymaps.Placemark([55.84665409738101, 37.848283609722245], {balloonContent: 105, hintContent: 105, clusterCaption: 105}, {}),
        new ymaps.Placemark([55.602087484721736, 37.36716916454808], {balloonContent: 106, hintContent: 106, clusterCaption: 106}, {}),
        new ymaps.Placemark([55.54728780140627, 37.63691788611451], {balloonContent: 107, hintContent: 107, clusterCaption: 107}, {}),
        new ymaps.Placemark([55.90471998135058, 37.94925965906569], {balloonContent: 108, hintContent: 108, clusterCaption: 108}, {}),
        new ymaps.Placemark([55.81637626309374, 37.59589614591588], {balloonContent: 109, hintContent: 109, clusterCaption: 109}, {}),
        new ymaps.Placemark([55.577665240289114, 37.84678523140738], {balloonContent: 110, hintContent: 110, clusterCaption: 110}, {}),
        new ymaps.Placemark([55.90987381326378, 37.9099569016702], {balloonContent: 111, hintContent: 111, clusterCaption: 111}, {}),
        new ymaps.Placemark([55.57551066144116, 37.701604328352374], {balloonContent: 112, hintContent: 112, clusterCaption: 112}, {}),
        new ymaps.Placemark([55.842987021861, 37.4899694908103], {balloonContent: 113, hintContent: 113, clusterCaption: 113}, {}),
        new ymaps.Placemark([55.53318975681794, 37.91051186862515], {balloonContent: 114, hintContent: 114, clusterCaption: 114}, {}),
        new ymaps.Placemark([55.55778792766274, 37.472198318858695], {balloonContent: 115, hintContent: 115, clusterCaption: 115}, {}),
        new ymaps.Placemark([55.59710605196926, 37.9766121866609], {balloonContent: 116, hintContent: 116, clusterCaption: 116}, {}),
        new ymaps.Placemark([55.79580532546474, 37.92885393904126], {balloonContent: 117, hintContent: 117, clusterCaption: 117}, {}),
        new ymaps.Placemark([55.91634457853452, 37.66590233358639], {balloonContent: 118, hintContent: 118, clusterCaption: 118}, {}),
        new ymaps.Placemark([55.8015187712159, 37.74463245093412], {balloonContent: 119, hintContent: 119, clusterCaption: 119}, {}),
        new ymaps.Placemark([55.69027679678211, 37.66932621383003], {balloonContent: 120, hintContent: 120, clusterCaption: 120}, {}),
        new ymaps.Placemark([55.85486977922393, 37.71386945659597], {balloonContent: 121, hintContent: 121, clusterCaption: 121}, {}),
        new ymaps.Placemark([55.63417223054297, 37.44772163337385], {balloonContent: 122, hintContent: 122, clusterCaption: 122}, {}),
        new ymaps.Placemark([55.884677929572156, 37.716880650562786], {balloonContent: 123, hintContent: 123, clusterCaption: 123}, {}),
        new ymaps.Placemark([55.83648462708844, 37.435058285985555], {balloonContent: 124, hintContent: 124, clusterCaption: 124}, {}),
        new ymaps.Placemark([55.88093008914964, 37.88530692262921], {balloonContent: 125, hintContent: 125, clusterCaption: 125}, {}),
        new ymaps.Placemark([55.7919249650996, 37.44188761381648], {balloonContent: 126, hintContent: 126, clusterCaption: 126}, {}),
        new ymaps.Placemark([55.91726137113494, 37.847179486010106], {balloonContent: 127, hintContent: 127, clusterCaption: 127}, {}),
        new ymaps.Placemark([55.76098833033855, 37.57898050794521], {balloonContent: 128, hintContent: 128, clusterCaption: 128}, {}),
        new ymaps.Placemark([55.558988847254454, 37.81944053106909], {balloonContent: 129, hintContent: 129, clusterCaption: 129}, {}),
        new ymaps.Placemark([55.78412618453261, 37.74461707865467], {balloonContent: 130, hintContent: 130, clusterCaption: 130}, {}),
        new ymaps.Placemark([55.77796625003829, 37.88334333755611], {balloonContent: 131, hintContent: 131, clusterCaption: 131}, {}),
        new ymaps.Placemark([55.688500172178976, 37.66016525876094], {balloonContent: 132, hintContent: 132, clusterCaption: 132}, {}),
        new ymaps.Placemark([55.863839786593985, 37.765843932478134], {balloonContent: 133, hintContent: 133, clusterCaption: 133}, {}),
        new ymaps.Placemark([55.828445382928535, 37.84131429101163], {balloonContent: 134, hintContent: 134, clusterCaption: 134}, {}),
        new ymaps.Placemark([55.66953444022072, 37.47116327692621], {balloonContent: 135, hintContent: 135, clusterCaption: 135}, {}),
        new ymaps.Placemark([55.66535896663872, 37.87900209140699], {balloonContent: 136, hintContent: 136, clusterCaption: 136}, {}),
        new ymaps.Placemark([55.890596697222094, 37.32490510040717], {balloonContent: 137, hintContent: 137, clusterCaption: 137}, {}),
        new ymaps.Placemark([55.62442458114548, 37.72791794049536], {balloonContent: 138, hintContent: 138, clusterCaption: 138}, {}),
        new ymaps.Placemark([55.792995703992936, 37.809357097461], {balloonContent: 139, hintContent: 139, clusterCaption: 139}, {}),
        new ymaps.Placemark([55.828124626901335, 37.66669294841759], {balloonContent: 140, hintContent: 140, clusterCaption: 140}, {}),
        new ymaps.Placemark([55.741910968387785, 37.9262408602639], {balloonContent: 141, hintContent: 141, clusterCaption: 141}, {}),
        new ymaps.Placemark([55.65736969195127, 37.318376867563906], {balloonContent: 142, hintContent: 142, clusterCaption: 142}, {}),
        new ymaps.Placemark([55.72025780203719, 37.75675893758799], {balloonContent: 143, hintContent: 143, clusterCaption: 143}, {}),
        new ymaps.Placemark([55.8617120577801, 37.69013038612084], {balloonContent: 144, hintContent: 144, clusterCaption: 144}, {}),
        new ymaps.Placemark([55.85216715702031, 37.731749704283544], {balloonContent: 145, hintContent: 145, clusterCaption: 145}, {}),
        new ymaps.Placemark([55.63677556540871, 37.525395063179], {balloonContent: 146, hintContent: 146, clusterCaption: 146}, {}),
        new ymaps.Placemark([55.63342216064808, 37.98868439590788], {balloonContent: 147, hintContent: 147, clusterCaption: 147}, {}),
        new ymaps.Placemark([55.622399374884935, 37.98752515610741], {balloonContent: 148, hintContent: 148, clusterCaption: 148}, {}),
        new ymaps.Placemark([55.62023478190969, 37.7545003195095], {balloonContent: 149, hintContent: 149, clusterCaption: 149}, {}),
        new ymaps.Placemark([55.55906660652372, 37.5555016909797], {balloonContent: 150, hintContent: 150, clusterCaption: 150}, {}),
        new ymaps.Placemark([55.734772233766606, 37.379631961806275], {balloonContent: 151, hintContent: 151, clusterCaption: 151}, {}),
        new ymaps.Placemark([55.60928878350232, 37.897280303344274], {balloonContent: 152, hintContent: 152, clusterCaption: 152}, {}),
        new ymaps.Placemark([55.607094007254695, 37.891816185686736], {balloonContent: 153, hintContent: 153, clusterCaption: 153}, {}),
        new ymaps.Placemark([55.89621034417976, 37.70774470663437], {balloonContent: 154, hintContent: 154, clusterCaption: 154}, {}),
        new ymaps.Placemark([55.67363758837984, 37.51286678312082], {balloonContent: 155, hintContent: 155, clusterCaption: 155}, {}),
        new ymaps.Placemark([55.582513275605194, 37.76715733610527], {balloonContent: 156, hintContent: 156, clusterCaption: 156}, {}),
        new ymaps.Placemark([55.78421398250416, 37.35117862920871], {balloonContent: 157, hintContent: 157, clusterCaption: 157}, {}),
        new ymaps.Placemark([55.6513259421391, 37.673993527308454], {balloonContent: 158, hintContent: 158, clusterCaption: 158}, {}),
        new ymaps.Placemark([55.68185258267712, 37.49731417079003], {balloonContent: 159, hintContent: 159, clusterCaption: 159}, {}),
        new ymaps.Placemark([55.827496788801305, 37.434294530406454], {balloonContent: 160, hintContent: 160, clusterCaption: 160}, {}),
        new ymaps.Placemark([55.59005275621213, 37.6936274082922], {balloonContent: 161, hintContent: 161, clusterCaption: 161}, {}),
        new ymaps.Placemark([55.873674107947664, 37.39648669919317], {balloonContent: 162, hintContent: 162, clusterCaption: 162}, {}),
        new ymaps.Placemark([55.863997902783815, 37.36059906262509], {balloonContent: 163, hintContent: 163, clusterCaption: 163}, {}),
        new ymaps.Placemark([55.90139360188256, 37.427926314806804], {balloonContent: 164, hintContent: 164, clusterCaption: 164}, {}),
        new ymaps.Placemark([55.90874744100744, 37.82477790993053], {balloonContent: 165, hintContent: 165, clusterCaption: 165}, {}),
        new ymaps.Placemark([55.57489068161278, 37.77750801883795], {balloonContent: 166, hintContent: 166, clusterCaption: 166}, {}),
        new ymaps.Placemark([55.660020629883356, 37.41368902591913], {balloonContent: 167, hintContent: 167, clusterCaption: 167}, {}),
        new ymaps.Placemark([55.85984097094612, 37.63140486283502], {balloonContent: 168, hintContent: 168, clusterCaption: 168}, {}),
        new ymaps.Placemark([55.69117884096484, 37.46472066456901], {balloonContent: 169, hintContent: 169, clusterCaption: 169}, {}),
        new ymaps.Placemark([55.609935196144114, 37.70636857739501], {balloonContent: 170, hintContent: 170, clusterCaption: 170}, {}),
        new ymaps.Placemark([55.92021032435105, 37.72228827533891], {balloonContent: 171, hintContent: 171, clusterCaption: 171}, {}),
        new ymaps.Placemark([55.68644512004416, 37.853870041611486], {balloonContent: 172, hintContent: 172, clusterCaption: 172}, {}),
        new ymaps.Placemark([55.8113227424459, 37.99138844207246], {balloonContent: 173, hintContent: 173, clusterCaption: 173}, {}),
        new ymaps.Placemark([55.83190048115966, 37.90372828650416], {balloonContent: 174, hintContent: 174, clusterCaption: 174}, {}),
        new ymaps.Placemark([55.86892912631902, 37.78676936324101], {balloonContent: 175, hintContent: 175, clusterCaption: 175}, {}),
        new ymaps.Placemark([55.56553815228165, 37.65759176401244], {balloonContent: 176, hintContent: 176, clusterCaption: 176}, {}),
        new ymaps.Placemark([55.779230792084945, 37.54482476591276], {balloonContent: 177, hintContent: 177, clusterCaption: 177}, {}),
        new ymaps.Placemark([55.77352080475676, 37.87297140407955], {balloonContent: 178, hintContent: 178, clusterCaption: 178}, {}),
        new ymaps.Placemark([55.80933878994042, 37.57930373721448], {balloonContent: 179, hintContent: 179, clusterCaption: 179}, {}),
        new ymaps.Placemark([55.66186849583781, 37.387439769603056], {balloonContent: 180, hintContent: 180, clusterCaption: 180}, {}),
        new ymaps.Placemark([55.83145339605703, 37.29027289645584], {balloonContent: 181, hintContent: 181, clusterCaption: 181}, {}),
        new ymaps.Placemark([55.67369785976575, 37.541620283949136], {balloonContent: 182, hintContent: 182, clusterCaption: 182}, {}),
        new ymaps.Placemark([55.902174741135596, 37.292098783230735], {balloonContent: 183, hintContent: 183, clusterCaption: 183}, {}),
        new ymaps.Placemark([55.547909953350434, 37.876369902708944], {balloonContent: 184, hintContent: 184, clusterCaption: 184}, {}),
        new ymaps.Placemark([55.70642879049429, 37.47321130689071], {balloonContent: 185, hintContent: 185, clusterCaption: 185}, {}),
        new ymaps.Placemark([55.77698669547744, 37.78655577106602], {balloonContent: 186, hintContent: 186, clusterCaption: 186}, {}),
        new ymaps.Placemark([55.88349603204587, 37.50173243385999], {balloonContent: 187, hintContent: 187, clusterCaption: 187}, {}),
        new ymaps.Placemark([55.75130347181868, 37.95566198802899], {balloonContent: 188, hintContent: 188, clusterCaption: 188}, {}),
        new ymaps.Placemark([55.53080944930621, 37.53608793645382], {balloonContent: 189, hintContent: 189, clusterCaption: 189}, {}),
        new ymaps.Placemark([55.88504032691879, 37.45325789351538], {balloonContent: 190, hintContent: 190, clusterCaption: 190}, {}),
        new ymaps.Placemark([55.90426239477811, 37.30598184830652], {balloonContent: 191, hintContent: 191, clusterCaption: 191}, {}),
        new ymaps.Placemark([55.717281649378556, 37.31476004016569], {balloonContent: 192, hintContent: 192, clusterCaption: 192}, {}),
        new ymaps.Placemark([55.889696950524595, 37.68372492687686], {balloonContent: 193, hintContent: 193, clusterCaption: 193}, {}),
        new ymaps.Placemark([55.55808216357061, 37.82567557296116], {balloonContent: 194, hintContent: 194, clusterCaption: 194}, {}),
        new ymaps.Placemark([55.61659425477949, 37.96047893337655], {balloonContent: 195, hintContent: 195, clusterCaption: 195}, {}),
        new ymaps.Placemark([55.59704310643544, 37.4115040444971], {balloonContent: 196, hintContent: 196, clusterCaption: 196}, {}),
        new ymaps.Placemark([55.585626507885024, 37.30659318798045], {balloonContent: 197, hintContent: 197, clusterCaption: 197}, {}),
        new ymaps.Placemark([55.844473078143885, 37.648101671087176], {balloonContent: 198, hintContent: 198, clusterCaption: 198}, {}),
        new ymaps.Placemark([55.638771091075725, 37.314371803343796], {balloonContent: 199, hintContent: 199, clusterCaption: 199}, {})
]

    return placemarks

}