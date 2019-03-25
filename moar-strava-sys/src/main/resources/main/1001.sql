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

create or replace view `activity_view` as
    select
        year(`s`.`start_date`) `year`,
        month(`s`.`start_date`) `month`,
        day(`s`.`start_date`) `day`,
        `s`.`id` as `activity_id`,
        `s`.`athlete_id`,
        `name`,
        `type`,
        sum(`m`.`meters`) `movement`
    from
		    `activity_summary` `s`
            join
        `move` `m` on `m`.`activity_id` = `s`.`id`
            join
        `geo_point_d5` `p` on `p`.`id` = `m`.`point_id`
    group by `s`.`id` , `name` , `type` , `distance`
    order by `activity_id` desc;

create or replace view `move_view` as
    select
        year(`s`.`start_date`) `year`,
        month(`s`.`start_date`) `month`,
        day(`s`.`start_date`) `day`,
        `s`.`athlete_id`,
        `s`.`id` `activity_id`,
        `m`.`meters`,
        `s`.`type`,
        `p`.`off_road`,
        `q`.`country`,
        `q`.`state`,
        `q`.`county`,
        `q`.`place_name`
    from
		    `activity_summary` `s`
            join
        `move` `m` on `m`.`activity_id` = `s`.`id`
            join
        `geo_point_d5` `p` on `p`.`id` = `m`.`point_id`
            join
        `place` `q` on `q`.`id` = `p`.`place_id`
    order by `year` , `month`, `day`, `athlete_id`, `activity_id`, `meters`, `off_road`, `county`, `place_name`;

create or replace view `move_summary_view` as
    select
        `athlete_id`,
        `activity_id`,
        `year`,
        `month`,
        `day`,
        `type`,
        `country`,
        `state`,
        `county`,
        `place_name`,
        `off_road`,
        sum(`meters`) `meters`
    from
        `move_view`
    group by `athlete_id` , `activity_id`, `year` , `month` , `country` , `state` , `county` , `place_name` , `off_road`
    order by `meters` desc;

create or replace view `place_point_count_view` as
    select
        `p`.`id`,
        `p`.`country`,
        `p`.`state`,
        `p`.`county`,
        `p`.`place_name`,
        `boundary`,
        count(*) `point_count`
    from
        `place` `p`
            join
        `geo_point_d5` `g` on `g`.`place_id` = `p`.`id`
    group by `p`.`id`, `p`.`country`, `p`.`state`, `p`.`county` , `p`.`place_name`, `boundary`
    order by `point_count` desc;

create or replace view `point_view` as
    select
        `g`.`id` `point_id`,
        `g`.`off_road`,
        `g`.`inferred`,
        `p`.`country`,
        `p`.`state`,
        `p`.`county`,
        `p`.`place_name`
    from
        `geo_point_d5` `g`
        join `place` `p` on `p`.`id` = `g`.`place_id`;

create or replace view `point_move_count_view` as
    select
        `m`.`point_id`,
        count(*) `move_count`
    from
        `geo_point_d5` `g`
        join `move` `m` on `m`.`point_id` = `g`.`id`
    group by `g`.`id`
    order by `move_count` desc;

create or replace view `boundary_view` as
    select
        `b`.`point_id`, `b`.`place_id`, `b`.`seq`, `p`.`place_name`
    from
        `place_boundary` `b`
            join
        `place` `p` on `p`.`id` = `b`.`place_id`
    order by `place_id` , `seq`;