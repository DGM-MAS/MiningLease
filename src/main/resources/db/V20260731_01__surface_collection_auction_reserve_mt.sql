-- UAT feedback: Surface Collection Auction Report needs a "Reserve (MT)" column,
-- but the auction application had no such field to report on. Gives the auction
-- creation form a reserveMt input.

ALTER TABLE t_surface_collection_auction
    ADD COLUMN IF NOT EXISTS reserve_mt NUMERIC(12,2);
