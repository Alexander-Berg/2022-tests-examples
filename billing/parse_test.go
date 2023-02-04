package test

import (
	"context"
	"io/fs"
	"path/filepath"
	"strings"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	yaml2 "gopkg.in/yaml.v2"

	"a.yandex-team.ru/billing/configshop/example"
	"a.yandex-team.ru/billing/configshop/pkg/core/yaml"
	storagetest "a.yandex-team.ru/billing/configshop/pkg/storage/xtest"
)

func TestTreeMarshalUnmarshal(t *testing.T) {
	require.NoError(t, fs.WalkDir(example.TemplatesFS, "templates", func(path string, d fs.DirEntry, err error) error {
		if d.IsDir() {
			return nil
		}

		t.Run(d.Name(), func(t *testing.T) {
			content, err := example.TemplatesFS.ReadFile(path)
			require.NoError(t, err)

			var tree yaml.BlockTree
			require.NoError(t, yaml2.Unmarshal(content, &tree))

			newContent, err := yaml.CanonizeTree(content)
			require.NoError(t, err)

			var tree2 yaml.BlockTree
			require.NoError(t, yaml2.Unmarshal(newContent, &tree2))

			assert.Equal(t, tree, tree2)

		})
		return nil
	}))
}

func TestEmbedExpanded(t *testing.T) {
	ctx := context.Background()
	templateStore := storagetest.NewMemoryTestTemplateStorage()

	for _, filename := range []string{"test_2_integrations.yaml", "embed_graph.yaml"} {
		file, err := example.TemplatesFS.ReadFile(filepath.Join("templates", filename))
		require.NoError(t, err)

		_, err = templateStore.Insert(ctx, strings.TrimSuffix(filename, ".yaml"),
			string(file), nil, nil)
		require.NoError(t, err)
	}

	file, err := example.TemplatesFS.ReadFile(filepath.Join("templates", "embed_embed_graph.yaml"))
	require.NoError(t, err)

	var cfg yaml.BlockTree
	require.NoError(t, yaml2.Unmarshal(file, &cfg))

	enrichedCfg, err := cfg.EmbedTemplates(ctx, "embed_embed_graph", templateStore)
	require.NoError(t, err)

	expectedTpl, err := example.TemplatesFS.ReadFile(filepath.Join("templates", "expanded_embed_embed_graph.yaml"))
	require.NoError(t, err)

	gotTplBytes, err := yaml2.Marshal(enrichedCfg)
	require.NoError(t, err)

	assert.Equal(t, string(expectedTpl), string(gotTplBytes))
}
