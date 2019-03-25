create or replace view `place_summary_year_view` as
    select
        `athlete_id`,
        `year`,
        `summary_place_name` `place_name`,
        `meters`
    from
        (select
            `athlete_id`,
                (case
                    when (`v`.`place_name` > '') then `v`.`place_name`
                    else concat((case
                        when `off_road` = 1 then 'Other '
                        else 'Road '
                    end), (case
                        when `state` is not null then `state`
                        else coalesce(`country`, 'Unknown')
                    end))
                end) `summary_place_name`,
                `year`,
                sum(`meters`) `meters`
        from
            `move_summary_view` `v`
        group by `athlete_id` , `summary_place_name` , `year`) `summary`;

create or replace view `place_summary_view` as
	select `athlete_id` , `place_name`, sum(`meters`) `meters`
    from `place_summary_year_view`
    group by `athlete_id`, `place_name`;