create table `place_summary_year_result` (
    `athlete_id` bigint,
    `year` int,
    `result` mediumtext,
    `detail` boolean,
    `milliseconds` bigint,
    primary key (`athlete_id`,`year`)
);

drop table place_summary_year_cache;

alter table move add index (point_id, activity_id, meters);