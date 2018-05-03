package edu.rosehulman.me435;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import android.app.Activity;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;

/**
 * This class is a simple helper for doing TextToSpeech.  Create an instance
 * of this class then use the speak command.  In addition to speech there are
 * a few additional helper methods for ringtones and notification beeps.
 * 
 * @author fisherds@gmail.com (Dave Fisher)
 */
public class TextToSpeechHelper implements OnInitListener {

	/** TAG used for logging initialization error messages (if necessary). */
	public static final String TAG = TextToSpeechHelper.class.getSimpleName();
	
	/** Reference to use Androids TextToSpeech engine. */
	private TextToSpeech mTts;
	
	/** Constructor */
	public TextToSpeechHelper(Activity mainActivity) {
		mTts = new TextToSpeech(mainActivity, this);
	}


	/**
	 * Stop the TextToSpeech engine.
	 * Within the Activity's onDestroy method call this method.
	 */
	public void shutdown() {
		if (mTts != null) {
			mTts.stop();
			mTts.shutdown();
		}
	}
	

	/**
	 * Call this method with a String to speak that text.
	 * 
	 * @param messageToSpeak String to speak.
	 */
	public void speak(String messageToSpeak) {
		mTts.speak(messageToSpeak, TextToSpeech.QUEUE_FLUSH, null);
	}


	// ------------- OnInitListener method ----------------------------
	/**
	 * Method called when the TextToSpeech engine starts to determine if it is
	 * successful.
	 */
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = mTts.setLanguage(Locale.US);
			if (result == TextToSpeech.LANG_MISSING_DATA
					|| result == TextToSpeech.LANG_NOT_SUPPORTED) {
				Log.e(TAG, "This Language is not supported");
			} else {
				Log.d(TAG, "TTS Ready");
			}
		} else {
			Log.e(TAG, "TTS Initilization Failed!");
		}
	}
}
