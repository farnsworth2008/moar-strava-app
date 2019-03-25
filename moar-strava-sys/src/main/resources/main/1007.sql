create or replace view `place_summary_year_view` as
    select
        `athlete_id`, `place_name`, `year`, sum(`meters`) `meters`
    from
        `move_summary_view`
    group by `athlete_id` , `place_name`, `year`;

create or replace view `table_count_view` as
    select
        'move' `table`, count(*) `count`
    from
        `move`
    union select
        'place_summary_year_cache' `table`, count(*) `place_summary_year_cache_count`
    from
        `place_summary_year_cache`
    union select
        'score_job' `table`, count(*) `score_job_count`
    from
        `score_job`
    union select
        'o_auth_token' `table`, count(*) `o_auth_token_count`
    from
        `o_auth_token`
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
        'point_backlog' `table`, count(*) `point_backlog_count`
    from
        `move` `m` left join `geo_point_d5` `g` on `g`.`id` = `m`.`point_id` where `g`.`id` is null
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