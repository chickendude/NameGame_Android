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
import android.view.animation.DecelerateInterpolator;
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
	public static final String FRAG_TAG = "NameGameFragmentTag";

	private static final Interpolator OVERSHOOT = new OvershootInterpolator();
	private static final Interpolator DECELERATE = new DecelerateInterpolator();

	private static final String SAVE_PEOPLE = "save_people";
	private static final String SAVE_TEST_SET = "save_test_set";
	private static final String SAVE_TEST_ANSWER = "save_test_answer";
	private static final String SAVE_NUM_QUESTIONS = "save_num_questions";
	private static final String SAVE_CORRECT_ANSWERS = "save_correct_answers";
	private static final String SAVE_TIME_ELAPSED = "save_time_elapsed";
	private static final String SAVE_FACE_VISIBLE_STATUS = "save_face_visible_status";
	private static final String SAVE_TIME_LIMIT = "save_time_limit";

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
	TimerRunnable timerRunnable;

	// Views
	private RelativeLayout layout;
	private TextView title;
	private TextView questionAttempts;
	private TextView timerSeconds;
	private ViewGroup container;
	private CardView responseContainer;
	private TextView responseMessage;
	private ProgressBar timerProgress;
	private ProgressBar progressBar;
	private List<ImageView> faces = new ArrayList<>(6);

	// Objects
	private List<Person> people;
	private List<Person> testSet;
	private Person testAnswer;
	private int numQuestions;
	private int correctAnswers;
	private long questionStartTime;
	private int facesLoaded;
	private boolean[] visibleFacesStatus;
	private int totalQuestions;
	private String nameFilter;
	private int timeLimit;
	private long timeElapsed; // keep track of time across orientation changes
	private long timePassed; // used alongside time limit

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
		progressBar = view.findViewById(R.id.progressBar);

		//Hide the views until data loads
		progressBar.setScaleX(0);
		progressBar.setScaleY(0);
		responseContainer.setAlpha(0);
		responseContainer.setScaleX(0);
		responseContainer.setScaleY(0);
		responseContainer.bringToFront();
		title.setText("");

		// set up total number of questions to ask
		totalQuestions = 10;
		Bundle bundle = getArguments();
		if (bundle != null) {
			if (bundle.getBoolean(NameGameLandingFragment.EXTRA_IS_INFINITE, false))
				totalQuestions = 999999;
			nameFilter = bundle.getString(NameGameLandingFragment.EXTRA_FILTER, null);
			timeLimit = bundle.getInt(NameGameLandingFragment.EXTRA_TIME_LIMIT, 0);
		}

		// load face ImageViews into list
		int n = container.getChildCount();
		for (int i = 0; i < n; i++) {
			ImageView face = (ImageView) container.getChildAt(i);
			faces.add(face);
		}

		// reload saved data on orientation change, etc.
		if (savedInstanceState != null) {
			people = savedInstanceState.getParcelableArrayList(SAVE_PEOPLE);
			testSet = savedInstanceState.getParcelableArrayList(SAVE_TEST_SET);
			visibleFacesStatus = savedInstanceState.getBooleanArray(SAVE_FACE_VISIBLE_STATUS);
			numQuestions = savedInstanceState.getInt(SAVE_NUM_QUESTIONS);
			correctAnswers = savedInstanceState.getInt(SAVE_CORRECT_ANSWERS);
			testAnswer = savedInstanceState.getParcelable(SAVE_TEST_ANSWER);
			timeElapsed = savedInstanceState.getLong(SAVE_TIME_ELAPSED);
			timeLimit = savedInstanceState.getInt(SAVE_TIME_LIMIT);
			loadTestSet();
		} else {
			numQuestions = 0;
			correctAnswers = 0;
			timeElapsed = 0;
			// load values from API
			profilesRepository.register(this);
		}

		if (timeLimit != 0)
			timerProgress.setMax(timeLimit * 10);
		else
			timerProgress.setMax(100);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putParcelableArrayList(SAVE_PEOPLE, (ArrayList) people);
		outState.putParcelableArrayList(SAVE_TEST_SET, (ArrayList) testSet);
		outState.putBooleanArray(SAVE_FACE_VISIBLE_STATUS, visibleFacesStatus);
		outState.putParcelable(SAVE_TEST_ANSWER, testAnswer);
		outState.putInt(SAVE_NUM_QUESTIONS, numQuestions);
		outState.putInt(SAVE_CORRECT_ANSWERS, correctAnswers);
		outState.putLong(SAVE_TIME_ELAPSED, System.currentTimeMillis() - questionStartTime);
		outState.putInt(SAVE_TIME_LIMIT, timeLimit);
	}

	/**
	 * A method for setting the images from people into the imageviews
	 */
	private void setImages(List<ImageView> faces, List<Person> people) {
		animateProgressBar(1f);
		int imageSize = (int) Ui.convertDpToPixel(100, getContext());
		int n = faces.size();
		facesLoaded = 0;

		// keep track of which faces have been hidden
		if (visibleFacesStatus == null) {
			visibleFacesStatus = new boolean[]{true, true, true, true, true, true};
		}

		// load images into faces
		for (int i = 0; i < n; i++) {
			ImageView face = faces.get(i);

			//Hide the views until data loads
			face.setOnClickListener(null);
			face.setScaleX(0);
			face.setScaleY(0);
			face.setBackground(null);

			final Person person = people.get(i);
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
							Log.d(TAG, "Error loading image");
						}
					});
		}

		// set up background timer
		if (timerRunnable != null)
			timerRunnable.stop();
		if (timerThread != null)
			timerThread.interrupt();
		timerRunnable = new TimerRunnable();
		timerThread = new Thread(timerRunnable, "NameGameTimer");
		timerThread.start();
	}

	private void animateProgressBar(float scale) {
		progressBar.animate().scaleY(scale).scaleX(scale).setInterpolator(DECELERATE);
	}

	private int updateProgress(int progress) {
		timerProgress.setProgress(progress);
		int max = timerProgress.getMax();
		int secondsLeft = progress == max ? max / 10 : (progress / 10) + 1;
		String seconds = secondsLeft == 1 ? "second" : "seconds";
		timerSeconds.setText(String.format(Locale.getDefault(), "%d %s", secondsLeft, seconds));
		return secondsLeft;
	}


	@Override
	public void onPause() {
		super.onPause();
		if (timerRunnable != null)
			timerRunnable.stop();
		if (timerThread != null)
			timerThread.interrupt();
	}

	/**
	 * A method to animate the faces into view
	 */
	private void animateFacesIn() {
		animateProgressBar(0f);
		title.animate().alpha(1).start();
		for (int i = 0; i < faces.size(); i++) {
			if (visibleFacesStatus[i])
				faces.get(i).animate().scaleX(1).scaleY(1).alpha(1).setStartDelay(60 * i).setInterpolator(OVERSHOOT).start();
			final Person person = testSet.get(i);
			faces.get(i).setOnClickListener(view -> onPersonSelected(view, person));
		}
	}

	/**
	 * A method to handle when a person is selected
	 *
	 * @param view   The view that was selected
	 * @param person The person that was selected
	 */
	private void onPersonSelected(@NonNull View view, @NonNull Person person) {
		for (ImageView face : faces) {
			face.setOnClickListener(null);
		}
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
		timerRunnable.stop();
		if (isCorrect)
			correctAnswers++;
		else {
			int i = testSet.indexOf(testAnswer);
			faces.get(i).setBackgroundResource(R.drawable.correct_answer);
		}
		updateAttempts();

		if (timeLimit > 0) {
			// add the time taken to answer the question to timepassed
			timePassed = System.currentTimeMillis() - questionStartTime;

			// if we have reached the time limit, the game is over
			if (timePassed / 1000 >= timeLimit)
				gameOver();
		}

		// no extra onclicks and we need to read the entire layout for onclick once message shows up
		for (ImageView face : faces)
			face.setClickable(false);
		responseMessage.setText(getRandString(responses));
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
		if (numQuestions == totalQuestions)
			gameOver();

		// reset visible faces so that all will be visible
		visibleFacesStatus = null;

		// get our next set of people to test and choose an answer from among them
		testSet = listRandomizer.pickN(people, 6);
		testAnswer = listRandomizer.pickOne(testSet);
		loadTestSet();
		numQuestions++;
	}

	private void gameOver() {
		Log.d(TAG, "Game Over");
		getFragmentManager().popBackStack();
	}

	private void loadTestSet() {
		// create the question to display at the top
		String question = numQuestions == 1 ? QUESTIONS[0] : getRandString(QUESTIONS);
		String name = testAnswer.getFirstName() + " " + testAnswer.getLastName();
		title.setText(String.format(question, name));

		// create the stats below showing how many questions have been asked
		updateAttempts();

		// load images into imageviews
		setImages(faces, testSet);
	}

	private void updateAttempts() {
		String attempts = numQuestions == 0 ?
				"First Question!" :
				String.format(Locale.getDefault(), "%d/%d", correctAnswers, numQuestions);
		questionAttempts.setText(attempts);
	}

	private String getRandString(String[] strings) {
		return listRandomizer.pickOne(Arrays.asList(strings));
	}

	@Override
	public void onLoadFinished(@NonNull List<Person> people) {
		// apply a name filter if we have one
		if (nameFilter != null) {
			this.people = new ArrayList<>();
			for (Person person : people) {
				if (person.getFirstName().startsWith(nameFilter))
					this.people.add(person);
			}
		} else {
			this.people = people;
		}
		getNextTestSet();
	}

	@Override
	public void onError(@NonNull Throwable error) {
		Toast.makeText(getContext(), "There was an error getting the list of names!", Toast.LENGTH_LONG).show();
		Log.d(TAG, error.getLocalizedMessage());
	}

	/*
	* Runnable that counts down and shows the progress in the form of a ProgressBar
	* and updates the number of seconds left.
	*/
	class TimerRunnable implements Runnable {
		private volatile boolean exit = false;
		private NameGameFragment fragment;

		@Override
		public void run() {
			// wait until all faces have been loaded before starting the timer
			while (facesLoaded < 6) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// display the faces all at once
			handler.post(() -> animateFacesIn());

			facesLoaded = 0;
			for (boolean isVisible : visibleFacesStatus) {
				if (isVisible)
					facesLoaded++;
			}

			// set timer start time
			questionStartTime = System.currentTimeMillis() - timePassed - timeElapsed;
			timeElapsed = 0;
			do {
				final int progress = timerProgress.getMax() - ((int) ((System.currentTimeMillis() - questionStartTime) / 100));

				// update UI
				handler.post(() -> {
					int secondsLeft = updateProgress(progress);

					// gradually remove extra faces when we aren't doing a time attack
					if (timeLimit == 0 && ((secondsLeft + 1) / 2) <= facesLoaded - 2 && facesLoaded > 2) {
						facesLoaded--;
						List<ImageView> visibleFaces = new ArrayList<>();

						// add all images that are still visible
						ImageView correctFace = faces.get(testSet.indexOf(testAnswer));
						for (ImageView face : faces) {
							if (face.getAlpha() != 0 && face != correctFace)
								visibleFaces.add(face);
						}

						ImageView face = listRandomizer.pickOne(visibleFaces);
						visibleFacesStatus[faces.indexOf(face)] = false;
						face.setOnClickListener(null);
						face.animate().alpha(0).setInterpolator(DECELERATE);
					}
				});
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			} while (timerProgress.getProgress() > 0 && !exit);
			if (exit)
				return;
			// time's up!
			handler.postDelayed(() -> {
				if (exit)
					return;
				updateProgress(-10);
				answerSelected(TIMES_UP_RESPONSES, false);
			}, 200);
		}

		private void stop() {
			exit = true;
		}
	}
}
