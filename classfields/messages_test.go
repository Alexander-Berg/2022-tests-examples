package bot

import "github.com/YandexClassifieds/shiva/pkg/links"

var (
	newManifestWithoutIncludeMessage = `### 🔴 Ошибки
- **Манифест деплоя** new-service.yml (Prod)
  - Не найден(а) файл конфигурации (service/new.yml)
- **Манифест деплоя** new-service.yml (Test)
  - Не найден(а) файл конфигурации (service/new.yml)`

	updateCollisionOldIncludeMessage = `### 🔴 Ошибки
- **Манифест деплоя** old-service (Prod)
  - Ключ 'COMMON_INT_1' имеет коллизию в файлах: service/common.yml, service/prod.yml. [Подробнее](` + links.DeployManifestConf + `).`

	newCollisionByManifestNewInclude = `### 🔴 Ошибки
- **Манифест деплоя** new-service.yml (Prod)
  - Ключ 'COMMON_INT_1' имеет коллизию в файлах: service/common.yml, service/new.yml. [Подробнее](` + links.DeployManifestConf + `).
- **Манифест деплоя** new-service.yml (Test)
  - Ключ 'COMMON_INT_1' имеет коллизию в файлах: service/common.yml, service/new.yml. [Подробнее](` + links.DeployManifestConf + `).`

	collisionOldManifestUpdateIncludesMessage = `### 🔴 Ошибки
- **Манифест деплоя** old-service2.yml (Prod)
  - Ключ 'COMMON_STRING_1' имеет коллизию в файлах: service/common.yml, service/common2.yml. [Подробнее](` + links.DeployManifestConf + `).`

	allBlockFailMessage = `### 🔴 Ошибки
- **Файл конфигурации** conf/service/fail.yml
  - Не верный формат ключа 'fail-1' [A-Z,0-9,_]. [Подробнее](` + links.DeployManifestVars + `).`

	smInvalidMySQLOwners = `### 🔴 Ошибки
- **Карта сервисов** mysql/new-mdb.yml
  - Поле 'owner' должно вести на https://staff.yandex-team.ru. [Подробнее](` + links.ServiceMapSrc + `).`

	smInvalidMDBCluster = `### 🔴 Ошибки
- **Карта сервисов** mysql/new-mdb.yml
  - Указанный кластер 'mdb888888888' MDB не существует. [Подробнее](` + links.ServiceMapType + `).
  - Кластер 'mdb999999999' имеет имя 'new-mdb-neprod', а должен быть 'new-mdb-prod'. [Подробнее](` + links.ServiceMapType + `).`

	smInvalidMySQLDependency = `### 🔴 Ошибки
- **Карта сервисов** new-service.yml
  - Карта 'maps/mysql/old-mysql-invalid.yml' не найдена
  - В зависимой карте 'mysql/old-mysql-invalid' обнаружена ошибка - Не найден(а) карта сервисов (maps/mysql/old-mysql-invalid.yml). [Подробнее](` + links.ServiceMap + `).`

	smInvalidMySQLDependencyInterface = `### 🔴 Ошибки
- **Карта сервисов** new-service.yml
  - В карте 'old-mysql' не найден блок 'provides/name' с именем 'invalid' для сервиса 'new-service'. [Подробнее](` + links.ServiceMapInterfaceName + `), [еще](` + links.ServiceMapContainerName + `).
  - В поле 'DependsOn' для новой зависимости 'mysql/old-mysql' необходимо получить подтверждение от владельца сервиса. После получения подтверждения проверку необходимо перезапустить. [Подробнее](` + links.ServiceMapDependsOn + `).`

	smNewDependencyReviewNeededMessage = `### 🔴 Ошибки
- **Карта сервисов** new-service.yml
  - В поле 'DependsOn' для новой зависимости 'old-service2' необходимо получить подтверждение от владельца сервиса. После получения подтверждения проверку необходимо перезапустить. [Подробнее](` + links.ServiceMapDependsOn + `).`

	smOldDependencyReviewNeededMessage = `### 🔴 Ошибки
- **Карта сервисов** old-personal-service.yml
  - В поле 'DependsOn' для новой зависимости 'old-service2' необходимо получить подтверждение от владельца сервиса. После получения подтверждения проверку необходимо перезапустить. [Подробнее](` + links.ServiceMapDependsOn + `).`

	smNewDependencyOnMultipleOwnersReviewNeededMessage = `### 🔴 Ошибки
- **Карта сервисов** new-service.yml
  - В поле 'DependsOn' для новой зависимости 'old-service3' необходимо получить подтверждение от владельца сервиса. После получения подтверждения проверку необходимо перезапустить. [Подробнее](` + links.ServiceMapDependsOn + `).`

	newServiceNotification = `
@ibiryulin
Новые сервисы:
- [new-service](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/new-service.yml?rev=some-oid)

`

	newServiceMySQLNotification = `
@ibiryulin
Новые сервисы:
- [new-mdb](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/mysql/new-mdb.yml?rev=some-oid)

`

	newServicePostgreSQLNotification = `
@ibiryulin
Новые сервисы:
- [new-mdb](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/postgresql/new-mdb.yml?rev=some-oid)

`

	newServiceWithDependenciesNotification = `
@ibiryulin
Новые сервисы:
- [new-service](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/new-service.yml?rev=some-oid)
 Зависимости:
  - [old-service2 - api](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/old-service2.yml?rev=trunk-oid)

`

	newServiceWithMySQLDependenciesNotification = `
@ibiryulin
Новые сервисы:
- [new-service](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/new-service.yml?rev=some-oid)
 Зависимости:
  - [mysql/old-mysql - old](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/mysql/old-mysql.yml?rev=trunk-oid)

`

	oldServiceWithNewDependenciesNotification = `
@ibiryulin
Добавлены зависимости в старые сервисы:
- [old-service](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/old-service.yml?rev=some-oid)
 Зависимости:
  - [old-service2 - api](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/old-service2.yml?rev=trunk-oid)

`
	oldPersonalServiceWithNewDependenciesNotification = `
@ibiryulin
Добавлены зависимости в старые сервисы:
- [old-personal-service](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/old-personal-service.yml?rev=some-oid)
 Зависимости:
  - [old-service2 - api](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/old-service2.yml?rev=trunk-oid)

`

	newServiceWithDependenciesOnMultipleOwnersNotification = `
@ibiryulin
Новые сервисы:
- [new-service](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/new-service.yml?rev=some-oid)
 Зависимости:
  - [old-service3 - api](https://a.yandex-team.ru/arc_vcs/classifieds/services/maps/old-service3.yml?rev=trunk-oid)

`

	failDelegateMessage = `### 🔴 Ошибки
- **Манифест деплоя** old-service.yml (Prod)
  - Для сервиса 'old-service' не делегирован секрет 'sec-2'. [Подробнее](` + links.TemplatesSecrets + `).`

	unknownServiceAddressTemplateMessage = `### 🔴 Ошибки
- **Манифест деплоя** old-service.yml (Prod)
  - Не удалось найти сервис 'shiva', указанный в переменной 'SHIVA_URL'.`

	unknownServiceTvmTemplateMessage = `### 🔴 Ошибки
- **Манифест деплоя** old-service.yml (Prod)
  - Не удалось найти сервис 'shiva', указанный в переменной 'SHIVA_TVM'.`

	unknownProviderServiceAddressTemplateMessage = `### 🔴 Ошибки
- **Манифест деплоя** old-service.yml (Prod)
  - Шаблон в переменной SHIVA_HOST невалидный, так как у сервиса 'shiva-tg' в карте сервисов отсутствует провайдер 'qwerty'.`

	invalidServiceAddressTemplateMessage = `### 🔴 Ошибки
- **Манифест деплоя** old-service.yml (Prod)
  - Шаблон в переменной SHIVA_TVM невалидный. Используйте автогенерируемую переменную _DEPLOY_G_TVM_ID.`

	badQueueFailMessage = `### 🔴 Ошибки
- **Карта сервисов** new-service.yml
  - В карте 'new-service' задана несуществующая очередь 'NOVOID'. [Подробнее](` + links.ServiceMapStarTrack + `).`

	badGeobaseFailMessage = `### 🔴 Ошибки
- **Манифест деплоя** new-service.yml (Prod)
  - В поле 'geobase_version' указана неподдерживаемая версия геобазы. [Подробнее](` + links.DeployManifestGeoBase + `).
- **Манифест деплоя** new-service.yml (Test)
  - В поле 'geobase_version' указана неподдерживаемая версия геобазы. [Подробнее](` + links.DeployManifestGeoBase + `).`

	legacyOwnerFailMessage = `### 🔴 Ошибки
- **Карта сервисов** old-service.yml
  - Поле 'owner' является устаревшим и в будущем будет удалено. Используйте поле 'owners'. [Подробнее](` + links.ServiceMapOwners + `).
  - Поле 'owners' должно быть заполнено. [Подробнее](` + links.ServiceMapOwners + `).`

	dependentMapOwnerNotFoundMessage = `### 🔴 Ошибки
- **Карта сервисов** new-service.yml
  - В зависимой карте 'invalid-service' обнаружена ошибка - пользователь '' не найден`

	mapTransferToAnotherOwnerMessage = `### 🔴 Ошибки
- **Карта сервисов** old-personal-service.yml
  - При модификации карты необходимо получить подтверждение от нового владельца сервиса. После получения подтверждения проверку необходимо перезапустить. [Подробнее](` + links.ServiceMapOwners + `).`

	mapStoleByAnotherOwnerMessage = `### 🔴 Ошибки
- **Карта сервисов** old-personal-service.yml
  - При модификации карты необходимо получить подтверждение от владельца сервиса. После получения подтверждения проверку необходимо перезапустить. [Подробнее](` + links.ServiceMapOwners + `).`

	mapChangedByThirdPersonMessage = `### 🔴 Ошибки
- **Карта сервисов** old-personal-service.yml
  - При модификации карты необходимо получить подтверждение от владельца сервиса. После получения подтверждения проверку необходимо перезапустить. [Подробнее](` + links.ServiceMapOwners + `).
  - При модификации карты необходимо получить подтверждение от нового владельца сервиса. После получения подтверждения проверку необходимо перезапустить. [Подробнее](` + links.ServiceMapOwners + `).`

	confOrEnvWithConfig = `### 🔴 Ошибки
- **Манифест деплоя** old-service.yml (Prod)
  - Используются старые ('conf', 'env') и новое ('config') поле. Используйте 'config'. [Подробнее](` + links.DeployManifestAppConf + `).`

	stopForMap = `### 🔴 Ошибки
- **Карта сервисов** delete-not-stopped.yml
  - Нужно остановить все копии сервиса 'delete-not-stopped' перед удалением его карты`

	stopForManifest = `### 🔴 Ошибки
- **Карта сервисов** delete-not-stopped.yml
  - Нужно остановить все копии сервиса 'delete-not-stopped' перед удалением его карты`

	deleteWithoutManifest = `### 🔴 Ошибки
- **Карта сервисов** old-service.yml
  - Нужно удалить манифест для сервиса 'old-service'`

	deleteWithDependings = `### 🔴 Ошибки
- **Карта сервисов** depends-test-service.yml
  - Сервис 'depends-service' зависит от 'depends-test-service', поэтому нельзя удалить его карту`

	warnLegacyConf = `#### 🟡 Предупреждения
- **Манифест деплоя** new-service.yml (Prod)
  - Поле 'conf' устарело и в будущем будет удалено, используйте 'config'. [Подробнее](` + links.DeployManifestAppConf + `).`

	warnLegacyEnv = `#### 🟡 Предупреждения
- **Манифест деплоя** new-service.yml (Prod)
  - Поле 'env' устарело и в будущем будет удалено, используйте 'config'. [Подробнее](` + links.DeployManifestAppConf + `).`

	errorAndWarningText = `### 🔴 Ошибки
- **Карта сервисов** mysql/new-mdb.yml
  - В карте 'new-mdb' задана несуществующая очередь 'NOVOID'. [Подробнее](` + links.ServiceMapStarTrack + `).
#### 🟡 Предупреждения
- **Карта сервисов** mysql/new-mdb.yml
  - Поле 'mdb_mysql' является устаревшим и в будущем будет удалено. Используйте поле 'mdb_cluster'. [Подробнее](` + links.ServiceMapMdbCluster + `).`
	yamlServiceMap = `### 🔴 Ошибки
- **Карта сервисов** new-service.yaml
  - Формат файла 'yaml' не поддерживается, используйте 'yml' maps/new-service.yaml`
	yamlManifest = `### 🔴 Ошибки
- **Манифест деплоя** new-service.yaml
  - Формат файла 'yaml' не поддерживается, используйте 'yml' deploy/new-service.yaml`
	yamlInclude = `### 🔴 Ошибки
- **Файл конфигурации** conf/service/new.yaml
  - Формат файла 'yaml' не поддерживается, используйте 'yml' conf/service/new.yaml`
	ymlExtraField = `#### 🟡 Предупреждения
- **Манифест деплоя** old-service.yml (Prod)
  - Неизвестное поле 'config.filess'`
	mySQLType = `#### 🟡 Предупреждения
- **Карта сервисов** mysql/old-mysql.yml
  - Тип 'mysql' является устаревшим. Все базы переехали в MDB - используйте тип 'mdb_mysql'. [Подробнее](https://docs.yandex-team.ru/classifieds-infra/service-map#type).`
	mdbMySQLField = `#### 🟡 Предупреждения
- **Карта сервисов** mysql/new-mdb.yml
  - Поле 'mdb_mysql' является устаревшим и в будущем будет удалено. Используйте поле 'mdb_cluster'. [Подробнее](` + links.ServiceMapMdbCluster + `).`
	incorrectYamlInclude = `### 🔴 Ошибки
- **Файл конфигурации** conf/service/incorrect.yaml
  - Формат yaml не валиден. Ошибка: yaml: line 3: could not find expected ':'. [Подробнее](` + links.DeployManifestAppConf + `).`
	incorrectYmlManifest = `### 🔴 Ошибки
- **Манифест деплоя** new-service.yml (Prod)
  - Формат yaml не валиден. Ошибка: yaml: line 3: mapping values are not allowed in this context. [Подробнее](` + links.DeployManifest + `).
- **Манифест деплоя** new-service.yml (Test)
  - Формат yaml не валиден. Ошибка: yaml: line 3: mapping values are not allowed in this context. [Подробнее](` + links.DeployManifest + `).`
	incorrectYmlSMap = `### 🔴 Ошибки
- **Карта сервисов** new-service.yaml
  - Формат yaml не валиден. Ошибка: yaml: unmarshal errors:
  line 9: cannot unmarshal !!map into []*service_map.yamlServiceProvides. [Подробнее](` + links.ServiceMap + `).`
	notDefinedFormat = `### 🔴 Ошибки
- **Файл конфигурации** conf/yaml
  - Не определен формат для файла conf/yaml`
	dependentMapOwnersError = `### 🔴 Ошибки
- **Карта сервисов** new-service.yml
  - В зависимой карте 'old-service-wrong-owners' обнаружена ошибка - группа 'yandex_personal_veserv_infa_mnt' не найдена`
	languageUnknownError = `### 🔴 Ошибки
- **Карта сервисов** new-service.yml
  - Указанный в поле 'language' язык не поддерживается. [Подробнее](` + links.ServiceMapLanguage + `).`
)
