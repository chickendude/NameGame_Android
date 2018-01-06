package com.willowtreeapps.namegame.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.transition.Slide;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.willowtreeapps.namegame.R;
import com.willowtreeapps.namegame.core.NameGameApplication;

import java.util.ArrayList;
import java.util.List;

public class NameGameLandingFragment extends Fragment {
	private static final String SAVE_CURRENT_SELECTION = "save_current_selection";
	public static final String EXTRA_IS_INFINITE = "extra_is_infinite";
	private static final String TAG = NameGameLandingFragment.class.getSimpleName();
	LinearLayout container;

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

	// Views
	TextView description;
	Button playButton;
	List<Button> buttons;

	// Objects
	int currentSelection;


	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		NameGameApplication.get(getActivity()).component().inject(this);
	}


	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.name_game_landing_fragment, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		container = view.findViewById(R.id.buttonContainer);
		description = view.findViewById(R.id.description);
		playButton = view.findViewById(R.id.playButton);

		// load the currently selected game when clicked
		playButton.setOnClickListener(this::loadGame);

		// create buttons from GAME_MODES string array
		buttons = new ArrayList<>(5);
		for (int i = 0; i < GAME_MODES.length; i++) {
			Button button = new Button(getContext());
			button.setText(GAME_MODES[i]);
			button.setFocusable(true);
			button.setFocusableInTouchMode(true);
			final int position = i;
			button.setOnFocusChangeListener((v, hasFocus) -> {
				if (hasFocus) onButtonClick(v, position);
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
		switch(currentSelection) {
			case 0:
				startOriginal(false);
				break;
			case 1:
				startOriginal(true);
				break;
			case 2:
				break;
			case 3:
				break;
			case 4:
				break;
			case 5:
				break;
		}
	}

	private void startOriginal(boolean isInfinite) {
		NameGameFragment fragment = new NameGameFragment();

		// set up transitions
		fragment.setEnterTransition(new Slide(Gravity.RIGHT));
		setExitTransition(new Slide(Gravity.LEFT));

		// pass in arguments
		Bundle bundle = new Bundle();
		bundle.putBoolean(EXTRA_IS_INFINITE, isInfinite);
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
}
