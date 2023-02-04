package yav

import (
	"testing"

	"github.com/YandexClassifieds/shiva/pkg/staff"
	staffapi "github.com/YandexClassifieds/shiva/pkg/staff/api"
	"github.com/YandexClassifieds/shiva/pkg/yav/client"
	"github.com/YandexClassifieds/shiva/test"
	mock2 "github.com/YandexClassifieds/shiva/test/mock"
	"github.com/YandexClassifieds/shiva/test/test_db"
	"github.com/stretchr/testify/require"
)

const (
	secID = "sec-01efxmtecv69t6zmqcn2gj8kd6"
)

func TestRewriteReaders(t *testing.T) {

	// prepare
	test.RunUp(t)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	clientMock := &mock2.YavClient{}
	s := NewService(clientMock, staffService, log)

	var (
		addUser    = "https://staff.yandex-team.ru/alexander-s"
		skipUser   = "https://staff.yandex-team.ru/spooner"
		deleteUser = "https://staff.yandex-team.ru/danevge"

		addGroup    = "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_interface_interfaces2"
		skipGroup   = "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"
		deleteGroup = "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_comp_infra_intproc"

		addGroupID    = GroupID(t, staffService, addGroup)
		skipGroupID   = GroupID(t, staffService, skipGroup)
		deleteGroupID = GroupID(t, staffService, deleteGroup)
	)

	// prepare clientMock
	clientMock.On("Readers", secID).Return(&client.ReadersInfo{
		Name: "test name",
		Readers: []*client.RoleHolderInfo{
			{
				RoleSlug: appenderRole,
				Login:    "appender login",
			},
			{
				RoleSlug: ownerRole,
				Login:    "owner login",
			},
			{
				RoleSlug: readerRole,
				Login:    staffapi.ExtractName(skipUser),
			},
			{
				RoleSlug: readerRole,
				Login:    staffapi.ExtractName(deleteUser),
			},
			{
				RoleSlug:  readerRole,
				StaffID:   skipGroupID,
				StaffSlug: staffapi.ExtractName(skipGroup),
				StaffUrl:  skipGroup,
			},
			{
				RoleSlug:  readerRole,
				StaffID:   deleteGroupID,
				StaffSlug: staffapi.ExtractName(deleteGroup),
				StaffUrl:  deleteGroup,
			},
		},
	}, nil)
	clientMock.On("AddUserRole", secID, staffapi.ExtractName(addUser), readerRole).Return(nil)
	clientMock.On("AddGroupRole", secID, addGroupID, readerRole).Return(nil)
	clientMock.On("DeleteUserRole", secID, staffapi.ExtractName(deleteUser), readerRole).Return(nil)
	clientMock.On("DeleteGroupRole", secID, deleteGroupID, readerRole).Return(nil)

	// test
	require.NoError(t, s.RewriteReaders(secID, []string{addUser, addGroup, skipUser, skipGroup}))

	// asserts
	clientMock.AssertNumberOfCalls(t, "AddUserRole", 1)
	clientMock.AssertNumberOfCalls(t, "AddGroupRole", 1)
	clientMock.AssertNumberOfCalls(t, "DeleteUserRole", 1)
	clientMock.AssertNumberOfCalls(t, "DeleteGroupRole", 1)
}

func TestRewriteOwners(t *testing.T) {

	// prepare
	test.RunUp(t)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	clientMock := &mock2.YavClient{}
	s := NewService(clientMock, staffService, log)

	var (
		addUser    = "https://staff.yandex-team.ru/alexander-s"
		skipUser   = "https://staff.yandex-team.ru/spooner"
		deleteUser = "https://staff.yandex-team.ru/danevge"

		addGroup    = "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_interface_interfaces2"
		skipGroup   = "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"
		deleteGroup = "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_comp_infra_intproc"

		addGroupID    = GroupID(t, staffService, addGroup)
		skipGroupID   = GroupID(t, staffService, skipGroup)
		deleteGroupID = GroupID(t, staffService, deleteGroup)
	)

	// prepare clientMock
	clientMock.On("Owners", secID).Return(&client.OwnersInfo{
		Name: "test name",
		Owners: []*client.RoleHolderInfo{
			{
				RoleSlug: appenderRole,
				Login:    "appender login",
			},
			{
				RoleSlug: readerRole,
				Login:    "reader login",
			},
			{
				RoleSlug: ownerRole,
				Login:    staffapi.ExtractName(skipUser),
			},
			{
				RoleSlug: ownerRole,
				Login:    staffapi.ExtractName(deleteUser),
			},
			{
				RoleSlug:  ownerRole,
				StaffID:   skipGroupID,
				StaffSlug: staffapi.ExtractName(skipGroup),
				StaffUrl:  skipGroup,
			},
			{
				RoleSlug:  ownerRole,
				StaffID:   deleteGroupID,
				StaffSlug: staffapi.ExtractName(deleteGroup),
				StaffUrl:  deleteGroup,
			},
		},
	}, nil)
	clientMock.On("AddUserRole", secID, staffapi.ExtractName(addUser), ownerRole).Return(nil)
	clientMock.On("AddGroupRole", secID, addGroupID, ownerRole).Return(nil)
	clientMock.On("DeleteUserRole", secID, staffapi.ExtractName(deleteUser), ownerRole).Return(nil)
	clientMock.On("DeleteGroupRole", secID, deleteGroupID, ownerRole).Return(nil)

	// test
	require.NoError(t, s.RewriteOwners(secID, []string{addUser, addGroup, skipUser, skipGroup}))

	// asserts
	clientMock.AssertNumberOfCalls(t, "AddUserRole", 1)
	clientMock.AssertNumberOfCalls(t, "AddGroupRole", 1)
	clientMock.AssertNumberOfCalls(t, "DeleteUserRole", 1)
	clientMock.AssertNumberOfCalls(t, "DeleteGroupRole", 1)
}

func TestRewriteRole(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	clientMock := &mock2.YavClient{}
	s := NewService(clientMock, staffService, log)

	var (
		addUser    = "https://staff.yandex-team.ru/alexander-s"
		skipUser   = "https://staff.yandex-team.ru/spooner"
		deleteUser = "https://staff.yandex-team.ru/danevge"

		addGroup    = "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_interface_interfaces2"
		skipGroup   = "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_infra_mnt/"
		deleteGroup = "https://staff.yandex-team.ru/departments/yandex_personal_vertserv_comp_infra_intproc"

		addGroupID    = GroupID(t, staffService, addGroup)
		skipGroupID   = GroupID(t, staffService, skipGroup)
		deleteGroupID = GroupID(t, staffService, deleteGroup)
	)

	hi := []*client.RoleHolderInfo{
		{
			RoleSlug: appenderRole,
			Login:    "appender login",
		},
		{
			RoleSlug: readerRole,
			Login:    "reader login",
		},
		{
			RoleSlug: ownerRole,
			Login:    staffapi.ExtractName(skipUser),
		},
		{
			RoleSlug: ownerRole,
			Login:    staffapi.ExtractName(deleteUser),
		},
		{
			RoleSlug:  ownerRole,
			StaffID:   skipGroupID,
			StaffSlug: staffapi.ExtractName(skipGroup),
			StaffUrl:  skipGroup,
		},
		{
			RoleSlug:  ownerRole,
			StaffID:   deleteGroupID,
			StaffSlug: staffapi.ExtractName(deleteGroup),
			StaffUrl:  deleteGroup,
		},
	}
	clientMock.On("AddUserRole", secID, staffapi.ExtractName(addUser), ownerRole).Return(nil)
	clientMock.On("AddGroupRole", secID, addGroupID, ownerRole).Return(nil)
	clientMock.On("DeleteUserRole", secID, staffapi.ExtractName(deleteUser), ownerRole).Return(nil)
	clientMock.On("DeleteGroupRole", secID, deleteGroupID, ownerRole).Return(nil)

	err := s.(*service).rewriteRole(secID, []string{addUser, addGroup, skipUser, skipGroup}, hi, ownerRole)
	require.NoError(t, err)

	// asserts
	clientMock.AssertNumberOfCalls(t, "AddUserRole", 1)
	clientMock.AssertNumberOfCalls(t, "AddGroupRole", 1)
	clientMock.AssertNumberOfCalls(t, "DeleteUserRole", 1)
	clientMock.AssertNumberOfCalls(t, "DeleteGroupRole", 1)
}

func TestUpdateMetaInfo(t *testing.T) {
	test.RunUp(t)
	db := test_db.NewDb(t)
	log := test.NewLogger(t)
	staffApi := staffapi.NewApi(staffapi.NewConf(), log)
	staffService := staff.NewService(db, staffApi, log)
	clientMock := &mock2.YavClient{}
	s := NewService(clientMock, staffService, log)
	metaInfo := client.UpdateMetaInfoRequest{Name: "shiva-test"}

	clientMock.On("UpdateMetaInfo", secID, metaInfo).Return(nil)

	require.NoError(t, s.UpdateMetaInfo(secID, metaInfo))
}

func GroupID(t *testing.T, staffS *staff.Service, url string) int {
	name := staffapi.ExtractName(url)
	group, err := staffS.GetGroupByName(name)
	require.NoError(t, err)
	return group.ID
}
