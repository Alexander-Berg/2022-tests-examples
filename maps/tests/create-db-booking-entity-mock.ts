import {PlusOfferStatus} from 'app/v1/types/plus';
import {resourcesMock} from 'app/v1/routes/mocks/resources';
import {servicesMock} from 'app/v1/routes/mocks/services';
import {BookingInput, Appointment, BookingStatus, DBBookingEntity} from 'app/v1/types/booking';
import {v4 as uuid} from 'uuid';
import {baseBookingBody} from 'app/v1/routes/mocks/booking';
import {PARTNER_COMPANY_ID} from 'app/v1/routes/mocks/company';

type MockDBBookingInput = {
    status?: BookingStatus;
    hidden?: boolean;
    passport_id?: string;
    yandex_user_id?: string;
    partner_booking_id?: string;
    company_image?: string;
    appointment?: Appointment;
    partner_id?: string;
    plus_offer_amount?: number | null;
    plus_offer_status?: PlusOfferStatus | null;
} & Partial<BookingInput>;

function createDBBookingEntityMock(data: MockDBBookingInput): DBBookingEntity {
    const id = uuid();
    return {
        id,
        partner_id: data.partner_id ?? 'yclients',
        permalink: data.permalink ?? baseBookingBody.permalink,
        yandex_user_id: '123',
        phone: data.phone ?? baseBookingBody.phone,
        name: data.name ?? baseBookingBody.name,
        email: data.email ?? baseBookingBody.email,
        partner_company_id: data.partner_booking_id ?? PARTNER_COMPANY_ID,
        resource_id: data.appointment?.resourceId ?? baseBookingBody.appointment.resourceId,
        datetime: data.appointment?.datetime ?? baseBookingBody.appointment.datetime,
        partner_booking_id: data.partner_booking_id ?? `partner-${id}`,
        passport_id: data.passport_id ?? '123',
        comment: data.comment ?? baseBookingBody.comment,
        platform: data.platform ?? '',
        source: data.source ?? '',
        meta: data.meta ?? null,
        status: data.status ?? BookingStatus.CONFIRMED,
        company_name: 'Мистер Сосиска',
        company_address: 'Москва, ул. Пушкина, дом Колотушкина, 1',
        company_image: data.company_image || null,
        resource_name: resourcesMock[0].title,
        resource_description: resourcesMock[0].description || '',
        resource_image: resourcesMock[0].image || '',
        lat: 55.577015,
        lon: 52.55975,
        uri: null,
        services_info: [
            {
                id: servicesMock[0].id,
                image: servicesMock[0].image || undefined,
                price: servicesMock[0].price,
                title: servicesMock[0].title,
                category: servicesMock[0].category,
                resources: [
                    {
                        id: '1423132',
                        duration: 3600
                    },
                    {
                        id: '1572138',
                        duration: 3600
                    }
                ],
                description: servicesMock[0].description || undefined
            },
            {
                id: servicesMock[1].id,
                image: servicesMock[1].image || undefined,
                price: servicesMock[1].price,
                title: servicesMock[1].title,
                category: servicesMock[1].category,
                resources: [
                    {
                        id: '1423132',
                        duration: 3600
                    },
                    {
                        id: '1572138',
                        duration: 3600
                    }
                ],
                description: servicesMock[1].description || undefined
            }
        ],
        hidden: data.hidden ?? false, // check hidden
        tz_offset: 10800,
        updated_by: 'user',
        created_at: '2021-08-30T09:41:10.602Z',
        updated_at: '2021-08-30T11:27:02.605Z',
        company_category_class: null,
        plus_offer_amount: data.plus_offer_amount ?? null,
        plus_offer_status: data.plus_offer_status || null,
        purchase_token: null
    };
}

export {createDBBookingEntityMock, MockDBBookingInput};
