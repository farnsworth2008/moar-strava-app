create or replace view `table_count_view` as
    select
        'move' `table`, count(*) `count`
    from
        `move`
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
        'geo_point_d5' `table`, count(*) `geo_point_d5_count`
    from
        `geo_point_d5`
    union select
        'geo_point_d5_located' `table`, count(*) `geo_point_d5_count`
    from
        `geo_point_d5`
    where `inferred` is null
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
        `activity_detail`;

