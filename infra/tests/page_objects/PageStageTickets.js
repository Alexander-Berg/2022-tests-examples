import { SelectDataE2e } from '../helpers/extractors';
import { PageBase } from './PageBase';
import { Button } from './components/Button';
import { TextInput } from './components/TextInput';
import { TextArea } from './components/TextArea';
import { Select } from './components/Select';
import { Link } from './components/Link';
import { ReleaseSandboxRule } from './ReleaseSandboxRule';
import { Selector } from 'testcafe';

export class PageStageTickets extends PageBase {
   body = SelectDataE2e('Tickets:Body');

   buttons = {
      approvalPolicy: new Button(SelectDataE2e('ApprovalPolicy:Tooltip')),
      // reset
      // force refresh
      // skip selected
   };

   releaseRule = {
      select: new Select(SelectDataE2e('ReleaseRule:Select')),
      actions: {
         newSandboxRule: new Link(Selector('.yc-popup.yc-popup_open').find('a').withText('New Sandbox rule')),
         newDockerRule: new Link(Selector('.yc-popup.yc-popup_open').find('a').withText('New Docker rule')),
      },
      body: new ReleaseSandboxRule(),
   };

   getApprovalAction = name => t => new Button(SelectDataE2e(`ApprovalPolicy:${name}`)).click(t);

   approvalPolicy = {
      body: SelectDataE2e('ApprovalPolicy:Body'),
      actions: {
         init: this.getApprovalAction('InitCreating'),
         create: this.getApprovalAction('Create'),
         update: this.getApprovalAction('Update'),
         delete: this.getApprovalAction('Delete'),
         confirmDeleting: this.getApprovalAction('ConfirmDeleting'),
         confirmUpdating: this.getApprovalAction('ConfirmUpdating'),
         close: t => new Button(Selector('.yc-dialog-btn-close__btn')).click(t),
      },
      form: {
         body: SelectDataE2e('ApprovalPolicy:Form'),
         count: new TextInput(SelectDataE2e('ApprovalPolicy:Form').find('input[name="count"]').parent()),
      },
   };

   getTicketAction = name => async t => {
      await this.ticket.buttons[name].click(t);
      await t.expect(SelectDataE2e('Ticket:ActionModal').exists).eql(true);
      await new TextArea(SelectDataE2e('Ticket:ActionModal').find('textarea[name="message"').parent()).typeText(
         t,
         `testcafe ${name}`,
      );
      await new Button(SelectDataE2e('Ticket:ActionModal').find('button[type="submit"]')).click(t);
   };

   ticket = {
      body: SelectDataE2e('Ticket:Body'),
      buttons: {
         approve: new Button(SelectDataE2e('DeployTicket:Approve')),
         disapprove: new Button(SelectDataE2e('DeployTicket:Disapprove')),
         commit: new Button(SelectDataE2e('DeployTicket:Commit')),
         skip: new Button(SelectDataE2e('DeployTicket:Skip')),
         actions: new Button(SelectDataE2e('DeployTicket:Actions')),
      },
      actions: {
         approve: this.getTicketAction('approve'),
         disapprove: this.getTicketAction('disapprove'),
         commit: this.getTicketAction('commit'),
         skip: this.getTicketAction('commit'),
      },
      status: SelectDataE2e('Ticket:Status'),
   };
}
