package org.openintents.filemanager.test;

import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.BoundedMatcher;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.openintents.filemanager.bookmarks.BookmarkListAdapter;
import org.openintents.filemanager.files.FileHolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class BaseTestFileManager {
    public static final String TEST_DIRECTORY = "oi-filemanager-tests";
    protected static String sdcardPath;

    protected static void cleanDirectory(File file) {
        if (!file.exists()) return;
        for (String name : file.list()) {
            if (!name.startsWith("oi-") && !name.startsWith(".oi-")) {
                throw new RuntimeException(file + " contains unexpected file");
            }
            File child = new File(file, name);
            if (child.isDirectory())
                cleanDirectory(child);
            else
                child.delete();
        }
        file.delete();
        if (file.exists()) {
            throw new RuntimeException("Deletion of " + file + " failed");
        }
    }

    protected static void createFile(String path, String content) throws IOException {
        File file = new File(path);
        OutputStreamWriter wr = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        wr.write(content);
        wr.close();
    }

    protected static void createDirectory(String path) throws IOException {
        File file = new File(path);
        file.mkdir();
        if (!file.exists())
            throw new IOException("Creation of " + path + " failed");
    }

    public static Matcher<View> withResourceName(String resourceName) {
        return withResourceName(Matchers.is(resourceName));
    }

    public static Matcher<View> withResourceName(final Matcher<String> resourceNameMatcher) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with resource name: ");
                resourceNameMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                int id = view.getId();
                return id != View.NO_ID && id != 0 && view.getResources() != null
                        && resourceNameMatcher.matches(view.getResources().getResourceName(id));
            }
        };
    }

    public static Matcher<View> withAdaptedData(final Matcher<Object> dataMatcher) {
        return new TypeSafeMatcher<View>() {

            @Override
            public void describeTo(Description description) {
                description.appendText("with class name: ");
                dataMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                if (!(view instanceof AdapterView)) {
                    return false;
                }

                @SuppressWarnings("rawtypes")
                Adapter adapter = ((AdapterView) view).getAdapter();
                for (int i = 0; i < adapter.getCount(); i++) {
                    if (dataMatcher.matches(adapter.getItem(i))) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    protected void deleteDirectory(String path) {
        File file = new File(path);
        if (file.exists())
            if (file.isDirectory())
                cleanDirectory(file);
        file.delete();
    }

    protected void clickOnTestDirectory() {
        clickOnFile(TEST_DIRECTORY);
    }

    protected void clickOnFile(String filename) {
        Espresso.onData(Matchers.allOf(Matchers.instanceOf(FileHolder.class), hasName(filename))).perform(ViewActions.click(ViewActions.pressBack()));
    }

    protected void longClickOnFile(String filename) {
        Espresso.onData(Matchers.allOf(Matchers.instanceOf(FileHolder.class), hasName(filename))).perform(ViewActions.longClick());
    }

    protected void longClickOnBookmark(String filename) {
        Espresso.onData(Matchers.allOf(Matchers.instanceOf(BookmarkListAdapter.Bookmark.class), hasBookmarkName(filename))).perform(ViewActions.longClick());
    }

    protected void checkFile(String filename, ViewAssertion viewAssertion) {
        Espresso.onData(hasName(filename)).check(viewAssertion);
    }

    protected BoundedMatcher<Object, FileHolder> hasName(final String filename) {
        return new BoundedMatcher<Object, FileHolder>(FileHolder.class) {

            @Override
            public void describeTo(Description description) {
                description.appendText("with name " + filename);
            }

            @Override
            protected boolean matchesSafely(FileHolder item) {
                return filename.equals(item.getName());
            }
        };
    }

    protected BoundedMatcher<Object, BookmarkListAdapter.Bookmark> hasBookmarkName(final String filename) {
        return new BoundedMatcher<Object, BookmarkListAdapter.Bookmark>(BookmarkListAdapter.Bookmark.class) {

            @Override
            public void describeTo(Description description) {
                description.appendText("with name " + filename);
            }

            @Override
            protected boolean matchesSafely(BookmarkListAdapter.Bookmark item) {
                return filename.equals(item.getName());
            }
        };
    }
}
