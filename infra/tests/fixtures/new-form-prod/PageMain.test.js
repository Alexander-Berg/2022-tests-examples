import { HOST, PROJECT_NAME, ROUTES } from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
import { app } from '../../page_objects/AppNewForm';

const { header, page } = app;

fixture`Page Main tests (new)`.skip
   .beforeEach(async t => {
      await t.useRole(robotRole);
   })
   .page(HOST);

test('Page opens', async t => {
   await t
      .expect(page.main.body.exists)
      .ok()
      .expect(page.main.header.exists)
      .ok()
      .expect(page.main.content.exists)
      .ok();
});

test('Header elements exist', async t => {
   await t.click(header.linkLogo);

   await t
      .expect(header.supportLinks.exists)
      .ok()
      .expect(header.linkTelegram.exists)
      .ok()
      .expect(header.linkST.exists)
      .ok()
      .expect(header.linkDocs.exists)
      .ok()
      .expect(header.user.exists)
      .ok();

   await t.expect(header.eventBadge.exists).ok();

   await t.expect(header.eventPopup.exists).notOk();

   await t.click(header.eventBadge);

   await t.expect(header.eventPopup.exists).ok();

   await t.click(header.eventBadge);

   await t.expect(header.eventPopup.exists).notOk();
});

test('Stage filters work', async t => {
   await page.main.filters.owner.hasLabels(t, ['My', 'All']);
   await page.main.filters.owner.readValue(t, 'My');

   await t
      .expect(page.main.filters.owner.radioButton.exists)
      .ok()
      .expect(page.main.filters.name.input.exists)
      .ok()
      .expect(page.main.filters.buttons.reset.element.exists)
      .ok();

   if (HOST !== 'https://acceptance.deploy.yandex-team.ru') {
      const UI_PROJECT_NAME = 'deploy-ui';

      // у робота есть доступ только к 4 стейджам!
      const UI_STAGE_NAMES = ['man-pre-deploy-ui', 'pre-deploy-ui', 'pre2-deploy-ui', 'test-deploy-ui'];

      await t.click(page.main.projectWithName(UI_PROJECT_NAME));

      for (const name of UI_STAGE_NAMES) {
         await t.expect(page.main.stageWithName(name).exists).ok(`stage ${name} exists`);
      }

      await page.main.filters.owner.change(t, 'My', 'All');
      await page.main.filters.owner.change(t, 'All', 'My');
      await page.main.filters.owner.change(t, 'My', 'All');

      for (const name of UI_STAGE_NAMES) {
         await t.expect(page.main.stageWithName(name).exists).ok(`stage ${name} exists`);
      }

      for (const name of UI_STAGE_NAMES) {
         const noMatches = UI_STAGE_NAMES.find(v => v !== name && !v.includes(name));

         await page.main.filters.name.typeText(t, name);
         await page.main.filters.buttons.search.click(t);

         await t
            .expect(page.main.stageWithName(name).exists)
            .ok(`stage ${name} exists`)
            .expect(page.main.stageWithName(noMatches).exists)
            .notOk(`stage ${noMatches} not exists`);
      }
   }

   await page.main.filters.buttons.reset.click(t);

   await page.main.filters.name.replaceText(t, '', '$#%');
   await page.main.filters.buttons.search.click(t);

   await t.expect(page.main.noStagesMatchingYourCriteria.exists).ok();
});

test('Launch New Stage & Cancel buttons work', async t => {
   await t.expect(page.main.header.exists).ok().expect(page.main.body.exists).ok();

   await t.click(page.main.project);
   await page.main.buttons.launchNewStage.click(t);

   await t.expect(page.main.getLocation()).contains(ROUTES.NEW_STAGE(PROJECT_NAME));

   await t.click(header.linkLogo);

   await t
      .expect(page.main.body.exists)
      .ok()
      .expect(page.stage.formNew.pageCreateStage.exists)
      .notOk()
      .expect(page.main.getLocationPathName())
      .eql(ROUTES.HOME);
});
