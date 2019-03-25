create or replace view `move_view` as
    select
    `m`.`year`,
        `m`.`athlete_id`,
        `m`.`off_road`,
        `q`.`country`,
        `q`.`place_name`,
        `q`.`state`,
        `m`.`meters`
    from
        `move` `m`
            join
        `place` `q` on `q`.`id` = `m`.`place_id`;

create or replace view `move_summary_year_view` as
    select
        `athlete_id`,
        `year`,
        `country`,
        `off_road`,
        `place_name`,
        `state`,
        sum(`meters`) `meters`
    from
        `move_view`
    group by `athlete_id` , `year` , `country` , `state` ,  `place_name` , `off_road`;

create or replace view `move_summary_view` as
    select
        `athlete_id`,
        `country`,
        `off_road`,
        `place_name`,
        `state`,
        sum(`meters`) `meters`
    from
        `move_view`
    group by `athlete_id` , `country` , `state` ,  `place_name` , `off_road`;
