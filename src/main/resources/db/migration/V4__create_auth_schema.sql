CREATE TABLE roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_code (code),
    INDEX idx_roles_status (status)
);

CREATE TABLE permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(120) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_code (code),
    INDEX idx_permissions_status (status)
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role_id
        FOREIGN KEY (role_id)
        REFERENCES roles (id),
    CONSTRAINT fk_role_permissions_permission_id
        FOREIGN KEY (permission_id)
        REFERENCES permissions (id)
);

CREATE TABLE user_accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    phone VARCHAR(50),
    user_status VARCHAR(50) NOT NULL,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_accounts_email (email),
    INDEX idx_user_accounts_status (status),
    INDEX idx_user_accounts_user_status (user_status)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user_id
        FOREIGN KEY (user_id)
        REFERENCES user_accounts (id),
    CONSTRAINT fk_user_roles_role_id
        FOREIGN KEY (role_id)
        REFERENCES roles (id)
);

INSERT INTO roles (code, name, description)
VALUES
    ('USER', 'User', 'Default customer role'),
    ('ADMIN', 'Admin', 'System administrator role'),
    ('STAFF', 'Staff', 'Order operation role'),
    ('WAREHOUSE', 'Warehouse', 'Inventory operation role');

INSERT INTO permissions (code, name, description)
VALUES
    ('PRODUCT_READ', 'Read products', 'View product catalog data'),
    ('PRODUCT_WRITE', 'Write products', 'Create, update, and delete products'),
    ('ORDER_READ', 'Read orders', 'View order data'),
    ('ORDER_WRITE', 'Write orders', 'Create and update orders'),
    ('INVENTORY_READ', 'Read inventory', 'View inventory and reservations'),
    ('INVENTORY_WRITE', 'Write inventory', 'Create and update inventory reservations'),
    ('USER_READ', 'Read users', 'View user account data'),
    ('USER_WRITE', 'Write users', 'Create and update user account data'),
    ('ROLE_READ', 'Read roles', 'View role and permission configuration'),
    ('ROLE_WRITE', 'Write roles', 'Create and update role and permission configuration');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('PRODUCT_READ', 'ORDER_READ', 'ORDER_WRITE')
WHERE r.code = 'USER';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'PRODUCT_READ',
    'PRODUCT_WRITE',
    'ORDER_READ',
    'ORDER_WRITE',
    'INVENTORY_READ',
    'INVENTORY_WRITE',
    'USER_READ',
    'USER_WRITE',
    'ROLE_READ',
    'ROLE_WRITE'
)
WHERE r.code = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('PRODUCT_READ', 'ORDER_READ', 'ORDER_WRITE')
WHERE r.code = 'STAFF';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('PRODUCT_READ', 'INVENTORY_READ', 'INVENTORY_WRITE')
WHERE r.code = 'WAREHOUSE';
