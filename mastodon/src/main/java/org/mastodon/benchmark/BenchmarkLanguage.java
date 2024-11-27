package org.mastodon.benchmark;

/**
 * Represents and parses the language that instructs the benchmark what actions to take.
 * The syntax is:
 * Space separated sequence of ADDR_ACTION tokens.
 * ADDR := [bdv|ts][NUMBER|NOTHING]
 * NUMBER := any strictly-positive integer
 * NOTHING := '', just no character at all; means all windows are addressed
 * the addressed Mastodon window shall perform the action;
 *
 * ACTION := [TBFR][specificParameter]
 * where
 * T = switch to a timepoint, specificParameter = positive integer;
 * B = (applies only to BDV) switch to a bookmark (that needs to be set ahead in the project), specificParam = bookmark key;
 * F = focus (a Mastodon specific action) on a spot whose label is the specificParam;
 * R = (applies only to BDV) rotate, specificParam is positive integer saying in how many steps should a full rotation be done;
 * Z = (applies only to TS) moves between two views/bookmarks, specificParam is bookmark key, another one, and number of steps
 */
public class BenchmarkLanguage {
	public BenchmarkLanguage(final String theQuery) {
		//TODO: throw when split fails
		tokens = theQuery.split(" ");

		currentTokenIdx = 0;
		if (isTokenAvailable()) prepareToken();
	}

	private final String[] tokens;
	private int currentTokenIdx;
	private String curWindows, curAction;

	private void prepareToken() {
		//TODO: throw when split fails
		String[] s = tokens[currentTokenIdx].split("_");
		curWindows = s[0];
		curAction = s[1];
	}

	public boolean isTokenAvailable() {
		return currentTokenIdx < tokens.length;
	}

	public void moveToNextToken() {
		currentTokenIdx++;
		if (isTokenAvailable()) prepareToken();
	}

	public String getCurrentToken() {
		return isTokenAvailable() ? tokens[currentTokenIdx] : null;
	}

	// =========================================================================
	public enum WindowType {
		BDV, TS;
		int getLength() {
			return this == WindowType.TS ? 2 : 3;
		}
	}

	public enum ActionType { T, B, F, R, Z }

	public WindowType getCurrentWindowType() {
		if (curWindows.startsWith("BDV") || curWindows.startsWith("bdv")) return WindowType.BDV;
		if (curWindows.startsWith("TS") || curWindows.startsWith("ts")) return WindowType.TS;
		throw new IllegalArgumentException("Don't recognize the window address in the current token '"+getCurrentToken()+"'");
	}

	/**
	 * @return -1 if all windows are addressed, otherwise the strictly-positive window index is returned (1-based)
	 */
	public int getCurrentWindowNumber() {
		String numStr = curWindows.substring(getCurrentWindowType().getLength());
		if (numStr.isEmpty()) return -1;
		return Integer.parseInt(numStr);
		//TODO: throw on parsing error
	}

	public ActionType getCurrentAction() {
		char a = curAction.charAt(0);
		if (a == 'T') return ActionType.T;
		if (a == 'B') return ActionType.B;
		if (a == 'F') return ActionType.F;
		if (a == 'R') return ActionType.R;
		if (a == 'Z') return ActionType.Z;
		throw new IllegalArgumentException("Don't recognize the action in the current token '"+getCurrentToken()+"'");
	}

	public int getTimepoint() {
		return Integer.parseInt(curAction.substring(1));
		//TODO: throw on parsing error
		//TODO: throw if not long enough
	}
	public char getBookmarkKey() {
		return curAction.charAt(1);
		//TODO: throw if not long enough
	}
	public String getSpotLabel() {
		return curAction.substring(1);
		//TODO: throw if not long enough
	}
	public int getFullRotationSteps() {
		return Integer.parseInt(curAction.substring(1));
		//TODO: throw on parsing error
		//TODO: throw on negative or zero
	}
	public char getFromBookmark() {
		return curAction.split("-")[0].charAt(1);
		//NB: skip over the command itself, here 'Z'
	}
	public char getToBookmark() {
		return curAction.split("-")[1].charAt(0);
	}
	public int getFromToSteps() {
		return Integer.parseInt(curAction.split("-")[2]);
	}
}
