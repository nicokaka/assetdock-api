-- Convert any assets currently marked as LOST to RETIRED status
UPDATE assets SET status = 'RETIRED'::asset_status WHERE status = 'LOST'::asset_status;
