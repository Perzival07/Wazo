create table request (
    id            bigserial primary key,
    title         text not null,
    description   text,
    status        varchar(20) not null default 'PENDING',
    slack_channel varchar(30),
    slack_ts      varchar(30),
    decided_by    varchar(50),
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    unique    (slack_channel, slack_ts)
);

create table slack_event_log (
    event_id      varchar(60) primary key,
    recieved_at   timestamptz not null default now()
);