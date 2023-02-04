import {diff} from 'jsondiffpatch';
import {Contact, ContactCode} from '@yandex-int/maps._abc';
import {cloneDeep} from 'lodash';
import {config} from '../../config';
import abcProvider from '../../providers/abc';
import {DryRunError} from '../../utils/autofix';

type UpdateContact = {
    content: string;
    contact_type: ContactCode;
    title: string;
};

export async function fixMonitoringLinkExists(args: {
    slug: string,
    contacts: Contact[],
    updatedContact: UpdateContact
}) {
    const serviceContacts = args.contacts.map((contact) => ({
        title: contact.title.ru || contact.title.en,
        content: contact.content,
        contact_type: contact.type.code
    }));
    const newContacts = cloneDeep(serviceContacts);
    const searchContactIndex = newContacts
        .findIndex((contact) => contact.contact_type === args.updatedContact.contact_type);

    if (searchContactIndex !== -1) {
        newContacts[searchContactIndex] = args.updatedContact;
    } else {
        newContacts.push(args.updatedContact);
    }

    if (config.autofix.dryRun) {
        throw new DryRunError([{
            message: 'Contacts change',
            diff: diff(serviceContacts, newContacts)
        }]);
    }

    await abcProvider.updateContacts(args.slug, {contacts: newContacts});
}
