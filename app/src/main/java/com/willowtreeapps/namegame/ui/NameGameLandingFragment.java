package com.willowtreeapps.namegame.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.willowtreeapps.namegame.R;
import com.willowtreeapps.namegame.core.NameGameApplication;

public class NameGameLandingFragment extends Fragment {
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

		for (int i = 0; i < 5; i++) {
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
			if (i==0)
				button.requestFocus();
		}
	}

	public void onButtonClick(View view, int position) {
		description.setText(GAME_MODE_DESCRIPTIONS[position]);
	}
}
