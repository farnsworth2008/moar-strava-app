create table `o_auth_token` (
    `id` varchar(50),
    `access_token` varchar(50) not null,
    `refresh_token` varchar(50) not null,
    `expires_at` bigint not null,
    primary key (`id`)
);