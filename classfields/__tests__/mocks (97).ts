import { ISitePlansSerpModalProps, FiltersState } from '..';

export const getProps = (filtersState: FiltersState) =>
    (({
        isVisible: true,
        card: {
            withBilling: true,
            weekTimeTable: true,
            timestamp: 1586963713431,
            salesDepartment: {
                name: 'Группа Компаний ПИК',
                encryptedPhones: [
                    {
                        phoneHash: 'KzcF5MHTEJ5MLzIN2MPTgRy',
                    },
                ],
                weekTimetable: [
                    {
                        dayFrom: 1,
                        dayTo: 7,
                        timePattern: [
                            {
                                open: '09:00',
                                close: '21:00',
                            },
                        ],
                    },
                ],
                timetableZoneMinutes: 180,
            },
        },
        filtersState,
    } as unknown) as ISitePlansSerpModalProps);
