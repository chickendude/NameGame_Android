package com.willowtreeapps.namegame.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.Slide;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.willowtreeapps.namegame.R;
import com.willowtreeapps.namegame.core.NameGameApplication;
import com.willowtreeapps.namegame.network.api.ProfilesRepository;
import com.willowtreeapps.namegame.network.api.model.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class NameGameLandingFragment extends Fragment implements ProfilesRepository.Listener {
	private static final String TAG = NameGameLandingFragment.class.getSimpleName();
	private static final String SAVE_CURRENT_SELECTION = "save_current_selection";
	public static final String EXTRA_IS_INFINITE = "extra_is_infinite";
	public static final String EXTRA_FILTER = "extra_filter";
	public static final String EXTRA_TIME_LIMIT = "extra_time_limit";
	public static final String EXTRA_RANGE_START = "extra_range_start";
	public static final String EXTRA_RANGE_END = "extra_range_end";

	String[] GAME_MODES = {
			"The one and only original!",
			"To infinity and beyond!",
			"Did you say \"Mat(t)\"?",
			"Roadrunner",
			"Let's focus!"
	};

	String[] GAME_MODE_DESCRIPTIONS = {
			"Answer a set of 10 questions within the time limit and score big points!",
			"Answer as many questions as you like until you've learned them all or get bored (like that'll ever happen)!",
			"Did you know that roughly 90 percent of your coworkers have some variation of the name \"Matt\"? Neither did we, but now you can focus on learning their names!",
			"How many faces can you recognize in one minute?",
			"Pick a subsection of your coworkers and focus on learning their names."
	};

	@Inject
	ProfilesRepository profilesRepository;

	// Views
	LinearLayout container;
	TextView description;
	Button playButton;
	List<Button> buttons;

	// Objects
	int currentSelection;
	int numEntries;


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		NameGameApplication.get(getActivity()).component().inject(this);
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		try {
			profilesRepository.register(this);
		} catch(IllegalStateException ise) {
			Log.d(TAG, ise.getLocalizedMessage());
		}
		return inflater.inflate(R.layout.name_game_landing_fragment, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		container = view.findViewById(R.id.buttonContainer);
		description = view.findViewById(R.id.description);
		playButton = view.findViewById(R.id.playButton);

		// load two main colors
		int colorSecondary = ContextCompat.getColor(getContext(), R.color.colorSecondaryText);
		int colorPrimary = ContextCompat.getColor(getContext(), R.color.colorPrimaryText);

		// load the currently selected game when clicked
		playButton.setOnClickListener(this::loadGame);

		// create buttons from GAME_MODES string array
		buttons = new ArrayList<>(5);
		for (int i = 0; i < GAME_MODES.length; i++) {
			Button button = new Button(getContext());
			button.setText(GAME_MODES[i]);
			button.setTextColor(colorSecondary);
			button.setFocusable(true);
			button.setFocusableInTouchMode(true);
			final int position = i;
			button.setOnFocusChangeListener((v, hasFocus) -> {
				int color;
				if (hasFocus) {
					onButtonClick(v, position);
					color = colorPrimary;
				} else
					color = colorSecondary;
				button.setTextColor(color);
			});
			button.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.button));
			container.addView(button);
			buttons.add(button);
		}

		// check if we have a saved "currentSelection" to restore
		if (savedInstanceState != null) {
			currentSelection = savedInstanceState.getInt(SAVE_CURRENT_SELECTION, 0);
		}

		// load the first button or whatever was saved as the previous selection
		buttons.get(currentSelection).requestFocus();
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putInt(SAVE_CURRENT_SELECTION, currentSelection);
	}

	private void loadGame(View v) {
		switch (currentSelection) {
			case 0:
				startOriginal(false);
				break;
			case 1:
				startOriginal(true);
				break;
			case 2:
				startOriginal(false, "Matt");
				break;
			case 3:
				startOriginal(true, null, 60);
				break;
			case 4:
				startFocus();
				break;
		}
	}

	private void startFocus() {
		if (numEntries > 0) {
			List<String> ranges = new ArrayList<>();
			int start = 1;
			while (true) {
				int end = start + 19;
				if (end > numEntries)
					end = numEntries;
				ranges.add(String.format(Locale.getDefault(), "%d - %d", start, end));
				if (end == numEntries)
					break;
				start += 20;
			}

			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			builder.setTitle("Select a range to practice!");
			builder.setItems(ranges.toArray(new String[ranges.size()]), (dialog, which) -> {
				int startValue = which * 20;
				int endValue = startValue + 19;
				if (endValue >= numEntries)
					endValue = numEntries - 1;
				startRanged(startValue, endValue);
			});
			builder.show();
		} else {
			Toast.makeText(getContext(), "Employee list not loaded, unable to start!", Toast.LENGTH_SHORT).show();
		}

	}

	private void startRanged(int start, int end) {
		startOriginal(false, null, 0, start, end);
	}

	private void startOriginal(boolean isInfinite) {
		startOriginal(isInfinite, null, 0);
	}

	private void startOriginal(boolean isInfinite, String filter) {
		startOriginal(isInfinite, filter, 0);
	}

	private void startOriginal(boolean isInfinite, String filter, int timeLimit) {
		startOriginal(isInfinite, filter, timeLimit, 0, 0);
	}

	private void startOriginal(boolean isInfinite, String filter, int timeLimit, int start, int end) {
		NameGameFragment fragment = new NameGameFragment();

		// set up transitions
		fragment.setEnterTransition(new Slide(Gravity.RIGHT));
		setExitTransition(new Slide(Gravity.LEFT));

		// pass in arguments
		Bundle bundle = new Bundle();
		bundle.putBoolean(EXTRA_IS_INFINITE, isInfinite);
		bundle.putString(EXTRA_FILTER, filter);
		bundle.putInt(EXTRA_TIME_LIMIT, timeLimit);
		bundle.putInt(EXTRA_RANGE_START, start);
		bundle.putInt(EXTRA_RANGE_END, end);
		fragment.setArguments(bundle);

		// load fragment
		getFragmentManager().beginTransaction()
				.addToBackStack(null)
				.replace(R.id.container, fragment, NameGameFragment.FRAG_TAG)
				.commit();
	}

	/*
	* Method to update the game mode description when a button is clicked.
	*/
	public void onButtonClick(View view, int position) {
		currentSelection = position;
		description.setText(GAME_MODE_DESCRIPTIONS[position]);
	}

	@Override
	public void onLoadFinished(@NonNull List<Person> people) {
		numEntries = people.size();
	}

	@Override
	public void onError(@NonNull Throwable error) {
		Toast.makeText(getContext(), "Error loading employee list, \"Let's focus\" game will not work.", Toast.LENGTH_LONG).show();
	}
}