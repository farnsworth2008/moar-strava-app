create table `place` (
  `id` bigint not null auto_increment,
  `country` varchar(100),
  `state` varchar(100),
  `county` varchar(100),
  `place_name` varchar(100),
  `boundary` boolean,
  unique key(`country`, `state`, `county`, `place_name`),
  primary key (`id`)
);

create table `geo_point_d5` (
  `id` bigint not null,
  `place_id` bigint not null,
  `off_road` boolean not null,
  `inferred` bigint,
  primary key (`id`)
);

create table `place_boundary` (
  `place_id` bigint not null,
  `seq` bigint not null,
  `point_id` bigint not null,
  primary key (`place_id`,`seq`)
);

create table `activity_list` (
  `id` bigint not null auto_increment,
  `athlete_id` bigint not null,
  `before` bigint not null,
  `after` bigint not null,
  primary key (`id`)
);

create table `activity_list_entry` (
  `activity_list_id` bigint not null,
  `activity_id` bigint not null,
  primary key (`activity_list_id`, `activity_id`)
);

create table `activity_detail` (
  `id` bigint not null,
  `polyline` longtext,
  `scored_distance` decimal(8,1) not null,
  primary key (`id`)
);

create table `activity_summary` (
  `id` bigint not null,
  `athlete_id` bigint not null,
  `summary_polyline` longtext,
  `start_date` date not null,
  `name` varchar(1024) not null,
  `distance` decimal(8,1) not null,
  `elapsed_time` decimal(8,1) not null,
  `moving_time` decimal(8,1) not null,
  `type` varchar(50) not null,
  primary key (`id`)
);

create table `move` (
  `point_id` bigint not null,
  `activity_id` bigint not null,
  `seq` int not null,
  `meters` decimal(10,3) not null,
  `lat` decimal(10,6) not null,
  `lon` decimal(10,6) not null,
  primary key (`point_id`,`activity_id`, `seq`)
);

create table `segment` (
  `id` bigint not null,
  name varchar(255),
  activity_type varchar(255),
  start_latitude double,
  start_longitude double,
  average_grade double,
  climb_category double,
  distance double,
  elevation_high double,
  elevation_low double,
  maximum_grade double,
  primary key (`id`)
);