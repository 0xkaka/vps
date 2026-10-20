-- Apply once to existing deployments; preserve product enable/monitor switches.
UPDATE vps_product SET interval_seconds = 120 WHERE interval_seconds <> 120;
