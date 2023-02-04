package bot

import (
	"context"
	"crypto/sha1"
	"encoding/base64"
	"fmt"
	"testing"
	"time"

	"github.com/YandexClassifieds/shiva/cmd/github-app/events"
	"github.com/YandexClassifieds/shiva/cmd/shiva/env/external/reader"
	validation "github.com/YandexClassifieds/shiva/pb/validator/status"
	"github.com/YandexClassifieds/shiva/pkg/arc/diff/file"
	"github.com/YandexClassifieds/shiva/pkg/arcanum"
	"github.com/YandexClassifieds/shiva/pkg/mdb"
	"github.com/YandexClassifieds/shiva/pkg/secrets"
	"github.com/YandexClassifieds/shiva/pkg/template"
	staffMock "github.com/YandexClassifieds/shiva/test/mock/staff"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"

	"github.com/YandexClassifieds/shiva/common/config"
	"github.com/YandexClassifieds/shiva/common/user_error"
	"github.com/YandexClassifieds/shiva/pb/shiva/api/deploy"
	error1 "github.com/YandexClassifieds/shiva/pb/shiva/types/error"
	"github.com/YandexClassifieds/shiva/pb/shiva/types/flags"
	"github.com/YandexClassifieds/shiva/pkg/conductor"
	"github.com/YandexClassifieds/shiva/pkg/feature_flags"
	"github.com/YandexClassifieds/shiva/pkg/include"
	confParser "github.com/YandexClassifieds/shiva/pkg/include/parser"
	confValidator "github.com/YandexClassifieds/shiva/pkg/include/validator"
	"github.com/YandexClassifieds/shiva/pkg/manifest"
	manifestParser "github.com/YandexClassifieds/shiva/pkg/manifest/parser"
	manifestValidator "github.com/YandexClassifieds/shiva/pkg/manifest/validator"
	"github.com/YandexClassifieds/shiva/pkg/service_map"
	sMapParser "github.com/YandexClassifieds/shiva/pkg/service_map/parser"
	sMapValidator "github.com/YandexClassifieds/shiva/pkg/service_map/validator"
	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/test"
	testMock "github.com/YandexClassifieds/shiva/test/mock"
	mqMock "github.com/YandexClassifieds/shiva/test/mock/mq"
	"github.com/YandexClassifieds/shiva/test/mock/service_change"
	"github.com/YandexClassifieds/shiva/test/test_db"
	nomadAPI "github.com/hashicorp/nomad/api"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/mock"
	"github.com/stretchr/testify/require"
	"google.golang.org/grpc"
)

func TestSuccessValidate(t *testing.T) {
	cases := []*TestCase{
		NewCase("new_map", smNew()).
			addComment(newServiceNotification),
		NewCase("new_mysql", smNewMySQL()).
			addComment(newServiceMySQLNotification),
		NewCase("new_postgresql", smNewPostgreSQL()).
			addComment(newServicePostgreSQLNotification),
		NewCase("new_config", smNew(), mNewWithConfig()),
		NewCase("new_all", smNew(), mNewWitNewFile(), inclNew()).
			addComment(newServiceNotification),
		NewCase("new_include", inclNew()),
		NewCase("update_map", smUpdate()),
		NewCase("shadow_update_map", smUpdate()).setUser("ibiryulin"),
		NewCase("approve_reviews_map", smUpdate()).setUser("goodfella").
			addApprovers("ibiryulin"),
		NewCase("map_transfer_to_another_owner", smOwnerChanged()).
			addApprovers("swapster"),
		NewCase("map_stole_by_another_owner", smOwnerChanged()).
			setUser("swapster").addApprovers("ibiryulin"),
		NewCase("map_changed_by_third_person", smOwnerChanged()).
			setUser("m8rge").addApprovers("ibiryulin", "swapster"),
		NewCase("update_manifest", mUpdate()),
		NewCase("update_include", inclUpdateCommon()),
		NewCase("update_include_map", inclUpdateCommon(), smUpdate()),
		NewCase("update_include_manifest", inclUpdateCommon(), mUpdate()),
		NewCase("update_map_manifest", mUpdate(), smUpdate()),
		NewCase("update_all", inclUpdateCommon(), smUpdate(), mUpdate()),
		NewCase("no_collision_new_include", inclNewCollision()),
		NewCase("new_map_skip_ss_err", smNew(), mNewWithSec2Config()).
			addComment(newServiceNotification),
		NewCase("new_map_dependency_by_author", smNewAddDependency()).
			addComment(newServiceWithDependenciesNotification),
		NewCase("new_map_dependency_reviewed", smNewAddDependency()).
			setUser("swapster").
			addApprovers("ibiryulin").
			addComment(newServiceWithDependenciesNotification),
		NewCase("new_map_dependency_mysql_by_author", smNewAddMySQLDependency()).
			addComment(newServiceWithMySQLDependenciesNotification),
		NewCase("new_map_dependency_mysql_reviewed", smNewAddMySQLDependency()).
			addApprovers("ibiryulin").
			addComment(newServiceWithMySQLDependenciesNotification),
		NewCase("update_map_add_dependency_by_author", smAddDependencyToMineService()).
			addComment(oldServiceWithNewDependenciesNotification),
		NewCase("update_map_add_dependency_reviewed", smAddDependencyToAnotherService()).
			setUser("goodfella").
			addApprovers("ibiryulin").
			addComment(oldPersonalServiceWithNewDependenciesNotification),
		NewCase("new_map_add_dependency_with_multiple_owners_group_approver", smNewAddDependencyOnMultipleOwners()).
			setUser("swapster").
			addApprovers("ibiryulin").
			addComment(newServiceWithDependenciesOnMultipleOwnersNotification),
		NewCase("new_map_add_dependency_with_multiple_owners_user_approver", smNewAddDependencyOnMultipleOwners()).
			addApprovers("swapster").
			addComment(newServiceWithDependenciesOnMultipleOwnersNotification),
		NewCase("check_geobase", smNew(), mNewWithGeobase()),
		NewCase("delete_map", smDeleted("delete-me.yml")),
		NewCase("delete_manifest", mDeleted("delete-me.yml")),
		NewCase("delete_both", smDeleted("old-service.yml"), mDeleted("old-service.yml")),
		NewCase("smWithLanguage", smNewWithLanguage()),
		NewCase("smLanguageEmpty", smNewWithLanguageEmpty()),
		NewCase("validTemplate", smNewWithValidTemplate()),
		NewCase("new_kafka_topic", smNewKafkaTopic()),
		NewCase("update_kafka_topic", smUpdateKafkaTopic()),
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			test.RunUpGhApp(t)
			staffApi := staffapi.NewApi(staffapi.NewConf(), test.NewLogger(t))
			arcanumMock := newArcanumMock()
			service := RunUp(t, staffApi, arcanumMock, c.file)
			result, err := service.validate(context.TODO(), &events.PrCtx{
				Author:    c.user,
				Approvers: c.approvers,
				Reviewers: c.reviewers,
			})
			require.NoError(t, err)
			printErrors(t, result)
			assert.False(t, result.CheckError())
			assert.False(t, result.CheckWarn())
			assertComment("", t, service, c, result)
			assertMessage(t, service, c, result)
			assertReviewers(t, c.requestedReviewers, arcanumMock.requestedReviewers[0])
		})
	}
}

func TestWarnValidate(t *testing.T) {
	cases := []*TestCase{
		NewCase("env", smNew(), mNewWithEnv()).
			addComment(newServiceNotification).
			addMsg(warnLegacyEnv),
		NewCase("conf", smNew(), mNewWithConf()).
			addComment(newServiceNotification).
			addMsg(warnLegacyConf),
		NewCase("extra_field", mExtraField()).
			addMsg(ymlExtraField),
		NewCase("mysql_type", smOldMySQLType()).
			addMsg(mySQLType),
		NewCase("mdb_mysql_field", smOldMDBMySQLField()).
			addMsg(mdbMySQLField),
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			test.RunUpGhApp(t)
			staffApi := staffapi.NewApi(staffapi.NewConf(), test.NewLogger(t))
			arcanumMock := newArcanumMock()
			service := RunUp(t, staffApi, arcanumMock, c.file)
			validate, err := service.validate(context.TODO(), &events.PrCtx{
				Author:    c.user,
				Approvers: c.approvers,
				Reviewers: c.reviewers,
			})
			require.NoError(t, err)
			assert.False(t, validate.CheckError())
			assert.True(t, validate.CheckWarn())
			printErrors(t, validate)
			assertMessage(t, service, c, validate)
			assertComment("", t, service, c, validate)
			assertReviewers(t, c.requestedReviewers, arcanumMock.requestedReviewers[0])
		})
	}
}

func TestFailValidate(t *testing.T) {
	cases := []*TestCase{
		NewCase("newManifestWithoutInclude", smNew(), mNewWitNewFile()).
			addMsg(newManifestWithoutIncludeMessage),
		NewCase("updateCollisionOldInclude", inclUpdateProdCollision()).
			addMsg(updateCollisionOldIncludeMessage),
		NewCase("newCollisionByManifestNewInclude", smNew(), mNewWitNewFile(), inclNewCollision()).
			addMsg(newCollisionByManifestNewInclude),
		NewCase("collisionOldManifestUpdateIncludes", m2UpdateAddCommonCollision()).
			addMsg(collisionOldManifestUpdateIncludesMessage),
		NewCase("allBlockFail", inclNewParseFail()).
			addMsg(allBlockFailMessage),
		NewCase("new_map_add_invalid_mysql_owners", smNewAddInvalidMySQLOwners()).
			addMsg(smInvalidMySQLOwners),
		NewCase("new_map_add_invalid_mysql_mdb_cluster", smNewAddInvalidMySQLMDBCluster()).
			addMsg(smInvalidMDBCluster),
		NewCase("new_map_add_invalid_mysql_dependency", smNewAddInvalidMySQLDependency()).
			addMsg(smInvalidMySQLDependency),
		NewCase("new_map_add_mysql_dependency_invalid_interface", smNewAddMySQLDependencyInvalidInterface()).
			setUser("swapster").
			addRequestedReviewers("ibiryulin").
			addMsg(smInvalidMySQLDependencyInterface),
		NewCase("new_map_add_dependency_not_reviewed", smNewMapWithDependencyToAnotherService()).
			setUser("swapster").
			addRequestedReviewers("ibiryulin").
			addMsg(smNewDependencyReviewNeededMessage),
		NewCase("update_map_add_dependency_not_reviewed", smAddDependencyToAnotherService()).
			setUser("goodfella").
			addRequestedReviewers("ibiryulin").
			addMsg(smOldDependencyReviewNeededMessage),
		NewCase("new_map_add_dependency_with_multiple_owners_not_reviewed", smNewAddDependencyOnMultipleOwners()).
			setUser("swapster").
			addRequestedReviewers("ibiryulin", "dshtan").
			addMsg(smNewDependencyOnMultipleOwnersReviewNeededMessage),
		NewCase("new_map_add_dependency_with_multiple_owners_not_reviewed_with_pending", smNewAddDependencyOnMultipleOwners()).
			setUser("swapster").
			addReviewers("dshtan").
			addApprovers("openyshev").
			addRequestedReviewers("dshtan", "ibiryulin").
			addMsg(smNewDependencyOnMultipleOwnersReviewNeededMessage),
		NewCase("dependent_map_error", smNewAddDependencyOnInvalidService()).
			addMsg(dependentMapOwnerNotFoundMessage),
		NewCase("failDelegate", mUpdateWithSec2Config()).
			addMsg(failDelegateMessage),
		NewCase("failServiceAddressTemplate", mUpdateWithTemplate1Config()).
			addMsg(unknownServiceAddressTemplateMessage),
		NewCase("failUnknownServiceTvmTemplate", mUpdateWithTemplate2Config()).
			addMsg(unknownServiceTvmTemplateMessage),
		NewCase("failProviderAddressTemplate", mUpdateWithProviderTemplateConfig()).
			addMsg(unknownProviderServiceAddressTemplateMessage),
		NewCase("failServiceTvmTemplate", mUpdateWithTemplate3Config()).
			addMsg(invalidServiceAddressTemplateMessage),
		NewCase("badQueue", smNewWithBadSTQueue()).
			addMsg(badQueueFailMessage),
		NewCase("badGeobase", smNew(), mNewWithBadGeobaseVersion()).
			addMsg(badGeobaseFailMessage),
		NewCase("usedEnvAndConfig", mNewWithEnvAndConfig()).
			addMsg(confOrEnvWithConfig),
		NewCase("usedConfAndConfig", mNewWithConfAndConfig()).
			addMsg(confOrEnvWithConfig),
		NewCase("multipleDocLinks", mNewWithConfAndConfig()).
			addMsg(confOrEnvWithConfig),
		NewCase("dependsOnCheck", smDeleted("depends-test-service.yml")).
			addMsg(deleteWithDependings),
		NewCase("delete_map_running", smDeleted("delete-not-stopped.yml")).
			addMsg(stopForMap),
		NewCase("delete_manifest_running", smDeleted("delete-not-stopped.yml")).
			addMsg(stopForManifest),
		NewCase("delete_map_without_manifest", smDeleted("old-service.yml")).
			addMsg(deleteWithoutManifest),
		NewCase("map_transfer_to_another_owner", smOwnerChanged()).
			addRequestedReviewers("swapster", "makarchuk-aa").
			addMsg(mapTransferToAnotherOwnerMessage),
		NewCase("map_stole_by_another_owner", smOwnerChanged()).
			setUser("swapster").
			addRequestedReviewers("ibiryulin", "makarchuk-aa").
			addMsg(mapStoleByAnotherOwnerMessage),
		NewCase("map_changed_by_third_person", smOwnerChanged()).
			setUser("m8rge").
			// TODO double add user like error
			addRequestedReviewers("ibiryulin", "swapster", "makarchuk-aa", "makarchuk-aa").
			addMsg(mapChangedByThirdPersonMessage),
		NewCase("service_map_yaml", smYaml()).addMsg(yamlServiceMap),
		NewCase("manifest_yaml", mYaml()).addMsg(yamlManifest),
		NewCase("include_yaml", inclYaml()).addMsg(yamlInclude),
		NewCase("includeYmlFormat", incWrongFormat()).addMsg(incorrectYamlInclude),
		NewCase("manifestYmlFormat", mWrongFormat()).addMsg(incorrectYmlManifest),
		NewCase("sMapYmlFormat", smWrongFormat()).addMsg(incorrectYmlSMap),
		NewCase("notDefinedFormat", incNotDefinedFormat()).addMsg(notDefinedFormat),
		NewCase("wrongOwnersInDependentMap", smNewAddDependencyFromWrongOwnersService()).
			addMsg(dependentMapOwnersError),
		NewCase("smWithLanguage", smNewWithLanguageUnknown()).addMsg(languageUnknownError),
		NewCase("owner", smUpdateToLegacyOwner()).addMsg(legacyOwnerFailMessage),
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			test.RunUpGhApp(t)
			staffApi := staffapi.NewApi(staffapi.NewConf(), test.NewLogger(t))
			arcanumMock := newArcanumMock()
			service := RunUp(t, staffApi, arcanumMock, c.file)
			validate, err := service.validate(context.TODO(), &events.PrCtx{
				Author:    c.user,
				Approvers: c.approvers,
				Reviewers: c.reviewers,
			})
			require.NoError(t, err)
			assert.True(t, validate.CheckError())
			assert.False(t, validate.CheckWarn())
			printErrors(t, validate)
			assertMessage(t, service, c, validate)
			assertReviewers(t, c.requestedReviewers, arcanumMock.requestedReviewers[0])
		})
	}
}

func TestMapTransferToTeamWithoutLeader(t *testing.T) {
	c := NewCase("", smOwnerChangedToTeamWithoutLeader()).
		addRequestedReviewers("coderoc").
		addMsg(mapTransferToAnotherOwnerMessage)

	test.RunUpGhApp(t)

	staffApiMock := staffMock.NewApiMock()
	arcanumMock := newArcanumMock()
	service := RunUp(t, staffApiMock, arcanumMock, c.file)
	validate, err := service.validate(context.TODO(), &events.PrCtx{
		Author:    c.user,
		Approvers: c.approvers,
		Reviewers: c.reviewers,
	})
	require.NoError(t, err)
	assert.True(t, validate.CheckError())
	assert.False(t, validate.CheckWarn())
	printErrors(t, validate)
	assertMessage(t, service, c, validate)
	assertReviewers(t, c.requestedReviewers, arcanumMock.requestedReviewers[0])
}

func TestWarnAndFailValidate(t *testing.T) {
	cases := []*TestCase{
		NewCase("error_and_warning", errorAndWarning()).
			addMsg(errorAndWarningText),
	}

	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			test.RunUpGhApp(t)
			staffApi := staffapi.NewApi(staffapi.NewConf(), test.NewLogger(t))
			arcanumMock := newArcanumMock()
			service := RunUp(t, staffApi, arcanumMock, c.file)
			validate, err := service.validate(context.TODO(), &events.PrCtx{
				Author:    c.user,
				Approvers: c.approvers,
				Reviewers: c.reviewers,
			})
			require.NoError(t, err)
			assert.True(t, validate.CheckError())
			assert.True(t, validate.CheckWarn())
			printErrors(t, validate)
			assertMessage(t, service, c, validate)
			assertReviewers(t, c.requestedReviewers, arcanumMock.requestedReviewers[0])
		})
	}
}

func TestHandlePr(t *testing.T) {
	testCases := []*TestCase{
		NewCase("new_service_success", smNew(), mNewWithConfig()).addStatus(validation.Status_SUCCESS).
			addMsg(allRight + "\n").addComment(newServiceNotification),
		NewCase("new_service_fail", smNew(), mNewWitNewFile()).addStatus(validation.Status_FAIL).
			addMsg(newManifestWithoutIncludeMessage),
		NewCase("delete_service_success", smDeleted("old-service.yml"), mDeleted("old-service.yml")).
			addStatus(validation.Status_SUCCESS).addMsg(allRight + "\n"),
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			test.RunUpGhApp(t)
			staffApi := staffapi.NewApi(staffapi.NewConf(), test.NewLogger(t))
			arcanumMock := newArcanumMock()
			service := RunUp(t, staffApi, arcanumMock, tc.file)
			arcanumMock.On("Comment", uint32(0), tc.message).Return(nil).Once()
			arcanumMock.On("Comment", uint32(0), tc.comment).Return(nil).Once()

			pr := &events.PrCtx{
				Author:    tc.user,
				Approvers: tc.approvers,
				Reviewers: tc.reviewers,
			}
			require.NoError(t, service.HandlePR(pr))

			arcadiaPr, err := service.storage.GetByPrIDAndVersion(0, 0)
			require.NoError(t, err)
			require.Equal(t, tc.status, arcadiaPr.Status)
			require.Equal(t, pr.UpdatedAt.UnixMilli(), arcadiaPr.Time.UnixMilli())
			assertReviewers(t, tc.requestedReviewers, arcanumMock.requestedReviewers[0])
			arcanumMock.AssertExpectations(t)
		})
	}
}

func TestHandlePRSkipDuplicateMsg(t *testing.T) {
	c := NewCase("new service", smNew(), mNewWithConfig())

	test.RunUpGhApp(t)
	staffApi := staffapi.NewApi(staffapi.NewConf(), test.NewLogger(t))
	arcanumMock := newArcanumMock()
	service := RunUp(t, staffApi, nil, c.file)
	arcanumMock.On("GetPrInfo", uint32(0)).Return(&events.PrCtx{
		Author: c.user,
	}, nil)
	arcanumMock.On("Comment", uint32(0), mock.Anything).Return(nil)
	service.ArcanumService = arcanumMock

	pr := &events.PrCtx{
		UpdatedAt: time.Now(),
		Author:    c.user,
	}
	require.NoError(t, service.HandlePR(pr))
	assertReviewers(t, c.requestedReviewers, arcanumMock.requestedReviewers[0])

	_, err := service.storage.GetByPrIDAndVersion(0, 0)
	require.NoError(t, err)

	service.ArcanumService = nil
	require.NoError(t, service.HandlePR(pr))
}

func RunUp(t *testing.T, staffApi staffapi.IClient, arcanumMock arcanum.IService, files []*file.File) *Service {
	db := test_db.NewSeparatedDb(t)
	log := test.NewLogger(t)

	nomadConf := nomadAPI.DefaultConfig()
	nomadConf.Address = config.Str("NOMAD_ENDPOINT")

	nomadClient, err := nomadAPI.NewClient(nomadConf)
	if err != nil {
		log.WithError(err).Fatalf("Can't create new client")
	}

	includeService := include.NewService(db, log)
	mapService := service_map.NewService(db, log, service_change.NewNotificationMock())
	envService := reader.NewService(db, log)
	staffService := staff.NewService(db, staffApi, log)
	mdbService := new(testMock.MdbService)
	mdbService.On("GetCluster", mock.Anything, "mdb000000000").Return(&mdb.Cluster{Environment: mdb.Prestable, Name: "new-mdb-test"}, nil)
	mdbService.On("GetCluster", mock.Anything, "mdb111111111").Return(&mdb.Cluster{Environment: mdb.Production, Name: "new-mdb-prod"}, nil)
	mdbService.On("GetCluster", mock.Anything, "mdb000000001").Return(&mdb.Cluster{Environment: mdb.Prestable, Name: "shared-01-test"}, nil)
	mdbService.On("GetCluster", mock.Anything, "mdb111111110").Return(&mdb.Cluster{Environment: mdb.Production, Name: "shared-01-prod"}, nil)
	mdbService.On("GetCluster", mock.Anything, "mdb888888888").Return(nil, status.Errorf(codes.PermissionDenied, "cluster not found"))
	mdbService.On("GetCluster", mock.Anything, "mdb999999999").Return(&mdb.Cluster{Environment: mdb.Production, Name: "new-mdb-neprod"}, nil)
	featureFlagsService := feature_flags.NewService(db, mqMock.NewProducerMock(), log)
	mapValidator := sMapValidator.NewService(mapService, mdbService, staffService, nomadClient, *conductor.NewApi(log), featureFlagsService, log)
	mParser := manifestParser.NewService(log, nil)
	mService := manifest.NewService(db, log, mParser, includeService)
	ssMock := testMock.NewAccessClientMock(t)
	ssMock.Add(newService, "sec-2", "ver-2", nil, false)
	ssMock.Add(oldService, "sec-2", "ver-2",
		&error1.UserError{RuMessage: "Для сервиса 'old-service' не делегирован секрет 'sec-2'", Docs: []string{secrets.DocsUrl}}, false)
	ssMock.Add(oldService, "sec-3", "ver-3", nil, true, "MY_SECRET_3")
	secretSvc := secrets.NewService(secrets.NewConf(0), ssMock, log)
	mockShiva := new(mockShivaCli)
	mockShiva.On("Status", mock.Anything, &deploy.StatusRequest{Service: "depends-service"}).Return(&deploy.StatusResponse{}, nil)
	mockShiva.On("Status", mock.Anything, &deploy.StatusRequest{Service: "delete-me"}).Return(&deploy.StatusResponse{}, nil)
	mockShiva.On("Status", mock.Anything, &deploy.StatusRequest{Service: "old-service"}).Return(&deploy.StatusResponse{}, nil)
	mockShiva.On("Status", mock.Anything, &deploy.StatusRequest{Service: "depends-test-service"}).Return(&deploy.StatusResponse{}, nil)
	mockShiva.On("Status", mock.Anything, &deploy.StatusRequest{Service: "delete-not-stopped"}).Return(&deploy.StatusResponse{
		Info: []*deploy.StatusResponse_Info{{Service: "delete-not-stopped"}},
	}, nil)
	diffServiceMock := &testMock.ArcDiffService{}
	diffServiceMock.On("ChangeList", mock.Anything).Return(files, nil)

	commitMock := &testMock.ArcCommitService{}
	commitMock.On("GetCommitOid", "trunk").Return("trunk-oid", nil)
	commitMock.On("GetCommitOid", mock.Anything).Return("some-oid", nil)

	RunUpDB(t, includeService, mapService, mService)

	return (&Service{
		Conf:              NewConf(),
		IncludeSrv:        includeService,
		IncludeParser:     confParser.NewService(log),
		IncludeValidator:  confValidator.NewService(log),
		SMapService:       mapService,
		SMapParser:        sMapParser.NewService(),
		SMapValidator:     mapValidator,
		ManifestService:   mService,
		ManifestParser:    mParser,
		ManifestValidator: manifestValidator.NewService(mapService, secretSvc, template.NewService(mapService, envService), log),
		StaffService:      staffService,
		ShivaCli:          mockShiva,

		ArcanumService: arcanumMock,
		CommitService:  commitMock,
		DiffService:    diffServiceMock,
		Log:            log,
		DB:             db,
	}).Init()
}

func printErrors(t *testing.T, blocks *validateBlocks) {
	for _, b := range blocks.serviceMapBlock {
		test.PrintUserErrors(t, "map block error: "+b.Name(), b.Errors)
		test.PrintUserErrors(t, "map block warn: "+b.Name(), b.Warns)
	}

	for _, b := range blocks.manifestBlock {
		test.PrintUserErrors(t, "manifest block errors: "+b.Name, b.Errors)
		test.PrintUserErrors(t, "manifest block warns: "+b.Name, b.Warns)
		if b.Test != nil {
			test.PrintUserErrors(t, "test manifest block errors: "+b.Test.Name(), b.Test.Errors)
			test.PrintUserErrors(t, "test manifest block warns: "+b.Test.Name(), b.Test.Warns)
		}
		if b.Prod != nil {
			test.PrintUserErrors(t, "prod manifest block errors: "+b.Prod.Name(), b.Prod.Errors)
			test.PrintUserErrors(t, "prod manifest block warns: "+b.Prod.Name(), b.Prod.Warns)
		}
	}

	for _, b := range blocks.serviceMapBlock {
		test.PrintUserErrors(t, "map block errors: "+b.Name(), b.Errors)
		test.PrintUserErrors(t, "map block warns: "+b.Name(), b.Warns)
	}

	for _, b := range blocks.includeBlock {
		test.PrintUserErrors(t, "include block errors: "+b.Include.Path, b.Errors)
		test.PrintUserErrors(t, "include block warns: "+b.Include.Path, b.Warns)
	}
	for _, b := range blocks.sideBlock {
		test.PrintUserErrors(t, "side block errors: "+b.Manifest.Name, b.Errors)
		test.PrintUserErrors(t, "side block warns: "+b.Manifest.Name, b.Warns)
	}
}

func RunUpDB(t *testing.T, includeSrv *include.Service, mapSrv *service_map.Service, mSrv *manifest.Service) {
	makeInclude(t, includeSrv, commonYml, commonYmlPath)
	makeInclude(t, includeSrv, common2ml, common2YmlPath)
	makeInclude(t, includeSrv, prodYml, prodYmlPath)
	makeInclude(t, includeSrv, testYml, testYmlPath)
	makeInclude(t, includeSrv, sec1Yml, sec1YmlPath)
	makeInclude(t, includeSrv, sec2Yml, sec2YmlPath)
	makeInclude(t, includeSrv, template1Yml, template1YmlPath)
	makeInclude(t, includeSrv, template2Yml, template2YmlPath)
	makeInclude(t, includeSrv, template3Yml, template3YmlPath)
	makeInclude(t, includeSrv, template4Yml, template4YmlPath)
	makeInclude(t, includeSrv, templateValidYml, templateValidYmlPath)
	makeMap(t, mapSrv, oldServiceMap, config.MapBasePath+"/old-service.yml")
	makeMap(t, mapSrv, oldServiceMap2, config.MapBasePath+"/old-service2.yml")
	makeMap(t, mapSrv, oldServiceMap3, config.MapBasePath+"/old-service3.yml")
	makeMap(t, mapSrv, oldServiceMapWrongOwners, config.MapBasePath+"/old-service-wrong-owners.yml")
	makeMap(t, mapSrv, oldServiceMapMySQL, config.MapBasePath+"/mysql/old-mysql.yml")
	makeMap(t, mapSrv, oldPersonalServiceMap, config.MapBasePath+"/old-personal-service.yml")
	makeMap(t, mapSrv, invalidMap, config.MapBasePath+"/invalid-service.yml")
	makeMap(t, mapSrv, shivaTgMap, config.MapBasePath+"/shiva-tg.yml")
	makeManifest(t, mSrv, oldManifest, config.ManifestBasePath+"/old-service.yml")
	makeManifest(t, mSrv, oldManifest2, config.ManifestBasePath+"/old-service2.yml")
	makeMap(t, mapSrv, dependsMap, config.ManifestBasePath+"/depends-service.yml")
	makeMap(t, mapSrv, dependsTestMap, config.ManifestBasePath+"/depends-test-service.yml")
}

func makeInclude(t *testing.T, s *include.Service, body, path string) {
	require.NoError(t, s.ReadAndSave([]byte(body), 10, path))
}

func makeMap(t *testing.T, mSrv *service_map.Service, body, path string) {
	require.NoError(t, mSrv.ReadAndSave([]byte(body), 10, path))
}

func makeManifest(t *testing.T, mSrv *manifest.Service, body, path string) {
	require.NoError(t, mSrv.ReadAndSave([]byte(body), 10, path))
}

type TestCase struct {
	name               string
	file               []*file.File
	mapErrors          map[string]*user_error.UserErrors
	manifestErrors     map[string]*user_error.UserErrors
	testManifestErrors map[string]*user_error.UserErrors
	prodManifestErrors map[string]*user_error.UserErrors
	sideErrors         map[string]*user_error.UserErrors
	includeErrors      map[string]*user_error.UserErrors
	message            string
	user               string
	approvers          []string
	reviewers          []string
	requestedReviewers []string
	comment            string
	status             validation.Status
}

func NewCase(name string, file ...*file.File) *TestCase {
	return &TestCase{
		name:               name,
		user:               "ibiryulin",
		file:               file,
		mapErrors:          map[string]*user_error.UserErrors{},
		manifestErrors:     map[string]*user_error.UserErrors{},
		testManifestErrors: map[string]*user_error.UserErrors{},
		prodManifestErrors: map[string]*user_error.UserErrors{},
		sideErrors:         map[string]*user_error.UserErrors{},
		includeErrors:      map[string]*user_error.UserErrors{},
	}
}

func (tc *TestCase) setUser(user string) *TestCase {
	tc.user = user
	return tc
}

func (tc *TestCase) addApprovers(logins ...string) *TestCase {
	tc.approvers = append(tc.approvers, logins...)
	return tc
}

func (tc *TestCase) addReviewers(logins ...string) *TestCase {
	tc.reviewers = append(tc.reviewers, logins...)
	return tc
}

func (tc *TestCase) addRequestedReviewers(users ...string) *TestCase {
	tc.requestedReviewers = append(tc.requestedReviewers, users...)
	return tc
}

func (tc *TestCase) addComment(comment string) *TestCase {
	tc.comment = comment
	return tc
}

func (tc *TestCase) addMsg(message string) *TestCase {
	tc.message = message
	return tc
}

func (tc *TestCase) addStatus(st validation.Status) *TestCase {
	tc.status = st
	return tc
}

func (tc *TestCase) add(errMap map[string]*user_error.UserErrors, name string, userErrors []string) *TestCase {
	block, ok := errMap[name]
	if !ok {
		block = user_error.NewUserErrors()
	}
	for _, e := range userErrors {
		block.AddError(user_error.NewUserError(fmt.Errorf("empty"), e))
	}
	errMap[name] = block
	return tc
}

func assertMessage(t *testing.T, s *Service, tc *TestCase, validate *validateBlocks) {
	t.Helper()
	if tc.message == "" {
		return
	}
	result, err := s.buildMessage(validate)
	require.NoError(t, err)
	assert.Equal(t, tc.message, result)
}

func assertComment(branch string, t *testing.T, s *Service, tc *TestCase, validate *validateBlocks) {
	if tc.comment == "" {
		return
	}
	result, err := s.buildSecurityNotificationMessage(branch, validate)
	require.NoError(t, err)
	assert.Equal(t, tc.comment, result)
}

func assertReviewers(t *testing.T, expected []string, actual []string) {
	assert.ElementsMatch(t, expected, actual, "reviewers are not match")
}

func sha(body string) string {
	hasher := sha1.New()
	hasher.Write([]byte(body))
	return base64.URLEncoding.EncodeToString(hasher.Sum(nil))
}

type arcanumMock struct {
	*testMock.ArcanumService

	requestedReviewers map[uint32][]string
}

func newArcanumMock() *arcanumMock {
	return &arcanumMock{
		requestedReviewers: map[uint32][]string{},
		ArcanumService:     &testMock.ArcanumService{},
	}
}

func (a *arcanumMock) PutReviewers(id uint32, logins []string) error {
	a.requestedReviewers[id] = append(a.requestedReviewers[id], logins...)
	return nil
}

type mockShivaCli struct {
	mock.Mock
}

func (m *mockShivaCli) Envs(ctx context.Context, in *deploy.EnvsRequest, opts ...grpc.CallOption) (*deploy.EnvsResponse, error) {
	panic("implement me")
}

func (m *mockShivaCli) Settings(ctx context.Context, in *deploy.SettingsRequest, opts ...grpc.CallOption) (*flags.Flags, error) {
	panic("implement me")
}

func (m *mockShivaCli) Run(ctx context.Context, in *deploy.RunRequest, opts ...grpc.CallOption) (deploy.DeployService_RunClient, error) {
	panic("implement me")
}

func (m *mockShivaCli) AsyncRun(ctx context.Context, in *deploy.RunRequest, opts ...grpc.CallOption) (*deploy.AsyncResponse, error) {
	panic("implement me")
}

func (m *mockShivaCli) Stop(ctx context.Context, in *deploy.StopRequest, opts ...grpc.CallOption) (deploy.DeployService_StopClient, error) {
	panic("implement me")
}

func (m *mockShivaCli) Restart(ctx context.Context, in *deploy.RestartRequest, opts ...grpc.CallOption) (deploy.DeployService_RestartClient, error) {
	panic("implement me")
}

func (m *mockShivaCli) Revert(ctx context.Context, in *deploy.RevertRequest, opts ...grpc.CallOption) (deploy.DeployService_RevertClient, error) {
	panic("implement me")
}

func (m *mockShivaCli) State(ctx context.Context, in *deploy.StateRequest, opts ...grpc.CallOption) (deploy.DeployService_StateClient, error) {
	panic("implement me")
}

func (m *mockShivaCli) Cancel(ctx context.Context, in *deploy.CancelRequest, opts ...grpc.CallOption) (deploy.DeployService_CancelClient, error) {
	panic("implement me")
}

func (m *mockShivaCli) Promote(ctx context.Context, in *deploy.PromoteRequest, opts ...grpc.CallOption) (deploy.DeployService_PromoteClient, error) {
	panic("implement me")
}

func (m *mockShivaCli) Approve(ctx context.Context, in *deploy.ApproveRequest, opts ...grpc.CallOption) (deploy.DeployService_ApproveClient, error) {
	panic("implement me")
}

func (m *mockShivaCli) ApproveList(ctx context.Context, in *deploy.ApproveListRequest, opts ...grpc.CallOption) (*deploy.ApproveListResponse, error) {
	panic("implement me")
}

func (m *mockShivaCli) Status(ctx context.Context, in *deploy.StatusRequest, opts ...grpc.CallOption) (*deploy.StatusResponse, error) {
	args := m.Called(ctx, in)
	if v, ok := args.Get(0).(*deploy.StatusResponse); ok {
		return v, args.Error(1)
	}
	return nil, args.Error(1)
}

func (m *mockShivaCli) AllStatus(ctx context.Context, in *deploy.AllStatusRequest, opts ...grpc.CallOption) (*deploy.StatusResponse, error) {
	panic("implement me")
}

func (m *mockShivaCli) BalancerStatus(ctx context.Context, in *deploy.BalancerStatusRequest, opts ...grpc.CallOption) (*deploy.BalancerStatusResponse, error) {
	panic("implement me")
}

func (m *mockShivaCli) ReleaseHistory(ctx context.Context, in *deploy.ReleaseHistoryRequest, opts ...grpc.CallOption) (*deploy.ReleaseHistoryResponse, error) {
	panic("implement me")
}
