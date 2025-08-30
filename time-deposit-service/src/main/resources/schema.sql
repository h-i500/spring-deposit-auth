-- 既存の CREATE TABLE を以下のように更新
CREATE TABLE IF NOT EXISTS time_deposits (
  id UUID PRIMARY KEY,
  owner TEXT NOT NULL,
  principal NUMERIC(19,2) NOT NULL,
  annual_rate NUMERIC(9,6) NOT NULL,
  term_days INT NOT NULL,
  start_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  maturity_at TIMESTAMPTZ NOT NULL,
  status TEXT NOT NULL DEFAULT 'OPEN',
  payout_amount NUMERIC(19,2),
  payout_account UUID,
  closed_at TIMESTAMPTZ,
  CONSTRAINT time_deposits_status_chk CHECK (status IN ('OPEN','CLOSING','CLOSED'))
);
