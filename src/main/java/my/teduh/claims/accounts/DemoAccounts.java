package my.teduh.claims.accounts;

import java.util.UUID;

/**
 * The four demo people from the scope. Fixed ids, so the policies module can give Aisyah and Daniel their
 * policies, and the demo is the same every time it is reset.
 *
 * The password is not a secret. The "Try as customer" and "Try as agent" buttons send it, so it ends up in
 * the frontend code anyone can read. These accounts can only see demo data.
 */
public final class DemoAccounts {

	public static final UUID AISYAH = UUID.fromString("7ed00000-0000-4000-8000-000000000001");

	public static final UUID DANIEL = UUID.fromString("7ed00000-0000-4000-8000-000000000002");

	public static final UUID FARID = UUID.fromString("7ed00000-0000-4000-8000-000000000011");

	public static final UUID MEI_LING = UUID.fromString("7ed00000-0000-4000-8000-000000000012");

	public static final String PASSWORD = "teduh-demo";

	private DemoAccounts() {
	}

}
