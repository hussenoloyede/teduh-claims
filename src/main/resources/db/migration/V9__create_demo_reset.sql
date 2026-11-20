-- When the demo data was last put back to its starting state (DemoReset.java). One row, always id 1.
create table demo_reset (
    id             integer primary key check (id = 1),
    last_reset_on  date    not null
);

insert into demo_reset (id, last_reset_on) values (1, date '2000-01-01');
