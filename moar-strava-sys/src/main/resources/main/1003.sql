create table `athlete` (
    `id` bigint not null,
    `firstname` varchar(50),
    `lastname` varchar(50),
    `profile_medium` varchar(1024),
    `profile` varchar(1024),
    `city` varchar(50),
    `state` varchar(50),
    `country` varchar(50),
    `sex` varchar(1),
    `username` varchar(50),
    created_at date not null,
    primary key (`id`)
);

create table `athlete_segment` (
  `athlete_id` bigint not null,
  `segment_id` bigint not null,
  primary key (`athlete_id`, `segment_id`)
);

create or replace view `table_count_view` as
    select
        'move' `table`, count(*) `count`
    from
        `move`
    union select
        'point_backlog' `table`, count(*) `point_backlog_count`
    from
        `move` `m` left join `geo_point_d5` `g` on `g`.`id` = `m`.`point_id` where `g`.`id` is null
    union select
        'athlete' `table`, count(*) `athlete_count`
    from
        `athlete`
    union select
        'athlete_segment' `table`, count(*) `athlete_segment_count`
    from
        `athlete_segment`
    union select
        'activity_list' `table`, count(*) `activity_list_count`
    from
        `activity_list`
    union select
        'segment' `table`, count(*) `segment_count`
    from
        `segment`
    union select
        'activity_list_entry' `table`, count(*) `activity_list_entry_count`
    from
        `activity_list_entry`
    union select
        'activity_summary' `table`, count(*) `activity_summary_count`
    from
        `activity_summary`
    union select
        'point' `table`, count(*) `geo_point_d5_count`
    from
        `geo_point_d5`
    union select
        'point_located' `table`, count(*) `geo_point_d5_count`
    from
        `geo_point_d5`
    where inferred is null
    union select
        'place' `table`, count(*) `place_count`
    from
        `place`
    union select
        'moar_main' `table`, count(*) `moar_main_count`
    from
        `moar_main`
    union select
        'place_boundary' `table`, count(*) `place_boundary_count`
    from
        `place_boundary`
    union select
        'activity_detail' `table`, count(*) `activity_detail_count`
    from
        `activity_detail` order by `table`;