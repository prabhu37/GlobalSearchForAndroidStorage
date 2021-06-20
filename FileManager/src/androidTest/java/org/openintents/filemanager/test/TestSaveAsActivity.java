package org.openintents.filemanager.test;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.InjectEventSecurityException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.rule.ActivityTestRule;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openintents.filemanager.R;
import org.openintents.filemanager.SaveAsActivity;

import java.io.File;
import java.io.IOException;

public class TestSaveAsActivity extends BaseTestFileManager {

    @Rule
    public SaveAsActivityTestRule rule = new SaveAsActivityTestRule();

    @BeforeClass
    public static void setup() throws IOException {
        sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath() + '/';
        createDirectory(sdcardPath + TEST_DIRECTORY);
        createFile(sdcardPath + "oi-filemanager-tests/oi-to-open.txt", "bbb");
    }

    @Test
    public void testIntentSaveAs() {

        Espresso.onView(ViewMatchers.withHint(R.string.filename_hint)).perform(ViewActions.closeSoftKeyboard(), ViewActions.replaceText("oi-target.txt"));
        Espresso.onView(ViewMatchers.withHint(R.string.filename_hint)).perform(ViewActions.actionWithAssertions(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return ViewMatchers.isDisplayed();
            }

            @Override
            public String getDescription() {
                return null;
            }

            @Override
            public void perform(UiController uiController, View view) {
                try {
                    sendKeyEvent(uiController);
                } catch (InjectEventSecurityException e) {
                    e.printStackTrace();
                }
            }

            private boolean sendKeyEvent(UiController controller)
                    throws InjectEventSecurityException {

                boolean injected = false;
                long eventTime = SystemClock.uptimeMillis();
                for (int attempts = 0; !injected && attempts < 4; attempts++) {
                    injected = controller.injectKeyEvent(new KeyEvent(eventTime,
                            eventTime,
                            KeyEvent.ACTION_DOWN,
                            KeyEvent.KEYCODE_ENTER,
                            0,
                            0));
                }
                return injected;
            }
        }));
        //onView(withId(R.id.pickbar_button)).perform(click());

        Assert.assertThat(new File(sdcardPath + "oi-filemanager-tests/oi-target.txt").exists(), Matchers.is(true));
    }

    private static class SaveAsActivityTestRule extends ActivityTestRule<SaveAsActivity> {
        public SaveAsActivityTestRule() {
            super(SaveAsActivity.class);
        }

        @Override
        protected Intent getActivityIntent() {
            Uri uri = Uri.parse("file://" + sdcardPath + "oi-filemanager-tests/oi-to-open.txt");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.setComponent(new ComponentName(InstrumentationRegistry.getTargetContext(), SaveAsActivity.class));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }
    }
}
