package org.mastodon.benchmark.windows;

import org.mastodon.mamut.views.trackscheme.MamutViewTrackScheme;
import org.mastodon.views.trackscheme.ScreenTransform;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.InputMismatchException;
import java.util.Scanner;

public class TrackSchemeBookmarks {
	public TrackSchemeBookmarks(final MamutViewTrackScheme registerForThisTS) {
		associatedTS = registerForThisTS;
		bookmarks = new ScreenTransform[MAX_BOOKMARKS];
		for (int i = 0; i < bookmarks.length; ++i) bookmarks[i] = new ScreenTransform();
		installKeys();
	}

	public TrackSchemeBookmarks(final MamutViewTrackScheme registerForThisTS,
	                            final File useThisDefaultStorage) {
		this(registerForThisTS);
		this.bookmarksFile = useThisDefaultStorage;
	}

	private final MamutViewTrackScheme associatedTS;

	// ================== Bookmarks ==================
	public static final int MAX_BOOKMARKS = 9;
	private final ScreenTransform[] bookmarks;
	public File bookmarksFile = new File("tsb.txt");

	public void loadBookmarksFromFile(final File bookmarksFile) {
		try (Scanner scanner = new Scanner(bookmarksFile))
		{
			for (ScreenTransform st : bookmarks) {
				st.set( scanner.nextDouble(),
						scanner.nextDouble(),
						scanner.nextDouble(),
						scanner.nextDouble(),
						scanner.nextInt(),
						scanner.nextInt() );
			}
		} catch (FileNotFoundException|InputMismatchException e) {
			System.out.println("Reading file error: "+e.getMessage());
		}
	}

	public void saveBookmarksToFile(final File bookmarksFile) {
		try (PrintWriter writer = new PrintWriter(bookmarksFile))
		{
			for (ScreenTransform st : bookmarks) {
				writer.println( st.getMinX() + " "
						+ st.getMaxX() + " "
						+ st.getMinY() + " "
						+ st.getMaxY() + " "
						+ st.getScreenWidth() + " "
						+ st.getScreenHeight() );
			}
		} catch (FileNotFoundException e) {
			System.out.println("Reading file error: "+e.getMessage());
		}
	}

	public void setBookmark(final int bookmarkIndex) {
		associatedTS.getFrame().getTrackschemePanel().getScreenTransform().get(bookmarks[bookmarkIndex]);
	}

	public void applyBookmark(final int bookmarkIndex) {
		associatedTS.getFrame().getTrackschemePanel().getScreenTransform().set(bookmarks[bookmarkIndex]);
	}

	public ScreenTransform getBookmark(final int bookmarkIndex) {
		return bookmarks[bookmarkIndex];
	}

	// ================== Behaviours ==================
	private final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );

	void installKeys() {
		behaviours.install(associatedTS.getFrame().getTriggerbindings(), "TS bookmarks" );

		for (int i = 0; i < bookmarks.length; ++i) {
			final int j = i+1;
			behaviours.behaviour( (ClickBehaviour) (x, y) -> {
					System.out.println("Saving a TS view #"+j);
					setBookmark(j-1);
				}, "ts_store_bookmark"+j, "shift|"+j );
			behaviours.behaviour( (ClickBehaviour) (x, y) -> {
					System.out.println("Switching TS to view #"+j);
					applyBookmark(j-1);
				}, "ts_recall_bookmark"+j, String.valueOf(j) );
		}

		behaviours.behaviour((ClickBehaviour) (x, y) -> {
			System.out.println("Saving bookmarks to file "+bookmarksFile.getAbsolutePath());
			saveBookmarksToFile(bookmarksFile);
		}, "ts_save_bookmarks", "shift|Q");
		behaviours.behaviour((ClickBehaviour) (x, y) -> {
			System.out.println("Loading bookmarks from file "+bookmarksFile.getAbsolutePath());
			loadBookmarksFromFile(bookmarksFile);
		}, "ts_load_bookmarks", "Q");
	}
}
