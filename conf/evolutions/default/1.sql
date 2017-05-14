# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table device (
  device_id                 varchar(255) not null,
  vendor_id                 varchar(255) not null,
  unique_hash               varchar(255) not null,
  constraint uq_device_unique_hash unique (unique_hash))
;

create table tags (
  tag_name                  varchar(255) not null,
  vendor_id                 varchar(255) not null)
;




# --- !Downs

drop table if exists device cascade;

drop table if exists tags cascade;

