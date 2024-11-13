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
		bookmarks = new ScreenTransform[9];
		for (int i = 0; i < bookmarks.length; ++i) bookmarks[i] = new ScreenTransform();
		installKeys(registerForThisTS);
	}

	public TrackSchemeBookmarks(final MamutViewTrackScheme registerForThisTS,
	                            final File useThisDefaultStorage) {
		this(registerForThisTS);
		this.bookmarksFile = useThisDefaultStorage;
	}

	// ================== Bookmarks ==================
	final ScreenTransform[] bookmarks;
	public File bookmarksFile = new File("tsb.txt");

	public void loadBookmarksFromFile(final File bookmarksFile) {
		try (Scanner scanner = new Scanner(bookmarksFile))
		{
			for (int i = 0; i < bookmarks.length; ++i) {
				bookmarks[i].set( scanner.nextDouble(),
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
			for (int i = 0; i < bookmarks.length; ++i) {
				ScreenTransform st = bookmarks[i];
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

	void setBookmark(final MamutViewTrackScheme associatedTS, final int bookmarkIndex) {
		System.out.println("Saving a TS view #"+(bookmarkIndex+1));
		associatedTS.getFrame().getTrackschemePanel().getScreenTransform().get(bookmarks[bookmarkIndex]);
	}

	void applyBookmark(final MamutViewTrackScheme associatedTS, final int bookmarkIndex) {
		System.out.println("Switching TS to view #"+(bookmarkIndex+1));
		associatedTS.getFrame().getTrackschemePanel().getScreenTransform().set(bookmarks[bookmarkIndex]);
	}

	// ================== Behaviours ==================
	final Behaviours behaviours = new Behaviours( new InputTriggerConfig() );

	void installKeys(final MamutViewTrackScheme registerForThisTS) {
		behaviours.install(registerForThisTS.getFrame().getTriggerbindings(), "TS bookmarks" );

		for (int i = 0; i < bookmarks.length; ++i) {
			final int j = i+1;
			Integer index = new Integer(i);
			behaviours.behaviour((ClickBehaviour) (x, y) -> setBookmark(registerForThisTS, index), "ts_store_bookmark"+j, "shift|"+j);
			behaviours.behaviour((ClickBehaviour) (x, y) -> applyBookmark(registerForThisTS, index), "ts_recall_bookmark"+j, String.valueOf(j));
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
