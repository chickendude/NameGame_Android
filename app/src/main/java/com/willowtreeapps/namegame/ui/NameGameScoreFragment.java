package com.willowtreeapps.namegame.ui;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
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
import com.willowtreeapps.namegame.ui.objects.Score;

import java.util.List;

public class NameGameScoreFragment extends Fragment {
	public static final String EXTRA_SCORES = "extra_scores";

	private final String[] RANKS = {"Beginner", "Intern", "New Guy/Gal", "Old-Timer", "CEO"};
	private final String[] DESCRIPTIONS = {
			"Are you even a WillowTree employee? I think you may have come to the wrong office today...",
			"Don't worry, you'll make it someday!",
			"Nice to start seeing some familiar faces!",
			"Well there, aren't we something! Nice work, old-timer!",
			"Yes sir! Everything will be ready on time!"
	};

	// Views
	LinearLayout scoreLayout;
	TextView totalScore;
	TextView rank;
	TextView description;
	Button button;

	// Objects
	List<Score> scores;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		NameGameApplication.get(getActivity()).component().inject(this);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.name_game_score_fragment, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		scoreLayout = view.findViewById(R.id.scoreLayout);
		totalScore = view.findViewById(R.id.totalScore);
		rank = view.findViewById(R.id.rank);
		description = view.findViewById(R.id.description);
		button = view.findViewById(R.id.okButton);

		button.setOnClickListener(v -> getFragmentManager().popBackStack());

		Bundle bundle = getArguments();
		if (bundle != null) {
			scores = bundle.getParcelableArrayList(EXTRA_SCORES);
			loadScores();
		} else {
			Toast.makeText(getContext(), "Scores not received!", Toast.LENGTH_SHORT).show();
			getFragmentManager().popBackStack();
		}
	}

	private void loadScores() {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT,
				1f);
		textParams.setMargins(4, 4, 4, 4);
		int totalPoints = 0;
		for (Score score : scores) {
			LinearLayout layout = new LinearLayout(getContext());
			layout.setLayoutParams(params);
			layout.setOrientation(LinearLayout.HORIZONTAL);
			makeTextView(layout, textParams, score.getField() + ":");
			makeTextView(layout, textParams, score.getValue());
			makeTextView(layout, textParams, score.getPoints() + " points");
			scoreLayout.addView(layout);
			totalPoints += score.getPoints();
		}
		totalScore.setText(totalPoints + " points");

		// show the user's rank based on their total score
		int index = totalPoints / 500;
		if (index >= RANKS.length)
			index = RANKS.length - 1;
		rank.setText(RANKS[index]);
		description.setText(DESCRIPTIONS[index]);
	}

	private void makeTextView(LinearLayout layout, LinearLayout.LayoutParams params, String text) {
		TextView textView = new TextView(getContext());
		textView.setText(text);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
		}
		textView.setGravity(Gravity.CENTER);
		textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
		textView.setTextColor(ContextCompat.getColor(getContext(), R.color.colorSecondaryText));
		layout.addView(textView, params);
	}
}
