create table slack_message (
    id          bigserial primary key,
    channel     varchar(30) not null,
    ts          varchar(30) not null,
    user_id     varchar(30),
    text        text,
    created_at  timestamptz not null default now(),
    unique (channel, ts)
);
