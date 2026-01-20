```sql
CREATE TABLE mart_data.boundary_region
(
    region_code             text            NULL,
    region_english_name     text            NULL,
    region_korean_name      text            NULL,
    region_full_korean_name text            NULL,
    geom                    public.geometry NULL,
    is_donut_polygon        bool            NULL,
    center_geom             public.geometry NULL,
    center_lng              float8          NULL,
    center_lat              float8          NULL,
    area_paths              jsonb           NULL,
    gubun                   text            NULL
);
CREATE INDEX idx_boundary_region_geom ON mart_data.boundary_region USING gist (geom);
CREATE INDEX idx_boundary_region_region_code_gubun ON mart_data.boundary_region USING btree (region_code, gubun);
CREATE INDEX idx_boundary_region_sido ON mart_data.boundary_region USING btree ("left"(region_code, 2));
CREATE INDEX idx_boundary_region_sigungu ON mart_data.boundary_region USING btree ("left"(region_code, 5));
```