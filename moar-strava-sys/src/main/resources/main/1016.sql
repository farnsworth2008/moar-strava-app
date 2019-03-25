drop table `move`;
create table `move` (
  `point_id` bigint not null,
  `activity_id` bigint not null,
  `seq` int not null,
  `athlete_id` bigint not null,
  `off_road` boolean,
  `place_id` bigint not null,
  `year` int not null,
  `meters` decimal(10,3) not null,
  `lat` decimal(10,6) not null,
  `lon` decimal(10,6) not null,
  primary key (`point_id`,`activity_id`, `seq`)
);
alter table `move` add index (`athlete_id`, `year`, `place_id`, `meters`);
alter table o_auth_token modify column id bigint not null;