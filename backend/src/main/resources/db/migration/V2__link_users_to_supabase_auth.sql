ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS auth_user_id UUID;

UPDATE app_user
SET auth_user_id = id
WHERE auth_user_id IS NULL;

ALTER TABLE app_user
    ALTER COLUMN auth_user_id SET NOT NULL,
    ALTER COLUMN password_hash DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uq_app_user_auth_user_id'
          AND conrelid = 'app_user'::regclass
    ) THEN
        ALTER TABLE app_user
            ADD CONSTRAINT uq_app_user_auth_user_id UNIQUE (auth_user_id);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS ix_user_tenant_role_active_membership
    ON user_tenant_role (user_id, tenant_id)
    WHERE revoked_at IS NULL;
