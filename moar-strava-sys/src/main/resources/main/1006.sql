create table `place_summary_year_cache` (
    `athlete_id` bigint,
    `year` int,
    `result` mediumtext,
    primary key (`athlete_id`,`year`)
);