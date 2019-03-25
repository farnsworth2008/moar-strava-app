drop table `score_job`;
create table `score_job` (
    `id` bigint,
    `year` int,
    `detail` boolean,
    primary key (`id`)
);
alter table `athlete` drop column `username`;
