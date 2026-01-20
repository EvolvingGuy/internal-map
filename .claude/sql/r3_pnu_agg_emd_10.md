```sql
CREATE TABLE manage.r3_pnu_agg_emd_10
(
    id       int8 GENERATED ALWAYS AS IDENTITY ( INCREMENT BY 1 MINVALUE 1 MAXVALUE 9223372036854775807 START 1 CACHE 1 NO CYCLE) NOT NULL,
    code     int8                                                                                                                 NOT NULL,
    h3_index int8                                                                                                                 NOT NULL,
    cnt      int4                                                                                                                 NOT NULL,
    sum_lat  float8                                                                                                               NOT NULL,
    sum_lng  float8                                                                                                               NOT NULL,
    CONSTRAINT r3_pnu_agg_emd_10_pkey PRIMARY KEY (id),
    CONSTRAINT uq_pnu_h3 UNIQUE (code, h3_index)
);
```