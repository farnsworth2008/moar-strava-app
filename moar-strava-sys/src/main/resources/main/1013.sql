drop table `move`;
create table `move` (
  `point_id` bigint not null,
  `activity_id` bigint not null,
  `athlete_id` bigint,
  `year` int,
  `seq` int not null,
  `meters` decimal(10,3) not null,
  `lat` decimal(10,6) not null,
  `lon` decimal(10,6) not null,
  primary key (`point_id`,`activity_id`, `seq`)
);
alter table `move` add index (`athlete_id`, `year`, `point_id`);