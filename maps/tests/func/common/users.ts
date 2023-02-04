interface User {
    login: string;
    password: string;
}

enum UserRole {
    USER = 'user',
    ADMIN = 'admin',
    MODERATOR = 'moderator',
    SUPERUSER = 'superuser',
    WITHOUT_ACCESS = 'without-access'
}

const USERS_BY_ROLE: Record<UserRole, User> = {
    [UserRole.USER]: {
        login: 'geoadv-dev-robot-user',
        password: 'Geoadv123!'
    },
    [UserRole.ADMIN]: {
        login: 'geoadv-dev-robot',
        password: 'Geoadv123!'
    },
    [UserRole.MODERATOR]: {
        login: 'geoadv-dev-robot-moderator',
        password: 'Geoadv123!'
    },
    [UserRole.SUPERUSER]: {
        login: 'geoadv-dev-robot-superuser',
        password: 'Geoadv123!'
    },
    [UserRole.WITHOUT_ACCESS]: {
        login: 'geoadv-dev-robot-no-access',
        password: 'Geoadv123!'
    }
};

export default USERS_BY_ROLE;
export {UserRole, User};
