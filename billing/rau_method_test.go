package impl

import (
	"context"
	_ "embed"
	"strconv"
	"strings"
	"testing"

	"github.com/golang/mock/gomock"
	"github.com/stretchr/testify/suite"
	"gopkg.in/yaml.v2"

	"a.yandex-team.ru/billing/configdepot/pkg/commands"
	"a.yandex-team.ru/billing/configdepot/pkg/core"
	"a.yandex-team.ru/billing/configdepot/pkg/interactions"
	"a.yandex-team.ru/billing/library/go/billingo/mock"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/common"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/interactions/sandbox/entities"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
)

const (
	testOwner    = "BILLING-TEST"
	path         = "test.inner"
	resourceType = "TEST_TYPE"
)

type RenderAndUploadMethodSuite struct {
	btesting.BaseSuite

	r *renderAndUpload

	uploaderMock *mock.MockUploader
	sandbox      *mock.MockSandboxClientProtocol

	repo commands.Repository
}

func (s *RenderAndUploadMethodSuite) SetupTest() {
	s.r = NewRenderAndUploadMethod().(*renderAndUpload)

	s.uploaderMock = mock.NewMockUploader(s.Ctrl())
	s.sandbox = mock.NewMockSandboxClientProtocol(s.Ctrl())

	s.repo = commands.Repository{
		Storage:        nil,
		ConfigdepotSQS: nil,
		Clients:        &interactions.Clients{SandboxUploader: s.uploaderMock, Sandbox: s.sandbox},
		Registry:       nil,
		Config:         &core.Config{},
	}
}

func (s *RenderAndUploadMethodSuite) TestValidTemplateRender() {

	s.r.params = RenderAndUploadParams{
		Target: Target{
			Dst: path,
		},
		Owner: testOwner,
		Template: Template{
			Explicit:      templateContents,
			SerializeType: rawSerializationType,
		},
		ResourceType: resourceType,
	}

	deployID := int(btesting.RandN64())
	resourceID := btesting.RandN64()
	paramsToAppend := common.NewSyncStringMap()

	contexts := []map[string]any{
		{
			"key":   "foo",
			"value": "first",
		},
		{
			"key":   "bar",
			"value": "second",
		},
	}

	validYamlMap := map[string]any{
		"foo": "first",
		"bar": "second",
	}

	// return empty resources, so that renderAndUpload creates new resources.
	s.sandbox.EXPECT().Resources(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Return([]*entities.Resource{}, nil)

	s.uploaderMock.EXPECT().
		StreamUpload(
			gomock.Any(),
			testOwner,
			resourceType,
			map[string]string{
				"configdepot": "true",
				"deploy_id":   strconv.Itoa(deployID),
			},
			gomock.Any(),
		).
		// validate YAML file
		Do(func(_ any, _ any, _ any, _ any, streams []entities.FileStream) {
			s.Require().Equal(1, len(streams))

			stream := streams[0]
			s.Assert().Equal(defaultResourceName, stream.Filename)

			unmarchaled := make(map[string]any)
			err := yaml.Unmarshal(stream.Contents, &unmarchaled)
			s.Require().NoError(err)
			s.Assert().Equal(validYamlMap, unmarchaled)
		}).Return(resourceID, nil)

	err := s.r.Do(context.Background(), s.repo, deployID, contexts, paramsToAppend)
	s.Require().NoError(err)

	// validate that we write correct resource to the params.
	savedResourceID, err := paramsToAppend.GetRecursive(strings.Split(path, ".")...)
	s.Require().NoError(err)

	s.Assert().Equal(resourceID, savedResourceID)
}

func (s *RenderAndUploadMethodSuite) TestResourceExists() {
	s.r.params = RenderAndUploadParams{
		Target: Target{
			Dst: path,
		},
		Owner: testOwner,
		Template: Template{
			Explicit:      templateContents,
			SerializeType: rawSerializationType,
		},
		ResourceType: resourceType,
	}

	deployID := int(btesting.RandN64())

	returnResource := &entities.Resource{
		ID: btesting.RandN64(),
	}

	paramsToAppend := common.NewSyncStringMap()

	contexts := []map[string]any{
		{
			"key": "foo",
		},
	}

	s.sandbox.EXPECT().Resources(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Return([]*entities.Resource{returnResource}, nil)

	err := s.r.Do(context.Background(), s.repo, deployID, contexts, paramsToAppend)

	s.Require().NoError(err)

	// validate that we write correct resource to the params.
	savedResourceID, err := paramsToAppend.GetRecursive(strings.Split(path, ".")...)
	s.Require().NoError(err)

	s.Assert().Equal(returnResource.ID, savedResourceID)
}

func (s *RenderAndUploadMethodSuite) TestInvalidTemplateRender() {

	s.r.params.Template = Template{
		Explicit:      templateContents,
		SerializeType: rawSerializationType,
	}

	deployID := int(btesting.RandN64())

	paramsToAppend := common.NewSyncStringMap()

	contexts := []map[string]any{
		{
			"key": "foo",
		},
	}

	// return empty resources, so that renderAndUpload creates new resources.
	s.sandbox.EXPECT().Resources(gomock.Any(), gomock.Any(), gomock.Any(), gomock.Any()).Return([]*entities.Resource{}, nil)

	err := s.r.Do(context.Background(), s.repo, deployID, contexts, paramsToAppend)
	s.Require().Error(err)
}

func TestRenderAndUploadMethod(t *testing.T) {
	suite.Run(t, new(RenderAndUploadMethodSuite))
}
