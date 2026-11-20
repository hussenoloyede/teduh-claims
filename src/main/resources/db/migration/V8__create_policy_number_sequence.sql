-- Numbers for policies bought in the app (TTP-26-010000 and up). A sequence, so two customers buying at
-- the same moment can't get the same number. Starts above the seeded demo policies' numbers.
create sequence policy_number_seq start with 10000;
