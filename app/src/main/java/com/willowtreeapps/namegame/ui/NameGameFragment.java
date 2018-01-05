package com.willowtreeapps.namegame.ui;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.willowtreeapps.namegame.R;
import com.willowtreeapps.namegame.core.ListRandomizer;
import com.willowtreeapps.namegame.core.NameGameApplication;
import com.willowtreeapps.namegame.network.api.ProfilesRepository;
import com.willowtreeapps.namegame.network.api.model.Person;
import com.willowtreeapps.namegame.util.CircleBorderTransform;
import com.willowtreeapps.namegame.util.Ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class NameGameFragment extends Fragment implements ProfilesRepository.Listener {
	private static final String TAG = NameGameFragment.class.getSimpleName();

	private static final Interpolator OVERSHOOT = new OvershootInterpolator();
	private static final String[] QUESTIONS = {
			"Can you tell me who %s is?",
			"Who is %s?",
			"Show me who %s is!",
			"What about %s?",
			"And how about %s?",
			"And %s?"};

	private static final String[] CORRECT_RESPONSES = {
			"That was correct!",
			"Look at you go!",
			"Well done!",
			"Wow! Can you do that again?",
			"You're friggin' awesome!",
			"I can't believe it! Amazing!",
			"How'd you get THAT right?!"};

	private static final String[] INCORRECT_RESPONSES = {
			"Sorry, that was incorrect!",
			"Nope!",
			"Aww... maybe next time :(",
			"Could you try a little harder, please?",
			"Ummm... no.",
			"Not even close!",
			"That was not correct."};

	private static final String[] TIMES_UP_RESPONSES = {
			"Times up!",
			"Try to hurry up some next time!",
			"You were too slow!",
			"Would you make up your mind already?",
			"You ran out of time!",
			"BING! BING! BING!\n(Time's up)"};

	@Inject
	ListRandomizer listRandomizer;
	@Inject
	Picasso picasso;
	@Inject
	ProfilesRepository profilesRepository;
	Thread timerThread;
	Handler handler;

	// Views
	private RelativeLayout layout;
	private TextView title;
	private TextView questionAttempts;
	private TextView timerSeconds;
	private ViewGroup container;
	private CardView responseContainer;
	private TextView responseMessage;
	private ProgressBar timerProgress;
	private List<ImageView> faces = new ArrayList<>(6);

	// Objects
	private List<Person> people;
	private List<Person> testSet;
	private Person testAnswer;
	private int numQuestions;
	private int correctAnswers;
	private long questionStartTime;
	private int facesLoaded;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		NameGameApplication.get(getActivity()).component().inject(this);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.name_game_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		handler = new Handler();

		layout = view.findViewById(R.id.layout);
		title = view.findViewById(R.id.title);
		questionAttempts = view.findViewById(R.id.questionAttempts);
		timerProgress = view.findViewById(R.id.timerProgress);
		timerSeconds = view.findViewById(R.id.timerSeconds);
		container = view.findViewById(R.id.face_container);
		responseContainer = view.findViewById(R.id.responseContainer);
		responseMessage = view.findViewById(R.id.responseMessage);

		numQuestions = 0;
		correctAnswers = 0;

		//Hide the views until data loads
		responseContainer.setAlpha(0);
		responseContainer.setScaleX(0);
		responseContainer.setScaleY(0);
		title.setText("");

		int n = container.getChildCount();
		for (int i = 0; i < n; i++) {
			ImageView face = (ImageView) container.getChildAt(i);
			faces.add(face);
		}

		// load values from API
		profilesRepository.register(this);
	}

	/**
	 * A method for setting the images from people into the imageviews
	 */
	private void setImages(List<ImageView> faces, List<Person> people) {
		int imageSize = (int) Ui.convertDpToPixel(100, getContext());
		int n = faces.size();
		facesLoaded = 0;

		for (int i = 0; i < n; i++) {
			ImageView face = faces.get(i);

			//Hide the views until data loads
			face.setScaleX(0);
			face.setScaleY(0);
			face.setBackground(null);

			final Person person = people.get(i);
			face.setOnClickListener(imageView -> onPersonSelected(imageView, person));
			picasso.load("http:" + person.getHeadshot().getUrl())
					.placeholder(R.drawable.ic_face_white_48dp)
					.resize(imageSize, imageSize)
					.transform(new CircleBorderTransform(getContext()))
					.into(face, new Callback() {
						@Override
						public void onSuccess() {
							facesLoaded++;
						}

						@Override
						public void onError() {

						}
					});
		}
		timerThread = new Thread(timerRunnable, "NameGameTimer");
		timerThread.start();
	}

	/*
	* Runnable that counts down and shows the progress in the form of a ProgressBar
	* and updates the number of seconds left.
	*/
	Runnable timerRunnable = () -> {
		// wait until all faces have been loaded before starting the timer
		while (facesLoaded < 6) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// display the faces all at once
		handler.post(this::animateFacesIn);

		// set timer start time
		questionStartTime = System.currentTimeMillis();
		do {
			final int progress = 100 - ((int) ((System.currentTimeMillis() - questionStartTime) / 100));

			// update UI
			handler.post(() -> {
				timerProgress.setProgress(progress);
				int secondsLeft = progress == 100 ? 10 : (progress / 10) + 1;
				String seconds = secondsLeft == 1 ? "second" : "seconds";
				timerSeconds.setText(String.format(Locale.getDefault(), "%d %s", secondsLeft, seconds));
			});
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
		} while (timerProgress.getProgress() > 0 && !timerThread.isInterrupted());
		// time's up!
		handler.post(() -> answerSelected(TIMES_UP_RESPONSES, false));
	};


	/**
	 * A method to animate the faces into view
	 */
	private void animateFacesIn() {
		title.animate().alpha(1).start();
		for (int i = 0; i < faces.size(); i++) {
			ImageView face = faces.get(i);
			face.animate().scaleX(1).scaleY(1).setStartDelay(60 * i).setInterpolator(OVERSHOOT).start();
		}
	}

	/**
	 * A method to handle when a person is selected
	 *
	 * @param view   The view that was selected
	 * @param person The person that was selected
	 */
	private void onPersonSelected(@NonNull View view, @NonNull Person person) {
		if (person.equals(testAnswer)) {
			view.setBackgroundResource(R.drawable.correct_answer);
			answerSelected(CORRECT_RESPONSES, true);
		} else {
			view.setBackgroundResource(R.drawable.wrong_answer);
			answerSelected(INCORRECT_RESPONSES, false);
		}
	}

	/*
	* What to do when an answer has been chosen (or time has run out).
	* Displays a message and highlights the correct answer if chosen answer was incorrect.
	*/
	private void answerSelected(String[] responses, boolean isCorrect) {
		if (timerThread != null) {
			timerThread.interrupt();
		}
		if (isCorrect)
			correctAnswers++;
		else {
			int i = testSet.indexOf(testAnswer);
			faces.get(i).setBackgroundResource(R.drawable.correct_answer);
		}

		for (ImageView face : faces)
			face.setClickable(false);
		responseMessage.setText(getRandString(responses));
		responseContainer.setAlpha(1);
		responseContainer.animate().scaleX(1).scaleY(1).alpha(1).setInterpolator(OVERSHOOT).start();
		View.OnClickListener onClickListener = v -> {
			v.setOnTouchListener(null);
			responseContainer.animate().scaleX(0).scaleY(0).alpha(0).setInterpolator(OVERSHOOT).start();
			getNextTestSet();
		};
		layout.setOnClickListener(onClickListener);
	}

	/**
	 * Gets the next question to ask the user.
	 */
	private void getNextTestSet() {
		numQuestions++;

		// get our next set of people to test and choose an answer from among them
		testSet = listRandomizer.pickN(people, 6);
		testAnswer = listRandomizer.pickOne(testSet);

		// create the question to display at the top
		String question = numQuestions == 1 ? QUESTIONS[0] : getRandString(QUESTIONS);
		String name = testAnswer.getFirstName() + " " + testAnswer.getLastName();
		title.setText(String.format(question, name));

		// create the stats below showing how many questions have been asked
		String attempts = numQuestions == 1 ?
				"First Question!" :
				String.format(Locale.getDefault(), "%d/%d", correctAnswers, numQuestions - 1);
		questionAttempts.setText(attempts);

		// load images into imageviews
		setImages(faces, testSet);
	}

	private String getRandString(String[] strings) {
		return listRandomizer.pickOne(Arrays.asList(strings));
	}

	@Override
	public void onLoadFinished(@NonNull List<Person> people) {
		Log.d(TAG, people.toString());
		this.people = people;
		getNextTestSet();
	}

	@Override
	public void onError(@NonNull Throwable error) {
		Toast.makeText(getContext(), "There was an error getting the list of names!", Toast.LENGTH_LONG).show();
		Log.d(TAG, error.getLocalizedMessage());
	}
}
