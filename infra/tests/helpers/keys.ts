export type TestKeys = {
   'data-e2e':
      | 'AddSecretModal'
      | 'AddSecretVersionModal'
      | 'ApprovalPolicy:Body'
      | 'ApprovalPolicy:ConfirmDeleting'
      | 'ApprovalPolicy:ConfirmUpdating'
      | 'ApprovalPolicy:Delete'
      | 'ApprovalPolicy:Form'
      | 'ApprovalPolicy:InitCreating'
      | 'ApprovalPolicy:Modal'
      | 'ApprovalPolicy:Tooltip'
      | 'Box:Docker'
      | 'DeployPatches:AddPatch'
      | 'DeployTicket:Actions'
      | 'DeployUnitId'
      | 'Docker:Enabled'
      | 'Docker:Name'
      | 'Docker:Tag'
      | 'EditStageButton'
      | 'FormPageLayout'
      | 'FormPageLayout:Buttons'
      | 'FormPageLayout:CancelButton'
      | 'FormPageLayout:Header'
      | 'FormPageLayout:SubmitButton'
      | 'FormPageLayout:Title'
      | 'FormRevertButton'
      | 'FormRevertButton:LastChanged'
      | 'FormRevertButton:RevertButton'
      | 'FormTabs'
      | 'HugeForm'
      | 'HugeForm:Content'
      | 'Infra'
      | 'Infra:Environment'
      | 'Infra:Service'
      | 'NotifyPolicy:DeduplicationSwitcher'
      | 'ReleaseRule:Select'
      | 'RemoveStageConfirmModal:SureCheckbox'
      | 'SecretList'
      | 'SecretList:DocsLink'
      | 'SecretList:EditAliasModal'
      | 'SecretVersion:Link'
      | 'SecretVersion:NewBadge'
      | 'SideTree:EmptyList'
      | 'SideTree:Filter'
      | 'SideTree:HasError'
      | 'SideTree:ItemList'
      | 'SideTree:Nav'
      | 'SideTree:NoMatchedNodes'
      | 'SideTreeItem:Actions'
      | 'SideTreeItem:Icon'
      | 'SideTreeItem:Link'
      | 'SideTreeItem:Title'
      | 'SideTreeItem:firstRow'
      | 'SideTreeItem:isAdded'
      | 'SideTreeItem:isRemoved'
      | 'SideTreeItem:secondRow'
      | 'Stage:ActionModal'
      | 'Stage:OverrideDisruptionBudgetModal'
      | 'StageActionsPopupButton'
      | 'StageDiffView'
      | 'StageDiffView:CancelButton'
      | 'StageDiffView:DeployButton'
      | 'StageDiffView:Description'
      | 'StageDiffView:ParallelDeployButton'
      | 'StageDiffView:ReloadDelegationTokensCheckbox'
      | 'StageDiffView:SaveDraftButton'
      | 'StageHugeForm:Buttons'
      | 'StageIndexPage:Breadcrumbs'
      | 'StageIndexPage:Notifications'
      | 'StageIndexPage:Tabs'
      | 'StageIndexPage:content'
      | 'StagePatcherOptions'
      | 'StagePatcherOptions:Table'
      | 'SubForm:Actions'
      | 'SubForm:CloneButton'
      | 'SubForm:Header'
      | 'SubForm:RemoveButton'
      | 'SubForm:RestoreButton'
      | 'SubForm:Title'
      | 'Ticket:ActionModal'
      | 'Ticket:Body'
      | 'Ticket:Status'
      | 'Tickets:Body'
      | 'Tvm:Warning'
      | 'TvmClient:Mode'
      | 'TvmClient:RemoveClientButton'
      | 'TvmClient:Secret'
      | 'TvmClient:Table'
      | 'TvmDestination:RemoveButton'
      | 'Volume:Remove'
      | 'YasmTag'
      | 'YasmTag:Key'
      | 'YasmTag:RemoveTag'
      | 'YasmTag:Value'
      | 'YasmTags:Itype'
      | 'no-available-acl'
      | 'side-tree'
      | 'story'
      | `Action:<string>`
      | `FormTabContent:<string>`
      | `SideTreeItem:<string>Icon`
      | `SideTreeItem:<string>`
      | `SubForm:<string>`
      | `TvmClient:<string>`
      | `TvmDestination:<string>`
      | (string & {});
   'data-test':
      | 'AddSecretModal-Secret'
      | 'AddSecretModal-Version'
      | 'AddSecretModal:Alias'
      | 'AdvancedSettings:Access:Group'
      | 'AdvancedSettings:Access:User'
      | 'AdvancedSettings:Limits:Cpu'
      | 'AdvancedSettings:Limits:Ram'
      | 'Antiaffinity:PerNode'
      | 'Antiaffinity:PerRack'
      | 'Box:DynamicResources'
      | 'Box:JugglerSettings'
      | 'Box:Layers'
      | 'Box:StaticResources'
      | 'Box:Volumes'
      | 'BundleRow:Url'
      | 'DefaultLayerSourceFileStoragePolicy'
      | 'DeployTicket:Link'
      | 'DeployTicket:Row'
      | 'DiffView:FullContext'
      | 'DiffView:Toolbar'
      | 'DiffView:Type'
      | 'Disk:Layers'
      | 'Disk:StaticResources'
      | 'Disk:Volumes'
      | 'JugglerSettings:Bundles'
      | 'MultipleInputField'
      | 'MultipleInputRow:Input'
      | 'RemoveStageConfirmModal:Loader'
      | 'Resource:Id'
      | 'SandboxParams:DeployPatches'
      | 'StageConfigPage'
      | 'StageHistory:BackHistoryButton'
      | 'StageHistory:CompareRevisions'
      | 'StageHistory:CurrentRevision'
      | 'StageHistory:DiffApply'
      | 'StageHistory:DiffViewer'
      | 'StageHistory:List'
      | 'StageHistory:MessageBlock'
      | 'StageHistory:MessageEmpty'
      | 'StageHistory:Revision'
      | 'StageHistory:RevisionActions'
      | 'StageHistory:RevisionId'
      | 'StageHistory:RevisionMessage'
      | 'StageHistory:RevisionSpec'
      | 'StageHistory:RevisionSummary'
      | 'StageHistory:RevisionTime'
      | 'StageHistory:RevisionUser'
      | 'StageHistory:SelectRevision'
      | 'StageHistory:ShowYaml'
      | 'StageQuota:ABC'
      | 'StageQuota:LocationTitle'
      | 'StageQuota:OverQuota'
      | 'TagList'
      | 'TagList:Tag'
      | 'TextInputField'
      | 'Volume:Id'
      | 'Volume:Mode'
      | 'Volume:MountPoint'
      | 'YCSelectField'
      | 'YCSelectMultipleField'
      | 'actions'
      | 'balancer-owners'
      | 'balancers-list'
      | 'breadcrumbs'
      | 'button-close'
      | 'category-container'
      | 'confirm-dialog'
      | 'confirmation'
      | 'create-project-button'
      | 'creationDate'
      | 'deploy-tickets-list'
      | 'deploy-unit--badge'
      | 'deploy-unit--name'
      | 'deploy-unit--revision'
      | 'filter-logs-by-deploy-unit'
      | 'filter-logs-by-level'
      | 'filter-logs-from-date'
      | 'filter-logs-submit'
      | 'filter-logs-to-date'
      | 'filter-projects-by-name'
      | 'filter-projects-by-owner'
      | 'filter-projects-by-type'
      | 'filter-projects-reset'
      | 'filter-projects-search'
      | 'filter-reset'
      | 'force-refresh'
      | 'form-field'
      | 'form-field--tags'
      | 'form-field-content'
      | 'form-field-message'
      | 'infracloud--container-content'
      | 'infracloud--container-header'
      | 'json-custom'
      | 'list-item'
      | 'load-more-tickets'
      | 'log--expand'
      | 'log--load-more'
      | 'log--logger'
      | 'log--message'
      | 'log--source'
      | 'log--timestamp-date'
      | 'log--timestamp-time'
      | 'logo'
      | 'logs--row'
      | 'logs-not-found'
      | 'monitoring-iframe'
      | 'monitoring-select'
      | 'namespace-info-backends'
      | 'namespace-info-certs'
      | 'namespace-info-dns'
      | 'namespace-info-header'
      | 'namespace-info-l3'
      | 'namespace-info-l7'
      | 'namespace-info-upstreams'
      | 'navigation-tabs'
      | 'no-deploy-units'
      | 'page-content'
      | 'pod--additional'
      | 'pod--fqdn'
      | 'pod--host'
      | 'pod--revision'
      | 'pod--wall-e-link'
      | 'pod--yasm-link'
      | 'pods-filter'
      | 'pods-filter--custom'
      | 'pods-filter--custom-button'
      | 'pods-filter--custom-checkbox'
      | 'pods-filter--custom-input'
      | 'pods-filter--location'
      | 'pods-filter--pagination'
      | 'pods-filter--revision'
      | 'pods-filter--status'
      | 'primary-actions'
      | 'project-actions__create-stage'
      | 'project-create-form__button_cancel'
      | 'project-create-form__button_submit'
      | 'replica-set--cluster'
      | 'replica-set--pods-total'
      | 'replica-set--revision'
      | 'set-limit'
      | 'set-timezone'
      | 'stage--badge'
      | 'stage--name'
      | 'stage--revision'
      | 'stage-balancers'
      | 'status--badge'
      | 'status--pod'
      | 'status--replica-set'
      | 'support-link-docs'
      | 'support-link-st'
      | 'support-link-telegram'
      | 'support-links'
      | 'view-stage--logs'
      | 'view-stage--monitoring'
      | 'view-stage--status'
      | 'yt-error'
      | `<string>__button_cancel`
      | `<string>__button_submit`
      | `BundleRow:<string>`
      | `DynamicResource:<string>`
      | `Layer:<string>`
      | `MultipleInputRow:<string>`
      | `StaticResource:<string>`
      | `Volume:<string>`
      | `action-<string>`
      | `navigation-tabs--<string>`
      | `primary-action-<string>`
      | `project--<string>`
      | `project-actions__<string>`
      | `stage--<string>`
      | (string & {});
   'qa':
      | 'AddSecretModal:AddButton'
      | 'AddSecretModal:CancelButton'
      | 'AddSecretVersionModal:CancelButton'
      | 'AddSecretVersionModal:OkButton'
      | 'ProjectEditForm:CancelButton'
      | 'ProjectEditForm:SubmitButton'
      | 'SecretSubForm:ClearUnused'
      | 'Unistat:AddUnistatButton'
      | 'commands.init.buttons.add'
      | `LocationCard:<string>`
      | (string & {});
};
