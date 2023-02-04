package yaml

import (
	"context"
	"io/fs"
	"path/filepath"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"gopkg.in/yaml.v2"

	"a.yandex-team.ru/billing/configshop/example"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/template"
	"a.yandex-team.ru/billing/configshop/pkg/core/entities/tplops"
	storagetest "a.yandex-team.ru/billing/configshop/pkg/storage/xtest"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/configops"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xyaml"
)

func TestTreeTemplateBlocks(t *testing.T) {
	addOp, err := configops.NewAdd([]any{
		"hello ", "$2.b2out", " world!",
	})
	require.NoError(t, err)

	root := &RootBlock{
		baseBlock: baseBlock{BlockName: "1"},
		Inputs: []GlobalInput{
			{YAMLValue: tplops.OutputValue{Name: "inp1"}},
			{YAMLValue: tplops.OutputValue{Name: "inp2"}},
		},
		Outputs: []GlobalOutput{
			{
				Name: "out1",
				Var: entities.VarName{
					Name:  "b2out",
					Block: "2",
				},
			},
		},
		Children: []Block{
			{&IntegrationBlock{
				baseInputBlock: baseInputBlock{
					Inputs: []entities.VarName{
						{Name: "inp1", Block: "1"},
					},
					Cacheable: new(bool),
					baseBlock: baseBlock{BlockName: "2"},
				},
				Outputs: []tplops.OutputValue{{Name: "b2out"}},
				Args: xyaml.MapStr{
					"kekw": "arg",
				},
			}},
			{&CalcBlock{
				baseInputBlock: baseInputBlock{
					Inputs: []entities.VarName{
						{Name: "b2out", Block: "2"},
					},
					baseBlock: baseBlock{BlockName: "4"},
				},
				Exprs: []Expression{
					{
						Var: "calcOut",
						Expr: configops.BaseTransformWithMarshal{
							BaseTransform: configops.BaseTransform{
								Transform: addOp,
							},
						},
					},
				},
			}},
			{&IntegrationBlock{
				baseInputBlock: baseInputBlock{
					Inputs: []entities.VarName{
						{Name: "calcOut", Block: "4"},
					},
					baseBlock: baseBlock{BlockName: "3"},
				},
				Outputs: []tplops.OutputValue{{Name: "b3out"}},
				Args:    nil,
			}},
		},
	}

	var tree BlockTree
	require.NoError(t, tree.validateAndPrepare(Block{root}))

	blocks := tree.TemplateBlocks()
	assert.ElementsMatch(t, blocks, []template.GraphBlock{
		template.IntegrationBlock{
			BaseBlock:   template.BaseBlock{BlockName: "2"},
			InputVars:   []entities.VarName{{Name: "inp1", Block: entities.GlobalInputBlockName}},
			OutputNames: []tplops.OutputValue{{Name: "b2out"}},
			Args: xyaml.MapStr{
				"kekw": "arg",
			},
		},
		template.CalcBlock{
			BaseBlock: template.BaseBlock{BlockName: "4", Cacheable: true},
			InputVars: []entities.VarName{{Name: "b2out", Block: "2"}},
			Exprs: []configops.BaseTransformWithMarshal{
				{
					BaseTransform: configops.BaseTransform{
						Transform: addOp,
					},
				},
			},
			OutputNames: []string{"calcOut"},
		},
		template.IntegrationBlock{
			BaseBlock:   template.BaseBlock{BlockName: "3", Cacheable: true},
			InputVars:   []entities.VarName{{Name: "calcOut", Block: "4"}},
			OutputNames: []tplops.OutputValue{{Name: "b3out"}},
			Args:        nil,
		},
		template.GlobalOutputBlock{
			BaseBlock:   template.BaseBlock{BlockName: "1"},
			InputVars:   []entities.VarName{{Name: "b2out", Block: "2"}},
			OutputNames: []string{"out1"},
		},
	})
}

func TestTreeValidate(t *testing.T) {
	rootIntegration := &IntegrationBlock{}
	var tree BlockTree
	err := tree.validateAndPrepare(Block{rootIntegration})
	require.Error(t, err)
	assert.Contains(t, err.Error(), "root block")
}

func TestIsTreeEnriched(t *testing.T) {
	tree := BlockTree{
		Root: RootBlock{
			Children: []Block{
				{block: &IntegrationBlock{}},
				{block: &CalcBlock{}},
				{block: &Embed{}},
				{block: &IntegrationBlock{}},
			},
		},
	}
	assert.False(t, tree.isEnriched())

	tree = BlockTree{
		Root: RootBlock{
			Children: []Block{
				{block: &IntegrationBlock{}},
				{block: &CalcBlock{}},
				{block: &IntegrationBlock{}},
			},
		},
	}
	assert.True(t, tree.isEnriched())
}

func TestEmbedFailScenarios(t *testing.T) {
	testCases := []struct {
		filename string
		errPart  string
	}{
		{
			filename: "recursive.yaml",
			errPart:  "same template several times",
		},
		{
			filename: "no_inputs.yaml",
			errPart:  "cannot map inputs on embedded template",
		},
		{
			filename: "invalid_var.yaml",
			errPart:  "embed block doesn't have input",
		},
	}

	filesPrefix := filepath.Join("templates", "embed")
	testStorage := storagetest.NewMemoryTestTemplateStorage()
	require.NoError(t, fs.WalkDir(example.TemplatesFS, filesPrefix, func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if !strings.HasSuffix(d.Name(), ".yaml") {
			return nil
		}

		file, err := example.TemplatesFS.ReadFile(path)
		if err != nil {
			return err
		}

		var b BlockTree
		if err = yaml.Unmarshal(file, &b); err != nil {
			return err
		}

		_, err = testStorage.Insert(context.Background(), strings.TrimSuffix(d.Name(), ".yaml"), string(file), nil, nil)
		return err
	}))

	for _, tt := range testCases {
		tt := tt
		t.Run(tt.filename, func(t *testing.T) {
			file, err := example.TemplatesFS.ReadFile(filepath.Join(filesPrefix, tt.filename))
			require.NoError(t, err)

			var b BlockTree
			require.NoError(t, yaml.Unmarshal(file, &b))

			_, err = b.EmbedTemplates(context.Background(), strings.TrimSuffix(tt.filename, ".yaml"), testStorage)
			require.Error(t, err)
			assert.Contains(t, err.Error(), tt.errPart)
		})
	}
}
